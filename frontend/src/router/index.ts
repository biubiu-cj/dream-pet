import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePetStore } from '@/stores/pet'
import { useChatStore } from '@/stores/chat'
import { useTasksStore } from '@/stores/tasks'

const router = createRouter({
  // hash 路由：静态托管（CloudBase）无需额外配置 404 回退，刷新任意页面都可用
  history: createWebHashHistory(),
  routes: [
    {
      path: '/welcome',
      name: 'welcome',
      component: () => import('@/views/WelcomeView.vue'),
      meta: { public: true },
    },
    {
      path: '/create-pet',
      name: 'create-pet',
      component: () => import('@/views/CreatePetView.vue'),
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/tasks',
      name: 'tasks',
      component: () => import('@/views/TasksView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/tasks/:id',
      name: 'task-detail',
      component: () => import('@/views/TaskDetailView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/growth',
      name: 'growth',
      component: () => import('@/views/GrowthView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/diary',
      name: 'diary',
      component: () => import('@/views/DiaryView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/SettingsView.vue'),
      meta: { requiresPet: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  auth.hydrate()

  if (to.meta.public) {
    if (auth.isLoggedIn) {
      const petStore = usePetStore()
      if (!petStore.loaded) await petStore.load()
      return petStore.pet ? '/' : '/create-pet'
    }
    return true
  }

  if (!auth.isLoggedIn) {
    return { name: 'welcome' }
  }

  if (to.meta.requiresPet) {
    const petStore = usePetStore()
    if (!petStore.loaded) await petStore.load()
    if (!petStore.pet) return { name: 'create-pet' }
  }

  if (to.name === 'create-pet') {
    const petStore = usePetStore()
    if (!petStore.loaded) await petStore.load()
    if (petStore.pet) return { name: 'home' }
  }

  return true
})

// 登录失效时清理所有用户态数据
window.addEventListener('dreampet:unauthorized', () => {
  useAuthStore().logout()
  usePetStore().reset()
  useChatStore().reset()
  useTasksStore().reset()
  if (router.currentRoute.value.name !== 'welcome') {
    router.replace({ name: 'welcome' })
  }
})

export default router