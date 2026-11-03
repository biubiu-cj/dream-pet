import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChatHistory, sendChat } from '@/api/chat'
import type { ChatMessage, Task } from '@/api/types'
import { usePetStore } from './pet'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const historyLoaded = ref(false)
  const canSuggestTask = ref(false)
  const suggestedTask = ref<Task | null>(null)
  const error = ref('')

  async function loadHistory(force = false) {
    if (historyLoaded.value && !force) return
    error.value = ''
    try {
      const data = await getChatHistory(50, true)
      messages.value = data.messages ?? []
      historyLoaded.value = true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '聊天记录加载失败'
    }
  }

  async function send(text: string) {
    const content = text.trim()
    if (!content || loading.value) return null
    error.value = ''
    messages.value.push({ role: 'user', content })
    loading.value = true
    try {
      const response = await sendChat(content)
      messages.value.push({ role: 'assistant', content: response.reply })
      canSuggestTask.value = response.canSuggestTask
      suggestedTask.value = response.suggestedTask
      usePetStore().setPet(response.pet)
      return response
    } catch (e) {
      // 发送失败时移除刚插入的用户消息，由页面把内容放回输入框，避免重复
      const last = messages.value[messages.value.length - 1]
      if (last && last.role === 'user' && last.content === content) {
        messages.value.pop()
      }
      error.value = e instanceof Error ? e.message : '伙伴暂时没听清，请再说一次'
      return null
    } finally {
      loading.value = false
    }
  }

  function attachTask(task: Task) {
    suggestedTask.value = task
    canSuggestTask.value = false
  }

  function reset() {
    messages.value = []
    loading.value = false
    historyLoaded.value = false
    canSuggestTask.value = false
    suggestedTask.value = null
    error.value = ''
  }

  return {
    messages,
    loading,
    historyLoaded,
    canSuggestTask,
    suggestedTask,
    error,
    loadHistory,
    send,
    attachTask,
    reset,
  }
})