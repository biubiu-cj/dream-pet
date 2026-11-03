import { request } from './http'
import type { Achievement, DailySummary, GrowthEvent, GrowthOverview } from './types'

export function getGrowthOverview() {
  return request<GrowthOverview>({ url: '/growth/overview', method: 'get' })
}

export function getGrowthEvents(limit = 50) {
  return request<GrowthEvent[]>({
    url: '/growth/events',
    method: 'get',
    params: { limit },
  })
}

export function getGrowthDaily(days = 7) {
  return request<DailySummary[]>({
    url: '/growth/daily',
    method: 'get',
    params: { days },
  })
}

export function getAchievements() {
  return request<Achievement[]>({ url: '/growth/achievements', method: 'get' })
}