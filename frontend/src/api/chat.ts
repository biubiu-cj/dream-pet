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

/** 开启新对话：清空当前聊天窗口上下文，长期记忆保留 */
export function startNewConversation() {
  return request<ChatHistory>({
    url: '/chat/new',
    method: 'post',
  })
}