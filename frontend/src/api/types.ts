export type Species = 'fox' | 'rabbit' | 'cat' | 'bear' | 'dino'
export type Personality = 'curious' | 'gentle' | 'brave' | 'clever' | 'calm'
export type GrowthStage = 'seed' | 'sprout' | 'bloom' | 'dream'
export type Mood = 'excited' | 'happy' | 'calm' | 'sleepy'
export type TaskCategory =
  | 'knowledge'
  | 'observe'
  | 'creative'
  | 'story'
  | 'life'
  | 'logic'
export type TaskStatus = 'pending' | 'in_progress' | 'completed'
export type MemoryType = 'interest' | 'event' | 'profile' | 'task' | 'milestone'
export type EventType =
  | 'pet_created'
  | 'first_chat'
  | 'chat_exp'
  | 'task_completed'
  | 'level_up'
  | 'stage_up'
  | 'explore'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface SystemInfo {
  mode: 'mock' | 'real'
  appName: string
}

export interface AuthResponse {
  token: string
  username: string
  userId: number
  hasPet: boolean
}

export interface AuthProfile {
  userId: number
  username: string
  hasPet: boolean
}

export interface Pet {
  id: number
  name: string
  species: Species
  speciesName: string
  personality: Personality
  personalityName: string
  level: number
  experience: number
  growthStage: GrowthStage
  stageName: string
  avatarKey: string
  expToNextLevel: number
  levelProgress: number
  interests: string[]
  mood: Mood
  createdAt: string
}

export interface CreatePetPayload {
  name: string
  species: Species
  personality: Personality
  /** 兼容旧接口；创建流程已不再要求选择兴趣 */
  interests?: string[]
}

export interface UpdatePetPayload {
  name?: string
  species?: Species
  personality?: Personality
  interests?: string[]
}

export interface ChatMessage {
  id?: number
  role: 'user' | 'assistant'
  content: string
  createdAt?: string
}

export interface ChatHistory {
  messages: ChatMessage[]
  greeting?: string | null
}

export interface ChatResponse {
  reply: string
  pet: Pet
  suggestedTask: Task | null
  canSuggestTask: boolean
  mood: Mood
  demo: boolean
  /** 本轮用户意图（例如 task_request / memory_recall / emotion_negative） */
  intent?: string
  intentLabel?: string
}

export interface Task {
  id: number
  title: string
  description: string
  category: TaskCategory
  difficulty: number
  reward: number
  status: TaskStatus
  createdAt: string
  completedAt: string | null
}

export interface TodayTasks {
  tasks: Task[]
  hasActive: boolean
}

export interface CompleteTaskResponse {
  pet: Pet
  xpGained: number
  levelUp: boolean
  newLevel: number
  stageUp: boolean
  newStage: GrowthStage
  feedback: string
}

export interface GrowthEvent {
  id: number
  eventType: EventType
  description: string
  experienceGained: number
  createdAt: string
}

export interface DailySummary {
  date: string
  eventCount: number
  experience: number
  chatCount: number
  taskCount: number
  highlights: string[]
  milestone: boolean
}

export interface Achievement {
  key: string
  title: string
  description: string
  unlocked: boolean
  progress: number
  target: number
}

export interface GrowthOverview {
  pet: Pet
  stageDesc: string
  completedTasks: number
  totalExp: number
  expToNextLevel: number
  levelProgress: number
  interests: string[]
  recentEvents: GrowthEvent[]
}

export interface Memory {
  id: number
  memoryType: MemoryType
  content: string
  importance: number
  createdAt: string
}

export interface MemorySummary {
  summary: string
  importantEvents: string[]
  interests: string[]
  profile: string[]
}

export interface LoginPayload {
  username: string
  password: string
}