import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@/styles/global.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

// PWA：仅生产环境注册，避免开发时缓存干扰
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register(`${import.meta.env.BASE_URL}sw.js`).catch(() => {
      // 注册失败不影响正常使用
    })
  })
}