import { request } from './http'
import type { Memory, MemorySummary, MemoryType } from './types'

export function getMemories(params: { type?: MemoryType; limit?: number } = {}) {
  return request<Memory[]>({ url: '/memories', method: 'get', params })
}

export function getMemorySummary() {
  return request<MemorySummary>({ url: '/memories/summary', method: 'get' })
}