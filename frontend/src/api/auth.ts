import { request } from './http'
import type { AuthProfile, AuthResponse, LoginPayload } from './types'

export function register(payload: LoginPayload) {
  return request<AuthResponse>({ url: '/auth/register', method: 'post', data: payload })
}

export function login(payload: LoginPayload) {
  return request<AuthResponse>({ url: '/auth/login', method: 'post', data: payload })
}

export function guestLogin() {
  return request<AuthResponse>({ url: '/auth/guest', method: 'post' })
}

export function getMe() {
  return request<AuthProfile>({ url: '/auth/me', method: 'get' })
}