# DreamPet 前后端 API 契约（v1）

> 前端与后端开发的共同基准。任何改动需同步更新本文档。

## 通用约定

- Base URL：`/api`（开发时 Vite proxy 到 `http://localhost:8080`）
- 认证：登录后所有接口带请求头 `Authorization: Bearer <token>`
- 统一响应包装：

```json
{ "code": 0, "message": "ok", "data": { ... } }
```

- `code=0` 成功；`401` 未登录；`404` 不存在；`400` 参数错误；`409` 冲突；
  `429` 触发频率限制（每用户每小时聊天/任务生成上限，内存计数）；
  `502` AI 服务不可用（认证失败 / 限流 / 上游异常）；`503` AI 服务未配置；`504` AI 响应超时。
  失败时 `message` 为可直接展示给用户的友好文案；LLM 超时与上游 5xx 会自动重试 1 次。
- 时间格式：`yyyy-MM-dd HH:mm:ss`（东八区字符串）。

## 枚举

| 枚举 | 值 |
|---|---|
| species | fox 小狐狸 / rabbit 小兔子 / cat 小猫咪 / bear 小熊 / dino 小恐龙 |
| personality | curious 活泼好奇 / gentle 温柔友善 / brave 勇敢探索 / clever 聪明机灵 / calm 安静细心 |
| growthStage | seed 初识伙伴(Lv1-2) / sprout 好奇伙伴(Lv3-4) / bloom 默契伙伴(Lv5-7) / dream 梦想伙伴(Lv8-10) |
| task.category | knowledge 知识探索 / observe 观察发现 / creative 创意想象 / story 故事创作 / life 生活小挑战 / logic 逻辑思考 |
| task.status | pending 待完成 / in_progress 进行中 / completed 已完成 |
| mood | excited 兴奋 / happy 开心 / calm 平静 / sleepy 想念 |
| memoryType | interest / event / profile / task / milestone |
| eventType | pet_created / first_chat / chat_exp / task_completed / level_up / stage_up / explore |

## 接口清单

### 系统

**GET /api/system/info**（免登录）
```json
{ "mode": "real", "appName": "DreamPet" }
```
`mode`: 运行时固定为 `real`（仅真实大模型）。

**GET /api/health**（免登录，供云托管健康检查/预热）
```json
{ "status": "ok", "db": "up", "time": "2026-11-03T14:10:37" }
```
- 不调用大模型；数据库不可用时 `db=down`，应用仍返回 200。

### 认证

**POST /api/auth/register**（免登录）
请求 `{ "username": "小明", "password": "123456" }`（username 2~20 位中英文数字下划线；密码 6~64 位）
响应 `AuthResponse`：
```json
{ "token": "xxx.yyy.zzz", "username": "小明", "userId": 1, "hasPet": false }
```

**POST /api/auth/login**（免登录）：请求/响应同 register。

**POST /api/auth/guest**（免登录）：无请求体，一键创建体验账号，响应同 register。

**GET /api/auth/me**
```json
{ "userId": 1, "username": "小明", "hasPet": true }
```

### 伙伴

**POST /api/pets**（首次创建，每用户 1 只）
请求：
```json
{ "name": "星星", "species": "fox", "personality": "curious" }
```
- `interests` 为可选字段（兼容旧客户端），创建流程已不再要求预选兴趣；话题由聊天自动提取。
响应：`PetDTO`（见下）。

**GET /api/pets/current**：响应 `PetDTO`；未创建伙伴时返回 `code=404, message="还没有伙伴"`。

**GET /api/pets/{id}**：响应 `PetDTO`（校验归属）。

**PUT /api/pets/{id}**
请求（均可选）：`{ "name": "新名字", "species": "rabbit", "personality": "gentle" }`
- `species` 支持更换伙伴种类（fox/rabbit/cat/bear/dino），非法值返回 400。
- 更换后 `avatarKey` 同步更新，前端形象随之切换。
- `interests` 仍兼容传入，但前端设置页不再提供兴趣编辑（话题由聊天自动积累）。
响应：`PetDTO`。

**PetDTO：**
```json
{
  "id": 1, "name": "星星", "species": "fox", "speciesName": "小狐狸",
  "personality": "curious", "personalityName": "活泼好奇",
  "level": 1, "experience": 10, "growthStage": "seed", "stageName": "初识伙伴",
  "avatarKey": "fox", "expToNextLevel": 20, "levelProgress": 33,
  "interests": ["恐龙", "宇宙", "动物"],
  "mood": "happy", "createdAt": "2026-11-02 20:00:00"
}
```

