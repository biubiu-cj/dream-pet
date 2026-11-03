import { request } from './http'
import type { ChatHistory, ChatResponse } from './types'

export function sendChat(message: string) {
  return request<ChatResponse>({
    url: '/chat',
    method: 'post',
    data: { message },
    timeout: 70000,
  })
}

export function getChatHistory(limit = 50, greeting = false) {
  return request<ChatHistory>({
    url: '/chat/history',
    method: 'get',
    params: { limit, greeting },
  })
}