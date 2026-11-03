# DreamPet 技术架构说明

> 宣传语：每一次陪伴，都是一次成长。

## 1. 总体架构

```
┌─────────────────────────┐        ┌──────────────────────────────┐
│  前端 Vue 3 + TS + Vite  │  HTTP  │  后端 Spring Boot 3 (Java 17) │
│  Pinia / SCSS / Axios   │──────▶│  CloudBase 云托管（容器部署）    │
│  CloudBase 静态网站托管   │  JSON  │                              │
└─────────────────────────┘        │ ┌──────────────────────────┐ │
   宠物形象：本地角色素材 + CSS 动画  │ │ Controller 层（REST API） │ │
   （不调用任何图片/语音生成模型）     │ ├──────────────────────────┤ │
                                   │ │ Agent 层                  │ │
                                   │ │  CompanionAgent 对话       │ │
                                   │ │  TaskAgent     任务生成    │ │
                                   │ │  MemoryManager 长期记忆    │ │
                                   │ │  SafetyGuard   儿童安全    │ │
                                   │ │  IntentRecognizer 意图识别 │ │
                                   │ ├──────────────────────────┤ │
                                   │ │ LLM 统一封装 LlmService    │ │
                                   │ │  · OpenAI 兼容真实模型      │ │
                                   │ │  · 结构化失败/重试/缓存     │ │
                                   │ ├──────────────────────────┤ │
                                   │ │ MyBatis-Plus 数据访问      │ │
                                   │ └──────────────────────────┘ │
                                   └──────────┬───────────────────┘
                                              │ JDBC
                              ┌───────────────┴────────────────┐
                              │ 本地: H2 文件库（零成本演示）      │
                              │ 生产: CloudBase PostgreSQL       │
                              └────────────────────────────────┘
```

## 2. CloudBase 能力核实结论（2026-11）

| 项目 | 结论 |
|---|---|
| 免费体验版 | 可创建 1 个环境，3000 资源点/月（≈¥3），分享可 0 元续 6 个月 |
| MySQL | **2026-10-20 起停止新启用** ❌ |
| PostgreSQL | 官方主推 ✅，支持云托管内网直连（JDBC/PostgreSQL 协议） |
| 云托管 | 支持 Spring Boot 容器部署（官方有 Spring Boot 指南）✅ |
| 静态托管 | 支持 Vue 构建产物 ✅ |
| 环境变量 | 云托管支持配置环境变量（API Key 不入库不进 Git）✅ |

**决策：后端 Spring Boot 容器 → 云托管；数据库 → CloudBase PostgreSQL；前端 → 静态托管。**

## 3. 关键设计原则

1. **成本最小化**：只依赖一个 OpenAI 兼容文本大模型 API；本地开发用 H2 文件库，不装任何中间件。
2. **仅真实模型 + 可观测失败**：运行时只使用 OpenAI 兼容文本大模型；未配置 Key / 认证失败 / 限流 / 超时 / 上游 5xx 均转换为 502/503/504 结构化错误，前端直接展示友好提示；超时与 5xx 自动重试 1 次。离线单测使用测试源码内的确定性客户端，不访问网络。
3. **成长数值后端说了算**：经验、等级、阶段全部由 `GrowthService` 计算，LLM 只产出任务文案与反馈文字，且奖励值经过服务端上下限校验。
4. **长期记忆分层**：兴趣记忆 / 事件记忆 / 任务记忆 / 对话摘要 / 伙伴档案，5 类结构化存储，按需组装进 Prompt，绝不全量塞聊天记录。
5. **儿童安全**：SafetyGuard 输入输出双向规则过滤，危险话题温和引导寻求可信任成年人；不收集个人身份信息。
6. **意图驱动**：IntentRecognizer 规则优先（11 类意图），必要时调用一次短模型请求兜底；意图为 `task_request` 时触发任务推荐，`memory_recall`/`emotion_negative` 等会改变回应策略。
7. **数据隔离**：所有查询强制携带 `user_id` 条件，Token 解析用户，接口层不接受客户端传入 userId。

