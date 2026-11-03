#!/usr/bin/env node
/**
 * 意图识别演示脚本（真实模式，规则 + LLM 兜底）
 * 用法：node scripts/intent-demo.mjs <token-file>
 */
import fs from 'node:fs'

const BASE = process.env.API_BASE || 'http://localhost:8080/api'
const tokenFile = process.argv[2]
if (!tokenFile) {
  console.error('用法: node scripts/intent-demo.mjs <token-file>')
  process.exitCode = 1
  process.exit()
}
const token = fs.readFileSync(tokenFile, 'utf8').trim()

const samples = [
  '你好呀，星星',
  '我想做个小任务',
  '你还记得上次我们一起做过什么吗',
  '为什么天空是蓝色的？',
  '我今天有点难过',
  '我很喜欢恐龙',
  '我以后想成为科学家',
  '我觉得今天窗外的树叶好像在对我招手',
  '晚安，明天见',
]

for (const message of samples) {
  const res = await fetch(BASE + '/chat/intent', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ message }),
  })
  const json = await res.json()
  if (json.code !== 0) {
    console.log(`${message} -> 错误 ${json.code}: ${json.message}`)
    continue
  }
  console.log(`${message} -> ${json.data.intent}（${json.data.label}）`)
}