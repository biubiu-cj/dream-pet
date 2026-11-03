#!/usr/bin/env bash
# DreamPet 前端启动脚本（Linux / macOS / Git Bash）
set -e
cd "$(dirname "$0")/../frontend"

command -v node >/dev/null 2>&1 || { echo "[错误] 未找到 Node.js，请安装 Node 18+"; exit 1; }
if [ ! -d node_modules ]; then
  echo "首次运行，正在安装依赖..."
  npm install
fi

echo "============================================"
echo " DreamPet 前端启动中"
echo " 地址: http://localhost:5173"
echo " 请确保后端已在 8080 端口启动"
echo " 按 Ctrl+C 停止"
echo "============================================"
exec npm run dev