function toDate(value?: string | null): Date | null {
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateTime(value?: string | null): string {
  const date = toDate(value)
  if (!date) return value ?? ''
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}月${day}日 ${hours}:${minutes}`
}

export function formatDay(value?: string | null): string {
  const date = toDate(value)
  if (!date) return value ?? ''
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

export function formatFullDay(value?: string | null): string {
  const date = toDate(value)
  if (!date) return value ?? ''
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

/** 解析后端返回的 yyyy-MM-dd（按本地日期，避免时区偏移） */
export function parseLocalDay(value: string): Date | null {
  const matched = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!matched) return null
  return new Date(Number(matched[1]), Number(matched[2]) - 1, Number(matched[3]))
}

export function relativeDayLabel(value: string): string {
  const date = parseLocalDay(value)
  if (!date) return value
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const diff = Math.round((today.getTime() - date.getTime()) / 86400000)
  if (diff === 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff === 2) return '前天'
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

export function weekdayLabel(value: string): string {
  const date = parseLocalDay(value)
  if (!date) return ''
  return ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][date.getDay()]
}

export function greetingByHour(): string {
  const hour = new Date().getHours()
  if (hour < 5) return '夜深啦，做个甜甜的梦吧'
  if (hour < 11) return '早上好，小朋友'
  if (hour < 14) return '中午好，小朋友'
  if (hour < 18) return '下午好，小朋友'
  return '晚上好，小朋友'
}

export function todayLabel(): string {
  const date = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${date.getMonth() + 1}月${date.getDate()}日 · ${weekdays[date.getDay()]}`
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}