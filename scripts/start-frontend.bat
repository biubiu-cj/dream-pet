@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0..\frontend"

where node >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 Node.js，请安装 Node 18+
  exit /b 1
)
if not exist "node_modules" (
  echo 首次运行，正在安装依赖...
  call npm install
  if errorlevel 1 exit /b 1
)

echo ============================================
echo  DreamPet 前端启动中
echo  地址: http://localhost:5173
echo  请确保后端已在 8080 端口启动
echo  按 Ctrl+C 停止
echo ============================================
call npm run dev