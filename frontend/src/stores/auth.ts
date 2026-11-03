import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import type { AuthResponse, LoginPayload } from '@/api/types'
import { AUTH_KEY, TOKEN_KEY } from '@/constants/storage'

interface StoredAuth {
  userId: number
  username: string
  hasPet: boolean
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const userId = ref<number | null>(null)
  const username = ref('')
  const hasPet = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  let hydrated = false

  function hydrate() {
    if (hydrated) return
    hydrated = true
    const raw = localStorage.getItem(AUTH_KEY)
    if (!raw) return
    try {
      const stored = JSON.parse(raw) as StoredAuth
      userId.value = stored.userId
      username.value = stored.username
      hasPet.value = stored.hasPet
    } catch {
      localStorage.removeItem(AUTH_KEY)
    }
  }

  function persist() {
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(
      AUTH_KEY,
      JSON.stringify({ userId: userId.value, username: username.value, hasPet: hasPet.value }),
    )
  }

  function apply(response: AuthResponse) {
    token.value = response.token
    userId.value = response.userId
    username.value = response.username
    hasPet.value = response.hasPet
    persist()
  }

  async function login(payload: LoginPayload) {
    apply(await authApi.login(payload))
  }

  async function register(payload: LoginPayload) {
    apply(await authApi.register(payload))
  }

  async function guest() {
    apply(await authApi.guestLogin())
  }

  function setHasPet(value: boolean) {
    hasPet.value = value
    if (token.value) persist()
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    hasPet.value = false
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(AUTH_KEY)
  }

  return {
    token,
    userId,
    username,
    hasPet,
    isLoggedIn,
    hydrate,
    login,
    register,
    guest,
    setHasPet,
    logout,
  }
})