## 4. Agent 调用流程

聊天：
```
用户消息 → SafetyGuard.checkInput
        → IntentRecognizer.recognize（规则优先，必要时短 LLM 请求兜底）
        → MemoryManager.assembleContext（伙伴档案+兴趣+事件+摘要+近10条对话+进行中任务）
        → CompanionAgent.reply（真实 LLM，系统提示中携带意图与回应策略）
        → SafetyGuard.checkOutput
        → MemoryManager.extractAndSave（异步：真实 LLM 提取兴趣/事件/画像）
        → GrowthService.addChatExp（少量经验，每日有上限）
        → 意图为 task_request 且无进行中任务时生成任务建议
        → 返回前端（含 intent / intentLabel）
```
```

任务：
```
GET /api/tasks/today（无任务时）或 POST /api/tasks/generate
   → TaskAgent.generate（输入兴趣+历史任务，避免重复）
   → 结构化 JSON 校验 + 服务端合理性检查（难度1~3、奖励5~30 截断）
   → 保存 tasks 表 → 前端展示
完成任务 POST /api/tasks/{id}/complete
   → 状态机校验（防重复完成，条件 UPDATE）
   → GrowthService：加经验→判升级→判阶段→写 growth_events
   → CompanionAgent.feedback（个性化祝贺）
```

## 5. 成长数值（后端常量，见 GrowthService）

| 阶段 | key | 等级 | 升级所需累计经验 |
|---|---|---|---|
| 初识伙伴 | seed | Lv.1~2 | 0 / 30 |
| 好奇伙伴 | sprout | Lv.3~4 | 80 / 150 |
| 默契伙伴 | bloom | Lv.5~7 | 250 / 380 / 550 |
| 梦想伙伴 | dream | Lv.8~10 | 760 / 1020 / 1350 |

经验来源：完成任务 = 任务奖励值（5~30）；聊天互动 = 每次 2 点、每日上限 20 点；首次创建伙伴 = 10 点。

## 6. 目录结构

```
dream-pet/
├── backend/                  # Spring Boot 后端
│   ├── pom.xml
│   ├── Dockerfile            # 云托管容器部署
│   └── src/main/
│       ├── java/com/dreampet/
│       │   ├── agent/        # CompanionAgent/TaskAgent/MemoryManager/SafetyGuard/IntentRecognizer/PromptTemplates
│       │   ├── common/       # ApiResponse/BizException/GlobalExceptionHandler/UserContext
│       │   ├── config/       # Web/CORS/拦截器/MyBatisPlus 配置
│       │   ├── controller/   # REST API
│       │   ├── dto/          # 请求/响应对象
│       │   ├── entity/       # 8 张表实体
│       │   ├── llm/          # LlmService/LlmConfig/OpenAiCompatibleLlmClient/LlmException
│       │   ├── mapper/       # MyBatis-Plus Mapper
│       │   └── service/      # 业务服务
│       └── resources/
│           ├── application.yml / application-local.yml / application-prod.yml
│           └── db/schema-h2.sql / schema-postgres.sql
├── frontend/                 # Vue 3 前端
│   ├── package.json / vite.config.ts
│   ├── Dockerfile            # 备用（静态托管只需 dist）
│   └── src/
│       ├── api/ components/（含 PetSvg 全套形象）
│       ├── router/ stores/ styles/ views/
├── docs/                     # 架构/数据库/部署/记忆设计文档
├── scripts/                  # 本地启动脚本
└── .env.example
```

## 7. 安全与合规

- API Key 仅存在于后端环境变量，绝不进前端/Git/日志。
- 密码 BCrypt 哈希存储；Token 为 HMAC-SHA256 签名的轻量 JWT。
- 不收集真实姓名/学校/住址/联系方式；SafetyGuard 检测到孩子透露 PII 时温和提醒。
- 无签到惩罚、无消费诱导；任务轻松自愿。
- 支持删除数据（`DELETE /api/account` 级联删除当前用户全部数据）。