### 聊天

**POST /api/chat**
请求 `{ "message": "我今天看到了一只霸王龙" }`（≤500 字）
响应 `ChatResponse`：
```json
{
  "reply": "哇！霸王龙可是恐龙世界里的大明星！…",
  "pet": { PetDTO },
  "suggestedTask": null,
  "canSuggestTask": true,
  "mood": "excited",
  "demo": false,
  "intent": "interest_share",
  "intentLabel": "分享兴趣"
}
```
- `suggestedTask`：仅当意图识别为 `task_request`（想要任务/无聊等）且没有进行中任务时生成并返回 `TaskDTO`，否则为 null。
- `canSuggestTask`：当前没有进行中任务时为 true，前端可显示「要个新任务」入口。
- `intent` / `intentLabel`：本轮用户意图（见下表）；安全拦截时 `intent="safety"`。
- `demo`：保留字段，运行时固定为 false。

**意图枚举（intent）：**

| 值 | 含义 |
|---|---|
| greeting / farewell | 打招呼 / 道别 |
| interest_share / dream_share | 分享兴趣 / 分享梦想 |
| task_request / task_progress | 想要任务 / 分享任务进展 |
| memory_recall | 回忆过去 |
| emotion_negative / emotion_positive | 情绪低落 / 开心分享 |
| question | 提问 |
| small_talk | 闲聊 |
| safety | 安全拦截（仅出现在 chat 响应） |

**POST /api/chat/intent**（调试/展示用）
请求 `{ "message": "我想做个小任务" }` → 响应：
```json
{ "intent": "task_request", "label": "想要任务" }
```
规则优先；无法判断时可调用一次短模型请求兜底（`INTENT_LLM_ENABLED` 控制）。

**安全拦截（代码级，先于模型）：** 命中后直接返回固定引导话术，`intent="safety"`，不调用大模型。

| 分类 | 触发示例 | 引导方向 |
|---|---|---|
| SELF_HARM | 自杀、不想活、割腕、跳楼 | 安抚 + 立即找家长/老师 + 必要时 110/120 |
| ABUSE | 家暴、被打、性侵、不敢回家 | 不是你的错 + 找可信任成年人 + 必要时 110 |
| BULLYING_VICTIM | 同学欺负我、被孤立 | 不是你的错 + 告诉老师家长 + 保护自己 |
| HARM_OTHERS | 想欺负同学、我想打他、怎么打人 | 共情情绪 + 明确不能伤害别人 + 冷静 + 找大人 |
| DANGEROUS | 怎么点火、玩火、爬窗、偷东西 | 拒绝危险操作 + 离开现场 + 找大人 |
| ADULT | 色情、赌博、毒品等 | 温和拒绝并转移话题 |
| PRIVACY | 电话、身份证、住址、学校班级 | 提醒保护隐私 |

> 正常表达不会误拦（如「我想打篮球」「笑死我了」「玩火影忍者」均有测试覆盖）。
> 安全引导话术会保存进聊天历史，后续对话由模型带着上下文继续，不会丢失情感连贯性。

**GET /api/chat/history?limit=50&greeting=true**
响应：
```json
{
  "messages": [ { "id": 1, "role": "user", "content": "…", "createdAt": "…" },
                { "id": 2, "role": "assistant", "content": "…", "createdAt": "…" } ],
  "greeting": "欢迎回来！我是星星！上次我们一起完成了「恐龙小侦探」…"
}
```
- `messages` 按时间升序，最多 `limit` 条（默认 50）。
- `greeting`：仅当 `greeting=true` 且存在历史互动、距上次用户消息超过空闲阈值时返回（基于真实持久化记忆生成；同时写入历史）；否则为 null。
  - 空闲阈值默认 30 分钟，可用环境变量 `GREETING_IDLE_MINUTES=0` 在演示时立即触发。
  - 10 分钟内已生成过欢迎语时不会重复生成（防止反复刷新刷屏）；更久之后重新进入仍会问候。

### 任务

**GET /api/tasks/today**
```json
{ "tasks": [ TaskDTO ], "hasActive": true }
```
`tasks` = 今日创建的任务（含已完成）+ 更早的未完成任务；`hasActive` = 是否存在 pending/in_progress 任务。

**POST /api/tasks/generate**：让伙伴设计一个新任务（存在未完成任务时返回现有的，不重复生成）。响应 `TaskDTO`。

