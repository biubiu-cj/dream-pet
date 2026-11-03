# DreamPet CloudBase 部署指南（免费体验版）

> ℹ️ **当前实际部署（2026-11-03）已改在已有阿里云服务器上完成**，访问地址：
> `http://8.134.105.111/dreampet/`。具体步骤见文末「11. 阿里云服务器部署（当前采用）」。
>
> 下面第 1–10 节保留为 **CloudBase 备选方案**。如需改用 CloudBase，请勿购买付费套餐或开启自动超额付费。

> 目标架构（CloudBase 方案）：**前端静态托管 + 后端云托管（Docker）+ CloudBase PostgreSQL**，全部在 1 个免费体验环境中。

## 0. 前置信息（2026-11 核实）

| 项目 | 说明 |
|---|---|
| 免费体验版 | 1 个环境，约 3000 资源点/月（≈¥3）；需兑换码创建；分享可 0 元续 6 个月 |
| 数据库 | MySQL 自 2026-10-20 起停止新启用；本项目使用 **CloudBase PostgreSQL** |
| 云托管 | 支持 Spring Boot 容器部署（项目已提供 `backend/Dockerfile`），端口 **8080** |
| 静态托管 | 上传 `frontend/dist`；项目使用 hash 路由，无需配置 404 回退 |
| 成本 | 云托管实例、PostgreSQL、静态流量都会消耗资源点；演示结束请停用服务 |

## 1. 部署顺序总览（照着打勾）

1. 创建免费体验环境（记录环境 ID）
2. **先启用静态网站托管**，拿到默认域名（后面要用作后端 CORS）
3. 启用 PostgreSQL、记录直连信息、执行建表 SQL
4. 云托管部署后端，配置环境变量与健康检查
5. 用后端域名构建前端，上传 `dist` 到静态托管
6. 验证全流程；首次建表后把 `DB_INIT_MODE` 改回 `never`

> 为什么先启用静态托管：后端的启动安全自检要求 `CORS_ALLOWED_ORIGINS` 是明确域名，不能为空或 `*`。先拿到静态域名再部署后端，可以一次通过。

## 2. 创建环境

