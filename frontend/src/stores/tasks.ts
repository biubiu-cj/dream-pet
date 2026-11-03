import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  completeTask as apiCompleteTask,
  generateTask,
  getTaskList,
  getTodayTasks,
} from '@/api/tasks'
import type { CompleteTaskResponse, Task } from '@/api/types'
import { usePetStore } from './pet'
import { useCelebrationStore } from './celebration'

export const useTasksStore = defineStore('tasks', () => {
  const today = ref<Task[]>([])
  const history = ref<Task[]>([])
  const hasActive = ref(false)
  const loading = ref(false)
  const generating = ref(false)
  const error = ref('')

  const activeTask = computed(
    () => today.value.find((task) => task.status !== 'completed') ?? null,
  )

  async function loadToday() {
    loading.value = true
    error.value = ''
    try {
      const data = await getTodayTasks()
      today.value = data.tasks ?? []
      hasActive.value = data.hasActive
    } catch (e) {
      error.value = e instanceof Error ? e.message : '任务加载失败'
    } finally {
      loading.value = false
    }
  }

  async function loadHistory() {
    try {
      history.value = await getTaskList({ limit: 50 })
    } catch (e) {
      error.value = e instanceof Error ? e.message : '历史任务加载失败'
    }
  }

  async function generate() {
    if (generating.value) return null
    generating.value = true
    error.value = ''
    try {
      const task = await generateTask()
      const index = today.value.findIndex((item) => item.id === task.id)
      if (index >= 0) today.value[index] = task
      else today.value.unshift(task)
      hasActive.value = true
      return task
    } catch (e) {
      error.value = e instanceof Error ? e.message : '任务生成失败'
      return null
    } finally {
      generating.value = false
    }
  }

  async function complete(id: number, share?: string): Promise<CompleteTaskResponse | null> {
    error.value = ''
    try {
      const response = await apiCompleteTask(id, share)
      const apply = (list: Task[]) => {
        const target = list.find((task) => task.id === id)
        if (target) target.status = 'completed'
      }
      apply(today.value)
      apply(history.value)
      hasActive.value = today.value.some((task) => task.status !== 'completed')
      usePetStore().setPet(response.pet)
      useCelebrationStore().celebrate({
        levelUp: response.levelUp,
        stageUp: response.stageUp,
        newLevel: response.newLevel,
        newStage: response.newStage,
        xpGained: response.xpGained,
        title: today.value.find((task) => task.id === id)?.title,
      })
      return response
    } catch (e) {
      error.value = e instanceof Error ? e.message : '任务完成失败'
      return null
    }
  }

  function reset() {
    today.value = []
    history.value = []
    hasActive.value = false
    error.value = ''
  }

  return {
    today,
    history,
    hasActive,
    activeTask,
    loading,
    generating,
    error,
    loadToday,
    loadHistory,
    generate,
    complete,
    reset,
  }
})