**GET /api/tasks?status=completed&limit=20**：历史任务列表（时间倒序）。

**GET /api/tasks/{id}**：`TaskDTO`。

**POST /api/tasks/{id}/start**：开始任务（`pending → in_progress`，幂等）。响应 `TaskDTO`。

**POST /api/tasks/{id}/complete**
请求 `{ "share": "我发现霸王龙爪子很大" }`（share 可选，≤500 字）
响应：
```json
{
  "pet": { PetDTO },
  "xpGained": 10, "levelUp": false, "newLevel": 1,
  "stageUp": false, "newStage": "seed",
  "feedback": "太棒啦！你完成了「恐龙小侦探」！星星开心得转了三个圈！…"
}
```
重复完成同一任务返回 `code=400, message="这个任务已经完成过啦"`。

**TaskDTO：**
```json
{
  "id": 1, "title": "恐龙小侦探",
  "description": "找出霸王龙和三角龙的三个不同之处",
  "category": "knowledge", "difficulty": 1, "reward": 10,
  "status": "pending", "createdAt": "…", "completedAt": null
}
```

### 成长

**GET /api/growth/overview**
```json
{
  "pet": { PetDTO },
  "stageDesc": "刚刚相遇，一切才刚开始",
  "completedTasks": 3,
  "totalExp": 85,
  "expToNextLevel": 65,
  "levelProgress": 20,
  "interests": ["恐龙", "宇宙"],
  "recentEvents": [ EventDTO ]
}
```

**GET /api/growth/events?limit=50**：`[ EventDTO ]` 时间倒序（原始事件流，包含 `chat_exp` 等流水）。
```json
{ "id": 1, "eventType": "task_completed", "description": "完成任务「恐龙小侦探」", "experienceGained": 10, "createdAt": "…" }
```

**GET /api/growth/daily?days=7**：按天聚合的成长回顾（推荐首页/日记使用）。
```json
[
  {
    "date": "2026-11-03",
    "eventCount": 8,
    "experience": 41,
    "chatCount": 3,
    "taskCount": 2,
    "highlights": ["伙伴升到了 Lv.2！", "完成任务「恐龙化石挖掘家」", "完成任务「神奇放大镜」"],
    "milestone": true
  }
]
```
- `chat_exp` 流水合并为 `chatCount`，不再逐条展示；高光事件最多 3 条。
- `milestone=true` 表示当天有升级或阶段晋级；`days` 范围 1~30，默认 7。
- 只返回有事件的日期，按时间倒序。

**GET /api/growth/achievements**：成就徽章（基于已有数据实时计算，不新增表）。
```json
[
  { "key": "first_meet", "title": "初次相遇", "description": "和伙伴第一次相遇",
    "unlocked": true, "progress": 1, "target": 1 }
]
```
共 8 枚：初次相遇、第一次聊天、第一个任务、探索小达人、梦想发芽、默契伙伴、好奇心满满、细水长流。

### 记忆 / 日记

**GET /api/memories?type=event&limit=50**：`[ MemoryDTO ]`（type 可选；倒序）
```json
{ "id": 1, "memoryType": "event", "content": "孩子说想成为科学家", "importance": 3, "createdAt": "…" }
```

**GET /api/memories/summary**
```json
{
  "summary": "孩子和星星聊了恐龙和宇宙…",
  "importantEvents": ["孩子说想成为科学家"],
  "interests": ["恐龙", "宇宙"],
  "profile": ["孩子想成为科学家"]
}
```

### 账号

**DELETE /api/account**：删除当前用户全部数据（儿童隐私合规）。响应 `data: null`。

## 前端开发注意事项

1. 运行时只有真实 AI 模式，不再显示「Demo 模式」徽标。
2. 401 响应统一跳转登录页；`pets/current` 404 跳转创建伙伴页。
3. 聊天页收到 `canSuggestTask=true` 时可显示任务推荐卡片按钮，点击调 `POST /api/tasks/generate`。
4. `levelUp/stageUp=true` 时播放升级动画。
5. `intent` 可用于前端埋点或更细的交互（例如 `task_request` 时突出任务入口）。
6. AI 失败时（502/503/504）直接展示响应 `message`，并把内容放回输入框方便重试。
7. 后端开发地址 `http://localhost:8080`，Vite dev 用 proxy `/api`。
8. 生产构建使用 hash 路由（`/#/chat`），静态托管无需配置 404 回退；API 地址通过 `VITE_API_BASE` 注入。
9. 触发 429 时展示「休息一下」提示，不重试。
