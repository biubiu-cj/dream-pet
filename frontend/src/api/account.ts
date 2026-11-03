import { request } from './http'

export function deleteAccount() {
  return request<null>({ url: '/account', method: 'delete' })
}