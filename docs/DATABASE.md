# DreamPet 数据库设计

## 1. 选型

| 环境 | 数据库 | 说明 |
|---|---|---|
| 本地开发 / 演示 | H2 文件模式（PostgreSQL 兼容） | 零成本、无需安装，数据在 `backend/data/dreampet.mv.db`，重启不丢失 |
| 生产（CloudBase） | **CloudBase PostgreSQL** | MySQL 自 2026-10-20 起停止新启用，PostgreSQL 为官方主推并支持云托管内网直连 |

- Schema 脚本：`backend/src/main/resources/db/schema-h2.sql`（本地自动执行）/ `schema-postgres.sql`（生产手动执行或 `DB_INIT_MODE=always`）。
- 所有脚本均使用 `CREATE TABLE IF NOT EXISTS`，可重复执行。
- 主键统一 `BIGSERIAL/BIGINT AUTO_INCREMENT`，由 MyBatis-Plus `IdType.AUTO` 回填。

## 2. 表结构（8 张）

### users — 用户
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| username | VARCHAR(32) UNIQUE | 2~20 位中英文/数字/下划线 |
| password_hash | VARCHAR(100) | BCrypt 哈希（从不返回前端） |
| created_at / updated_at | TIMESTAMP | 自动填充 |

### pets — 伙伴（每用户 1 只）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT UNIQUE | 用户隔离关键字段 |
| name | VARCHAR(32) | 伙伴名字 |
| species | VARCHAR(16) | fox/rabbit/cat/bear/dino |
| personality | VARCHAR(16) | curious/gentle/brave/clever/calm |
| level | INT | 1~10，后端计算 |
| experience | INT | 累计经验，后端计算 |
| growth_stage | VARCHAR(16) | seed/sprout/bloom/dream |
| avatar_key | VARCHAR(32) | 前端素材键（= species） |
| created_at / updated_at | TIMESTAMP | |

### user_interests — 兴趣记忆
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT | 索引 idx_interests_user |
| interest | VARCHAR(32) | |
| source | VARCHAR(16) | create/chat/task |
| created_at | TIMESTAMP | |

唯一约束 `(user_id, interest)` 实现兴趣去重。

### tasks — 任务记忆
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id / pet_id | BIGINT | 索引 `(user_id,status)`、`(user_id,created_at)` |
| title | VARCHAR(64) | |
| description | VARCHAR(500) | |
| category | VARCHAR(16) | knowledge/observe/creative/story/life/logic |
| difficulty | INT | 1~3（后端截断） |
| reward | INT | 5~30（后端截断） |
| status | VARCHAR(16) | pending/in_progress/completed |
| created_at / completed_at | TIMESTAMP | |

**防重复领取经验**：`UPDATE ... WHERE id=? AND user_id=? AND status<>'completed'`，影响行数为 0 即视为已完成。

### chat_messages — 聊天记录
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id / pet_id | BIGINT | 索引 `(pet_id, created_at)` |
| role | VARCHAR(16) | user/assistant/system |
| content | VARCHAR(2000) | |
| message_type | VARCHAR(16) | `normal`（默认）/ `greeting`（欢迎语）/ `session_start`（新对话分隔标记，不展示） |
| created_at | TIMESTAMP | |

> **为什么需要 `message_type`**：欢迎语去重原先用 `content.startsWith("欢迎回来")` 判断，等于让模型的自由输出承担控制流职责——模型换个说法（如「好久不见呀」）去重就失效，用户会被连续刷欢迎语。改为结构化标记后不再依赖文本内容。
>
> `session_start` 用于「开启新对话」：系统消息作为分隔点，读取上下文时只取该标记之后的消息；旧消息与长期记忆不删除。

**升级已有数据库**：两份 schema 均已带 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`，可重复执行，存量记录自动归为 `normal`，无需停机或回填：

```sql
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS message_type VARCHAR(16) NOT NULL DEFAULT 'normal';
```

### memories — 长期记忆
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id / pet_id | BIGINT | 索引 `(user_id, memory_type)` |
| memory_type | VARCHAR(16) | interest/event/profile/task/milestone |
| content | VARCHAR(500) | |
| importance | INT | 1~5，组装上下文时优先高重要度 |
| created_at / updated_at | TIMESTAMP | |

### growth_events — 成长事件（成长日记数据源）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id / pet_id | BIGINT | 索引 `(user_id, created_at)` |
| event_type | VARCHAR(24) | pet_created/first_chat/chat_exp/task_completed/level_up/stage_up/explore |
| description | VARCHAR(300) | |
| experience_gained | INT | 聊天每日经验上限依赖该字段按日汇总 |
| created_at | TIMESTAMP | |

### conversation_summaries — 对话摘要（滚动）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id / pet_id | BIGINT | 索引 `(pet_id)` |
| summary | VARCHAR(1000) | LLM 生成的滚动摘要 |
| covered_until | BIGINT | 已覆盖到的 chat_messages.id |
| created_at / updated_at | TIMESTAMP | |

## 3. 关系

```
users 1 —— 1 pets
users 1 —— N user_interests / tasks / chat_messages / memories / growth_events / conversation_summaries
pets  1 —— N tasks / chat_messages / memories / growth_events / conversation_summaries
```

不使用数据库外键（便于 CloudBase PostgreSQL 权限最小化与简化运维），由应用层保证归属关系；**所有查询都强制带 `user_id` 条件**。

## 4. 索引与数据隔离

- 业务查询全部使用 `(user_id, ...)` 复合条件，杜绝跨用户数据泄漏。
- 关键索引：任务状态/时间、聊天记录 `(pet_id, created_at)`、记忆 `(user_id, memory_type)`、成长事件按日统计。
- 删除账号（`DELETE /api/account`）级联删除该用户全部表数据，满足儿童隐私合规。

## 5. 初始化

```bash
# 本地：启动后端自动建表（application-local.yml -> spring.sql.init.mode=always）
cd backend && mvn spring-boot:run

# 生产：CloudBase PostgreSQL 执行一次 schema-postgres.sql
# （或在云托管环境变量设置 DB_INIT_MODE=always 首次启动自动建表，之后改回 never）
```

## 6. 测试数据

比赛演示无需预置数据：`POST /api/auth/guest` 一键创建体验账号，再通过创建伙伴接口生成完整数据。这样演示的每一步（兴趣提取、任务、成长、重启恢复）都基于真实持久化数据，而不是写死种子数据。