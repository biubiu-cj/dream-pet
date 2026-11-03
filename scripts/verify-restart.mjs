#!/usr/bin/env node
/**
 * 重启后持久化验证：
 * 用法：node scripts/verify-restart.mjs <token-file>
 * 读取冒烟测试保存的 token，验证伙伴、记忆、任务、欢迎语在进程重启后仍存在。
 */
import fs from 'node:fs'

const BASE = process.env.API_BASE || 'http://localhost:8080/api'
const tokenFile = process.argv[2]
if (!tokenFile) {
  console.error('用法: node scripts/verify-restart.mjs <token-file>')
  process.exitCode = 1
}

let token
try {
  token = fs.readFileSync(tokenFile, 'utf8').trim()
} catch {
  console.error(`无法读取 token 文件: ${tokenFile}（请先运行 smoke-test.mjs 并设置 SAVE_TOKEN_FILE）`)
  process.exitCode = 1
}

async function get(path) {
  const res = await fetch(BASE + path, { headers: { Authorization: `Bearer ${token}` } })
  const json = await res.json()
  if (res.status !== 200) throw new Error(`${path} -> HTTP ${res.status}: ${json.message}`)
  return json.data
}

try {
  console.log('=== 重启后数据检查 ===')
  const pet = await get('/pets/current')
  console.log(`伙伴: ${pet.name} Lv.${pet.level} 经验 ${pet.experience} 阶段 ${pet.stageName}`)
  console.log(`兴趣: ${pet.interests.join(', ')}`)

  const summary = await get('/memories/summary')
  console.log(`记忆摘要: ${summary.summary || '(暂无)'}`)
  console.log(`重要事件: ${JSON.stringify(summary.importantEvents)}`)
  console.log(`画像: ${JSON.stringify(summary.profile)}`)

  const tasks = await get('/tasks?limit=5')
  console.log(`历史任务: ${JSON.stringify(tasks.map((t) => ({ title: t.title, status: t.status })))}`)

  const history = await get('/chat/history?greeting=true')
  console.log(`聊天记录条数: ${history.messages.length}`)
  console.log(`重新进入欢迎语: ${history.greeting || '(10 分钟内已问候过，不重复生成)'}`)

  // 重复请求不应重复写入欢迎语
  const historyAgain = await get('/chat/history?greeting=true')
  if (historyAgain.messages.length !== history.messages.length) {
    throw new Error('重复请求生成了重复欢迎语')
  }
  console.log('重复请求欢迎语去重: 正常')

  const growth = await get('/growth/overview')
  console.log(`成长: 累计经验 ${growth.totalExp}，完成任务 ${growth.completedTasks}`)

  if (pet.name !== '星星') throw new Error('伙伴数据未恢复')
  if (!summary.interests.includes('恐龙')) throw new Error('兴趣记忆未恢复')
  if (!tasks.some((t) => t.status === 'completed')) {
    throw new Error('任务记忆未恢复')
  }
  console.log('\n重启后持久化验证通过 ✅')
} catch (e) {
  console.error(`\n验证失败 ❌\n${e.message}`)
  process.exitCode = 1
}