1. 登录 [CloudBase 控制台](https://tcb.cloud.tencent.com/)；
2. 使用免费体验版兑换码创建「个人版」环境；
3. 记录 **环境 ID**（CLI 上传前端时需要）。

## 3. 启用静态网站托管（先拿域名）

1. 控制台 → 静态网站托管 → 开通；
2. 复制默认访问域名（形如 `https://<env-id>.tcloudbaseapp.com`，以控制台显示为准）；
3. 先不用上传文件，第 6 步再上传。

## 4. 启用 PostgreSQL 并建表

1. 控制台 → 数据库 → PostgreSQL → 初始化，记录：**数据库名、用户名、密码**；
2. 找到连接信息：**主机地址、端口（默认 5432）**。云托管访问建议使用内网/直连地址；
3. 在 PostgreSQL 的 SQL 控制台执行 `backend/src/main/resources/db/schema-postgres.sql`。
   - 脚本可重复执行；旧库升级也会自动补 `message_type` 列；
   - 如果控制台要求 SSL，给后端加 `DB_PARAMS=?sslmode=require`（见下一节）。

## 5. 部署后端（云托管）

### 5.1 上传代码

1. 控制台 → 云托管 → 新建服务；
2. 部署方式选「**上传代码包**」→ 代码包类型选「**文件夹**」；
3. 选择本地 `backend/` 目录（必须包含 `Dockerfile`）；
4. 端口填写 **8080**（项目镜像默认监听 8080）。

### 5.2 健康检查

- 健康检查路径填 **`/api/health`**（会检查数据库连接）；
- 如果控制台只允许探测根路径，项目已提供 `GET /` 返回 200，可直接使用。

> 首次访问可能冷启动较慢，部署完成后先访问一次 `/api/health` 预热。

### 5.3 环境变量（密钥只放这里，绝不写进仓库）

| 变量 | 必填 | 示例 / 说明 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 是 | `prod`（镜像已默认，仍建议显式设置） |
| `DB_HOST` | 是 | PostgreSQL 直连主机地址 |
| `DB_PORT` | 否 | 默认 `5432` |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | 是 | PostgreSQL 信息 |
| `DB_PARAMS` | 否 | 如 `?sslmode=require`；不需要则留空 |
| `DB_INIT_MODE` | 否 | **首次部署设为 `always`**，建表成功后改回 `never` |
| `AUTH_SECRET` | 是 | 32 位以上随机字符串，例如 `openssl rand -hex 32` 或 `python -c "import secrets;print(secrets.token_hex(32))"` |
| `CORS_ALLOWED_ORIGINS` | 是 | 静态托管域名，如 `https://<env-id>.tcloudbaseapp.com`（不要带结尾斜杠，不要用 `*`） |
| `LLM_API_KEY` | 是 | 文本大模型 Key（DeepSeek 等） |
| `LLM_BASE_URL` | 是 | 如 `https://api.deepseek.com/v1` |
| `LLM_MODEL` | 是 | 如 `deepseek-chat` |
| `LLM_READ_TIMEOUT_MS` | 否 | 默认 `30000` |
| `INTENT_LLM_ENABLED` | 否 | 生产默认 `false`（规则意图识别，省调用） |
| `GREETING_IDLE_MINUTES` | 否 | 演示可设 `0`，让「欢迎回来」立即触发 |
| `CHAT_RATE_LIMIT` / `TASK_RATE_LIMIT` | 否 | 每用户每小时上限，默认 40 / 12 |
| `PORT` | 否 | 默认 `8080` |

### 5.4 网络

- 如果使用 PostgreSQL **内网地址**：云托管服务需开启**私有网络（VPC）**并选择与数据库相同的 VPC/子网；
- 如果控制台提供了公网连接地址，也可以先用公网地址完成演示（注意安全组/白名单）；
- 数据库安全组/白名单需放行云托管网段。

### 5.5 启动自检

后端启动时会做安全自检，以下任一不满足会**拒绝启动**并在日志给出原因：

1. `AUTH_SECRET` 缺失 / 等于公开默认值或示例占位值 / 少于 16 位；
2. `CORS_ALLOWED_ORIGINS` 为空或包含 `*`；
3. 检测到 H2 数据源或 H2 控制台（容器已默认 `SPRING_PROFILES_ACTIVE=prod`，一般不会触发）。

看到「启动安全自检通过」即代表生产配置正确。

## 6. 构建并部署前端

### 6.1 构建（必须注入后端域名）

```bash
cd frontend
npm ci

# Git Bash / macOS / Linux
VITE_API_BASE=https://<云托管后端域名>/api npm run build
```

Windows PowerShell：

```powershell
$env:VITE_API_BASE="https://<云托管后端域名>/api"; npm run build
```

> 后端域名在云托管服务详情页获取。必须带 `/api` 后缀，且使用 `https://`。

### 6.2 上传

控制台 → 静态网站托管 → 上传 `frontend/dist` 下的全部文件。

或使用 CLI（可选）：

```bash
npm i -g @cloudbase/cli
tcb login
tcb hosting deploy dist -e <环境ID>
```

> 项目已使用 hash 路由（`/#/chat`），静态托管**不需要**配置 404 → index.html。`dist/` 已包含 PWA 的 `manifest.webmanifest`、`sw.js` 与图标。

### 6.3 回填 CORS

如果静态托管域名与第 5 步填写的不一致（例如换了自定义域名），把正确域名填入后端 `CORS_ALLOWED_ORIGINS` 并重启云托管服务。

## 7. 验证清单

- [ ] `GET https://<后端域名>/api/health` → `{"status":"ok","db":"up"}`
- [ ] `GET https://<后端域名>/` → 200（供平台健康检查）
- [ ] `GET https://<后端域名>/api/system/info` → `mode=real`
- [ ] 打开静态托管域名：登录页正常，无白屏/资源 404
- [ ] 一键体验 → 创建伙伴 → 聊天 → 生成任务 → 开始任务 → 完成 → 成长档案数据正确
- [ ] 成长页「本周小结 / 成就徽章」、日记「每日回顾」正常
- [ ] 聊天页「新对话」可用；聊天左右侧栏（桌面）正常
- [ ] 刷新 `/#/chat`、`/#/growth` 不 404
- [ ] 退出登录 / 重新登录 / 重启后端后数据仍在
- [ ] 两个游客账号数据互不可见
- [ ] 高频调用触发 429 友好提示
- [ ] 浏览器控制台无 CORS 报错

## 8. 常见问题

| 现象 | 排查 |
|---|---|
| 云托管启动失败 | 看日志：安全自检原因（AUTH_SECRET/CORS/H2）、数据库连接失败、端口不是 8080 |
| 数据库连不上 | 检查 DB_*、直连服务是否开启、VPC/子网、白名单；必要时加 `DB_PARAMS=?sslmode=require` |
| 前端请求 404 | `VITE_API_BASE` 是否带 `/api`、是否指向正确的云托管域名；重新构建后再上传 |
| CORS 报错 | `CORS_ALLOWED_ORIGINS` 是否为静态托管**完整 origin**（含 `https://`、无结尾 `/`） |
| 聊天 502/504 | LLM Key/模型名/地址是否正确；`LLM_READ_TIMEOUT_MS` 与云托管请求超时；可先重试 |
| 白屏 / 刷新 404 | 确认上传的是 `dist` 全部文件；项目为 hash 路由，不应出现路径 404 |
| 健康检查失败 | 路径用 `/api/health` 或 `/`；确认容器端口 8080；数据库已初始化 |
| 第一句回复很慢 | 冷启动；部署后先访问一次 `/api/health` 预热 |

## 9. 成本与运维

- 免费体验版约 **3000 资源点/月**：云托管实例、PostgreSQL、静态流量都会消耗；演示期间够用，**演示结束请在控制台停用云托管服务/实例**。
- 不购买套餐、不开启自动超额付费；如控制台要求升级付费才能使用 PostgreSQL，请先暂停并告知，不要直接购买。
- AI 调用成本控制已内置：10 分钟相同 Prompt 缓存、意图规则优先、记忆提取节流、每用户每小时限流、失败重试 1 次。
- 密钥仅存在于云托管环境变量，日志不输出；不要提交到 Git。

## 10. 需要你手动完成

1. 用你的 CloudBase 账号创建免费体验环境（兑换码）；
2. 初始化 PostgreSQL 并执行建表脚本；
3. 在云托管配置环境变量（尤其是 `AUTH_SECRET`、`CORS_ALLOWED_ORIGINS`、`LLM_API_KEY`）；
4. 获取后端域名后构建前端并上传静态托管；
5. 可选：`tcb login` 后由 CLI 协助上传。

## 11. 阿里云服务器部署（当前采用）

访问地址：**http://8.134.105.111/dreampet/**

服务器为 Ubuntu 24.04（2 vCPU / 1.7G 内存），已有 `software-market` 服务占用 80 和 8080 端口，DreamPet 以独立服务并行部署，互不影响。

### 11.1 部署形态

| 组件 | 位置 / 说明 |
|---|---|
| 前端静态文件 | `/opt/dreampet/www/dreampet/` |
| 后端 jar | `/opt/dreampet/app.jar`（Java 17，端口 8082，仅监听 127.0.0.1） |
| 数据库 | H2 文件库 `/opt/dreampet/data/dreampet.mv.db` |
| 环境变量 | `/opt/dreampet/dreampet.env`（600，含 AUTH_SECRET / LLM Key，勿提交仓库） |
| systemd 服务 | `dreampet.service` |
| Nginx | `/etc/nginx/snippets/dreampet.conf`，挂载在 80 端口的 `/dreampet/` 子路径 |
| 原项目配置备份 | `/root/nginx-default.backup-*` |

### 11.2 常用运维命令

```bash
systemctl status dreampet
systemctl restart dreampet
journalctl -u dreampet -f
curl http://127.0.0.1:8082/api/health
nginx -t && systemctl reload nginx
```

### 11.3 更新发布

```bash
# 本机（Windows Git Bash 需要关闭路径转换）
cd backend && mvn -q -DskipTests package
cd ../frontend && MSYS_NO_PATHCONV=1 VITE_BASE=/dreampet/ VITE_API_BASE=/dreampet/api npm run build

scp backend/target/dreampet-backend-1.0.0.jar root@8.134.105.111:/opt/dreampet/app.jar
tar -C frontend/dist -czf /tmp/dreampet-dist.tar.gz .
scp /tmp/dreampet-dist.tar.gz root@8.134.105.111:/tmp/
ssh root@8.134.105.111 'tar -xzf /tmp/dreampet-dist.tar.gz -C /opt/dreampet/www/dreampet && chown -R www-data:www-data /opt/dreampet && systemctl restart dreampet'
```

### 11.4 注意事项

- **不要修改** `/etc/nginx/sites-available/default` 的根路径与原有 `location`，DreamPet 只通过 `/dreampet/` 前缀接入；改动前先备份并执行 `nginx -t`。
- 后端使用 `local` profile + H2 文件库，适合比赛演示；如需真正生产化，可加装 PostgreSQL 并切换到 `prod` profile。
- 当前服务器资源较紧（1.7G 内存），DreamPet JVM 限制为 `-Xmx384m`；演示结束可 `systemctl stop dreampet` 释放内存。
- 8082 已绑定 127.0.0.1，外部只能通过 Nginx 的 `/dreampet/api/` 访问。