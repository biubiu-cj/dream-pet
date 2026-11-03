import { request } from './http'
import type { CompleteTaskResponse, Task, TaskStatus, TodayTasks } from './types'

export function getTodayTasks() {
  return request<TodayTasks>({ url: '/tasks/today', method: 'get' })
}

export function generateTask() {
  return request<Task>({
    url: '/tasks/generate',
    method: 'post',
    timeout: 70000,
  })
}

export function startTask(id: number) {
  return request<Task>({ url: `/tasks/${id}/start`, method: 'post' })
}

export function getTaskList(params: { status?: TaskStatus; limit?: number } = {}) {
  return request<Task[]>({ url: '/tasks', method: 'get', params })
}

export function getTask(id: number) {
  return request<Task>({ url: `/tasks/${id}`, method: 'get' })
}

export function completeTask(id: number, share?: string) {
  return request<CompleteTaskResponse>({
    url: `/tasks/${id}/complete`,
    method: 'post',
    data: share ? { share } : {},
  })
}