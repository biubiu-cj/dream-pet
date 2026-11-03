# DreamPet CloudBase 部署说明

> ⚠️ **当前项目尚未实际部署**。本地开发与真实 AI 验证已就绪，以下为在腾讯云 CloudBase 上人工部署的完整步骤。
> 部署涉及你的账号授权与（可能的）少量费用，请在本人操作并确认后再执行。

## 0. 前置信息（2026-11 核实）

| 项目 | 说明 |
|---|---|
| 免费体验版 | 1 个环境，3000 资源点/月（≈ ¥3）；需兑换码创建；分享可 0 元续 6 个月 |
| 数据库 | **MySQL 自 2026-10-20 起停止新启用**；使用 **CloudBase PostgreSQL** |
| 云托管 | 支持 Spring Boot 容器部署；提供内网直连 PostgreSQL |
| 静态托管 | 部署 `frontend/dist` 构建产物 |
| 成本 | 不购买套餐、不开启自动超额付费；超出免费额度会消耗资源点，需自行监控 |

## 1. 创建环境

1. 登录 [CloudBase 控制台](https://tcb.cloud.tencent.com/)；
2. 使用免费体验版兑换码创建「个人版」环境；
3. 记录 **环境 ID**（后续上传前端 / CLI 部署使用）。

## 2. 启用 PostgreSQL 并建表

1. 控制台 → 数据库 → PostgreSQL → 初始化（记录数据库名、用户名、密码）；
2. 开启「直连服务」，获取 **内网地址 + 端口(5432)**（云托管内网访问；本地调试可用公网地址）；
3. 执行建表脚本 `backend/src/main/resources/db/schema-postgres.sql`（可用 PG 控制台 SQL 窗口或 `psql`）。
   脚本可重复执行；已有旧库升级时同样执行一次，其中包含 `ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS message_type ...`。

## 3. 部署后端（云托管）

**方式 A：代码包部署（最简单）**

1. 本地构建：`cd backend && mvn clean package -DskipTests`；
2. 云托管 → 新建服务 → 上传代码包 → 选择 `backend/` 目录（含 Dockerfile）；
3. 端口填写 **8080**；
4. 配置环境变量（见下表）；
5. 部署完成后拿到后端访问域名，供前端配置。

**方式 B：本地 Docker 验证后推送镜像**

```bash
cd backend
docker build -t dreampet-backend:latest .
# 显式 -e 覆盖任何本地 env 文件中的 profile，避免误用 local/H2
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod --env-file ../.env dreampet-backend:latest
```

> 镜像已默认 `SPRING_PROFILES_ACTIVE=prod`；但如果 `--env-file` 里写了 `local`，会覆盖镜像默认值并退回 H2。
> 因此上面显式加了 `-e SPRING_PROFILES_ACTIVE=prod`。部署用的 env 文件建议直接从仓库模板复制后删除/注释 `SPRING_PROFILES_ACTIVE=local`。

**环境变量清单：**

| 变量 | 必填 | 示例 / 说明 |
|---|---|---|
| SPRING_PROFILES_ACTIVE | 是 | `prod`（Dockerfile 已默认 prod，仍建议在控制台显式设置） |
| DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD | 是 | PostgreSQL 内网连接信息 |
| DB_INIT_MODE | 否 | 首次可设 `always` 自动建表，之后改 `never` |
| LLM_API_KEY / LLM_BASE_URL / LLM_MODEL | 是 | 仅真实文本大模型，绝不写进前端或 Git |
| LLM_READ_TIMEOUT_MS | 否 | 读超时，默认 30000；超时自动重试 1 次 |
| LLM_MAX_TOKENS | 否 | 单次回复上限，默认 600 |
| INTENT_LLM_ENABLED | 否 | 生产默认 `false`（规则意图识别，省调用） |
| CHAT_RATE_LIMIT / TASK_RATE_LIMIT | 否 | 每用户每小时聊天/任务生成上限，默认 40 / 12 |
| GREETING_IDLE_MINUTES | 否 | 「欢迎回来」空闲阈值，默认 30 |
| AUTH_SECRET | 是 | 32 位以上随机字符串 |
| CORS_ALLOWED_ORIGINS | 是 | 前端静态托管域名（多个用逗号分隔） |
| PORT | 否 | 默认 8080 |

> 限流为**单实例内存计数**。免费版通常只跑 1 个实例，正好适用；若扩容多实例，需改为外部存储（不推荐在免费版做）。

## 4. 部署前端（静态网站托管）

```bash
cd frontend
npm install

# 关键：静态托管与云托管不同域名，必须注入后端 API 地址
# Git Bash / macOS / Linux
VITE_API_BASE=https://<云托管后端域名>/api npm run build

# Windows PowerShell
$env:VITE_API_BASE="https://<云托管后端域名>/api"; npm run build
```

1. 云开发 → 静态网站托管 → 上传 `dist/` 全部文件（或 `tcb hosting deploy dist -e <envId>`）；
2. 记录静态托管默认域名（如 `https://xxx-xxx.tcloudbaseapp.com`）；
3. 把该域名填入后端 `CORS_ALLOWED_ORIGINS` 并重启云托管服务；
4. 本项目已使用 **hash 路由**（如 `/#/chat`），静态托管无需配置 404 回退，直接刷新任意页面都能打开；
5. 已在 `dist/` 中生成 PWA 的 `manifest.webmanifest`、`sw.js` 与图标，可安装到主屏幕。

> 如果后端域名之后变化，重新执行上面的构建命令即可，不需要改代码。

## 5. 验证清单

- [ ] prod 缺少 `AUTH_SECRET` / `CORS_ALLOWED_ORIGINS` 时**拒绝启动**（日志出现「启动安全自检未通过」，不应出现 Tomcat started）
- [ ] 配置齐全后启动日志出现「启动安全自检通过：密钥已自定义、CORS 已限定、未使用 H2」
- [ ] `GET https://<后端域名>/api/health` 返回 `{"status":"ok","db":"up"}`
- [ ] `GET https://<后端域名>/api/system/info` 返回 `mode=real`
- [ ] 前端首页可打开；登录页正常；刷新 `/#/chat`、`/#/growth` 不 404
- [ ] 一键体验 → 创建伙伴 → 聊天 → 生成任务 → 开始任务 → 完成任务 → 成长档案数据正确
- [ ] 成长页显示「本周小结」与成就徽章；首页显示按天汇总的「最近几天」
- [ ] 重新进入（或重启云托管实例）后欢迎语引用真实历史
- [ ] 两个不同游客账号数据互不可见
- [ ] 高频调用触发 429 友好提示（可用脚本快速验证）
- [ ] 连续聊天后模型响应正常（读超时 30s，失败会自动重试 1 次）

## 6. 成本与运维注意

- 免费体验版有 **3000 资源点/月**：云托管实例常驻、数据库、静态托管流量都会消耗；比赛演示期够用，演示结束后可停用服务。
- 不开启自动按量付费；到期前按官方指引用分享活动续期或了解升级政策。
- LLM 调用成本控制：已做 10 分钟相同 Prompt 缓存、聊天上下文截断、意图识别规则优先（生产默认关闭 LLM 兜底）、
  记忆提取启发式节流（普通闲聊不再额外调用）、失败重试仅限 1 次、每用户每小时调用限流。
- API Key 仅存在于云托管环境变量，日志不输出。
- 演示前先访问一次 `/api/health` 预热实例，避免冷启动拖慢第一句对话。
- 演示结束建议停止云托管服务/环境，避免空耗资源点。

## 7. 常见问题

| 现象 | 处理 |
|---|---|
| 前端请求 401 | 检查 Token 是否过期；重新登录 |
| CORS 报错 | 检查 `CORS_ALLOWED_ORIGINS` 是否包含前端域名（含协议） |
| 后端启动失败（数据库） | 检查 DB_* 环境变量、直连服务是否开启、云托管与数据库是否同 VPC |
| 闲聊回复慢 | 真实模式下模型响应时间波动；读超时 30s 自动重试 1 次，前端展示「伙伴思考中」 |
| 刷新页面 404 | 已改为 hash 路由，应不会出现；若仍异常，检查是否用了旧版本 dist |
| 提示「休息一下」 | 触发每小时调用限流，属预期保护；可调大 `CHAT_RATE_LIMIT` |
| H2 数据在容器里丢失 | 生产必须用 PostgreSQL；容器本地文件不是正式存储 |