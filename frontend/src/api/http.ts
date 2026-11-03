import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiEnvelope } from './types'
import { AUTH_KEY, TOKEN_KEY } from '@/constants/storage'

export class ApiError extends Error {
  code: number

  constructor(message: string, code = -1) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const http = axios.create({
  // 开发时走 Vite proxy；生产构建可通过 VITE_API_BASE 指向云托管后端域名
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
})

function notifyUnauthorized() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(AUTH_KEY)
  window.dispatchEvent(new CustomEvent('dreampet:unauthorized'))
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status
    const payload = error.response?.data
    const code = payload?.code ?? status ?? -1

    if (status === 401 || code === 401) {
      notifyUnauthorized()
    }

    if (error.code === 'ECONNABORTED') {
      return Promise.reject(
        new ApiError('伙伴想得有点久，请稍后再试一次', code),
      )
    }

    if (!error.response) {
      return Promise.reject(
        new ApiError('伙伴暂时联系不上，请检查网络或稍后再试', -1),
      )
    }

    return Promise.reject(
      new ApiError(payload?.message || '出了点小问题，请稍后再试', code),
    )
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await http.request<ApiEnvelope<T>>(config)
    const payload = response.data

    if (!payload || typeof payload !== 'object' || typeof payload.code !== 'number') {
      throw new ApiError('服务返回的数据有点奇怪，请稍后再试', -1)
    }

    if (payload.code !== 0) {
      if (payload.code === 401) {
        notifyUnauthorized()
      }
      throw new ApiError(payload.message || '出了点小问题，请稍后再试', payload.code)
    }

    return payload.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    if (axios.isAxiosError(error)) {
      const payload = error.response?.data as ApiEnvelope<unknown> | undefined
      const code = payload?.code ?? error.response?.status ?? -1
      if (error.code === 'ECONNABORTED') {
        throw new ApiError('伙伴想得有点久，请稍后再试一次', code)
      }
      throw new ApiError(
        payload?.message ||
          (error.response
            ? '出了点小问题，请稍后再试'
            : '伙伴暂时联系不上，请检查网络或稍后再试'),
        code,
      )
    }
    throw new ApiError('出了一点小状况，请稍后再试', -1)
  }
}