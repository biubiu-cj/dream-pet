import { request } from './http'
import type { CreatePetPayload, Pet, UpdatePetPayload } from './types'

export function createPet(payload: CreatePetPayload) {
  return request<Pet>({ url: '/pets', method: 'post', data: payload })
}

export function getCurrentPet() {
  return request<Pet>({ url: '/pets/current', method: 'get' })
}

export function getPet(id: number) {
  return request<Pet>({ url: `/pets/${id}`, method: 'get' })
}

export function updatePet(id: number, payload: UpdatePetPayload) {
  return request<Pet>({ url: `/pets/${id}`, method: 'put', data: payload })
}