import type {
  EventType,
  GrowthStage,
  Mood,
  Personality,
  Species,
  TaskCategory,
} from '@/api/types'

export interface SpeciesOption {
  value: Species
  name: string
  blurb: string
  color: string
}

export const SPECIES_OPTIONS: SpeciesOption[] = [
  { value: 'fox', name: '小狐狸', blurb: '机灵又温暖', color: '#FFB887' },
  { value: 'rabbit', name: '小兔子', blurb: '软软又贴心', color: '#FFE3D3' },
  { value: 'cat', name: '小猫咪', blurb: '安静爱观察', color: '#CBB8F3' },
  { value: 'bear', name: '小熊', blurb: '可靠的大抱抱', color: '#D9A678' },
  { value: 'dino', name: '小恐龙', blurb: '好奇又勇敢', color: '#9FE0A8' },
]

export const PERSONALITY_OPTIONS: {
  value: Personality
  label: string
  desc: string
}[] = [
  { value: 'curious', label: '活泼好奇', desc: '总想和你一起发现新东西' },
  { value: 'gentle', label: '温柔友善', desc: '慢慢听你说，轻轻陪着你' },
  { value: 'brave', label: '勇敢探索', desc: '遇到难题也想试一试' },
  { value: 'clever', label: '聪明机灵', desc: '小脑袋里全是奇思妙想' },
  { value: 'calm', label: '安静细心', desc: '会留意每一个小细节' },
]


export const STAGE_OPTIONS: {
  value: GrowthStage
  label: string
  range: string
  desc: string
  minLevel: number
}[] = [
  {
    value: 'seed',
    label: '初识伙伴',
    range: 'Lv.1 - 2',
    desc: '刚刚相遇，一切才刚开始',
    minLevel: 1,
  },
  {
    value: 'sprout',
    label: '好奇伙伴',
    range: 'Lv.3 - 4',
    desc: '熟悉起来了，每天都想多聊一点',
    minLevel: 3,
  },
  {
    value: 'bloom',
    label: '默契伙伴',
    range: 'Lv.5 - 7',
    desc: '一个眼神就懂你的小心思',
    minLevel: 5,
  },
  {
    value: 'dream',
    label: '梦想伙伴',
    range: 'Lv.8 - 10',
    desc: '一起长大，一起追梦',
    minLevel: 8,
  },
]

export const CATEGORY_META: Record<
  TaskCategory,
  { label: string; icon: string; color: string; bg: string }
> = {
  knowledge: { label: '知识探索', icon: 'book', color: '#5C8FB8', bg: '#DFF1FF' },
  observe: { label: '观察发现', icon: 'eye', color: '#4E9378', bg: '#DDF5E8' },
  creative: { label: '创意想象', icon: 'palette', color: '#B8739F', bg: '#FBE7F4' },
  story: { label: '故事创作', icon: 'feather', color: '#B07A3D', bg: '#FFF0D6' },
  life: { label: '生活小挑战', icon: 'sun', color: '#C07C45', bg: '#FFE8D9' },
  logic: { label: '逻辑思考', icon: 'grid', color: '#7A6BBF', bg: '#EAE4FF' },
}

export const MOOD_META: Record<Mood, { label: string; phrase: string }> = {
  excited: { label: '兴奋', phrase: '今天也元气满满！' },
  happy: { label: '开心', phrase: '见到你就很开心～' },
  calm: { label: '平静', phrase: '慢慢来，我会陪着你。' },
  sleepy: { label: '想念', phrase: '有点想你了，抱抱好吗？' },
}

export const STATUS_META = {
  pending: { label: '待完成' },
  in_progress: { label: '进行中' },
  completed: { label: '已完成' },
} as const

export const EVENT_TYPE_META: Record<
  EventType,
  { label: string; icon: string; color: string; bg: string }
> = {
  pet_created: { label: '相遇的日子', icon: 'heart', color: '#E07A8F', bg: '#FFE4EA' },
  first_chat: { label: '第一次聊天', icon: 'chat', color: '#4E8AB8', bg: '#E1F2FF' },
  chat_exp: { label: '聊天成长', icon: 'sparkle', color: '#B5892E', bg: '#FFF2C9' },
  task_completed: { label: '完成任务', icon: 'check', color: '#4E9378', bg: '#DDF5E8' },
  level_up: { label: '升级啦', icon: 'star', color: '#C08A2B', bg: '#FFEFC2' },
  stage_up: { label: '阶段成长', icon: 'crown', color: '#A47A2B', bg: '#FFF0C7' },
  explore: { label: '探索发现', icon: 'compass', color: '#7A6BBF', bg: '#EBE5FF' },
}

export const DIFFICULTY_LABELS: Record<number, string> = {
  1: '轻松',
  2: '有点挑战',
  3: '大挑战',
}

export function speciesOption(value: Species | string | undefined): SpeciesOption {
  return SPECIES_OPTIONS.find((item) => item.value === value) ?? SPECIES_OPTIONS[0]!
}

export function stageOption(value: GrowthStage | string | undefined) {
  return STAGE_OPTIONS.find((item) => item.value === value) ?? STAGE_OPTIONS[0]!
}

export function categoryMeta(value: TaskCategory | string | undefined) {
  return CATEGORY_META[value as TaskCategory] ?? CATEGORY_META.knowledge
}

export function eventMeta(value: EventType | string | undefined) {
  return EVENT_TYPE_META[value as EventType] ?? EVENT_TYPE_META.explore
}

export function personalityLabel(value: Personality | string | undefined): string {
  return PERSONALITY_OPTIONS.find((item) => item.value === value)?.label ?? '活泼好奇'
}

export function moodMeta(value: Mood | string | undefined) {
  return MOOD_META[value as Mood] ?? MOOD_META.happy
}