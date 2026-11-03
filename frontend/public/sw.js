/* DreamPet 轻量 PWA：缓存应用外壳，API 请求不缓存 */
const CACHE = 'dreampet-shell-v2'
const BASE = new URL('./', self.location.href).pathname
const SHELL = [BASE, `${BASE}index.html`, `${BASE}manifest.webmanifest`]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(SHELL))
      .then(() => self.skipWaiting())
      .catch(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.origin !== self.location.origin) return
  if (url.pathname.startsWith(`${BASE}api`)) return

  if (request.mode === 'navigate') {
    event.respondWith(fetch(request).catch(() => caches.match(`${BASE}index.html`)))
    return
  }

  event.respondWith(
    caches.match(request).then((hit) => {
      const network = fetch(request)
        .then((response) => {
          if (response && response.ok) {
            const clone = response.clone()
            caches.open(CACHE).then((cache) => cache.put(request, clone))
          }
          return response
        })
        .catch(() => hit)
      return hit || network
    }),
  )
})