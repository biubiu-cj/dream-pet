#!/usr/bin/env node
/**
 * DreamPet 端到端冒烟测试（Node 18+，无需额外依赖）
 * 用法：node scripts/smoke-test.mjs
 * 可选环境变量：API_BASE（默认 http://localhost:8080/api）、SAVE_TOKEN_FILE
 * 覆盖：游客登录 → 创建伙伴 → 聊天（兴趣/画像提取）→ 生成任务 → 完成任务
 *      → 成长档案 → 历史恢复 →（阈值为 0 时）重新进入欢迎语。
 */

const BASE = process.env.API_BASE || 'http://localhost:8080/api'
const SAVE_TOKEN = process.env.SAVE_TOKEN_FILE || ''

async function request(method, path, body, token, expectStatus = 200) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    throw new Error(`${method} ${path} 返回非 JSON: ${text.slice(0, 200)}`)
  }
  if (res.status !== expectStatus) {
    throw new Error(`${method} ${path} 期望 HTTP ${expectStatus}，实际 ${res.status}: ${text.slice(0, 300)}`)
  }
  return json
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const ok = (msg) => console.log(`  ✔ ${msg}`)
const section = (title) => console.log(`\n=== ${title} ===`)

async function poll(fn, predicate, timeoutMs = 6000) {
  const deadline = Date.now() + timeoutMs
  let last
  while (Date.now() < deadline) {
    last = await fn()
    if (predicate(last)) return last
    await sleep(150)
  }
  throw new Error(`轮询超时，最后结果: ${JSON.stringify(last)?.slice(0, 300)}`)
}

try {
  section('1. 系统信息')
  const sys = (await request('GET', '/system/info')).data
  ok(`mode=${sys.mode} app=${sys.appName}`)

  section('2. 一键体验登录')
  const auth = (await request('POST', '/auth/guest')).data
  const token = auth.token
  ok(`用户 ${auth.username} (id=${auth.userId})，hasPet=${auth.hasPet}`)
  if (SAVE_TOKEN) {
    const fs = await import('node:fs')
    fs.writeFileSync(SAVE_TOKEN, token)
    ok(`token 已写入 ${SAVE_TOKEN}`)
  }

  section('3. 创建伙伴「星星」')
  const pet = (
    await request('POST', '/pets', {
      name: '星星',
      species: 'fox',
      personality: 'curious',
    }, token)
  ).data
  ok(`${pet.name} ${pet.speciesName} / ${pet.personalityName} / Lv.${pet.level} / ${pet.stageName}`)
  ok(`初始经验 ${pet.experience}，话题记忆 [${pet.interests.join(', ') || '暂无，等待聊天中自然发现'}]`)

  section('4. 第一次聊天（表达兴趣与梦想）')
  const chat = (
    await request('POST', '/chat', { message: '我很喜欢恐龙，我以后想成为科学家。' }, token)
  ).data
  ok(`星星回复：${chat.reply}`)
  ok(`demo=${chat.demo}，canSuggestTask=${chat.canSuggestTask}`)

  section('5. 长期记忆提取（异步）')
  const summary = await poll(
    async () => (await request('GET', '/memories/summary', undefined, token)).data,
    (d) => JSON.stringify(d.interests).includes('恐龙'),
  )
  ok(`兴趣记忆：${JSON.stringify(summary.interests)}`)
  const learned = (await request('GET', '/pets/current', undefined, token)).data
  ok(`伙伴已学会兴趣：${learned.interests.join(', ')}`)

  section('6. 生成个性化任务')
  const task = (await request('POST', '/tasks/generate', undefined, token)).data
  ok(`任务「${task.title}」/ ${task.category} / 难度 ${task.difficulty} / +${task.reward} 经验`)
  const again = (await request('POST', '/tasks/generate', undefined, token)).data
  if (again.id !== task.id) throw new Error('存在未完成任务时不应重复生成')
  ok('重复请求返回同一任务（不重复生成）')

  section('7. 完成任务')
  const done = (
    await request('POST', `/tasks/${task.id}/complete`, { share: '霸王龙的牙齿很尖，三角龙有三只角' }, token)
  ).data
  ok(`获得 +${done.xpGained} 经验，Lv.${done.newLevel}，levelUp=${done.levelUp}`)
  ok(`伙伴反馈：${done.feedback}`)
  const dup = await request('POST', `/tasks/${task.id}/complete`, {}, token, 400)
  ok(`重复完成被拒绝：${dup.message}`)

  section('8. 成长档案')
  const growth = (await request('GET', '/growth/overview', undefined, token)).data
  ok(`累计经验 ${growth.totalExp}，完成任务 ${growth.completedTasks} 个，阶段「${growth.pet.stageName}」`)
  ok(`成长事件 ${growth.recentEvents.length} 条`)

  section('9. 历史恢复 + 重新进入欢迎语')
  const history = (await request('GET', '/chat/history?greeting=true', undefined, token)).data
  ok(`聊天记录 ${history.messages.length} 条`)
  if (history.greeting) {
    ok(`欢迎语：${history.greeting}`)
  } else {
    console.log('  - 未生成欢迎语（距上次聊天未超过配置的空闲阈值，属正常现象）')
  }

  console.log('\n全部冒烟测试通过 ✅')
} catch (e) {
  console.error(`\n冒烟测试失败 ❌\n${e.message}`)
  process.exitCode = 1
}