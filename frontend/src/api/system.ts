import { request } from './http'
import type { SystemInfo } from './types'

export function getSystemInfo() {
  return request<SystemInfo>({ url: '/system/info', method: 'get' })
}