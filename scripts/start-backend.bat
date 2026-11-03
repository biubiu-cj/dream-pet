@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0..\backend"

where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 Java，请安装 JDK 17 并配置 JAVA_HOME
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 Maven，请安装 Maven 3.9+
  exit /b 1
)
if not exist "data" mkdir "data"

if "%LLM_API_KEY%"=="" (
  echo [提示] 未设置 LLM_API_KEY，AI 接口将返回 503。
  echo        可先执行: set LLM_API_KEY=你的Key
)

echo ============================================
echo  DreamPet 后端启动中（真实 AI 模式）
echo  地址: http://localhost:8080
echo  按 Ctrl+C 停止
echo ============================================
set SPRING_PROFILES_ACTIVE=local
call mvn spring-boot:run