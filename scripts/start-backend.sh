#!/usr/bin/env bash
# DreamPet 后端启动脚本（Linux / macOS / Git Bash）
set -e
cd "$(dirname "$0")/../backend"

command -v java >/dev/null 2>&1 || { echo "[错误] 未找到 Java，请安装 JDK 17"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "[错误] 未找到 Maven，请安装 Maven 3.9+"; exit 1; }
mkdir -p data

if [ -z "$LLM_API_KEY" ]; then
  echo "[提示] 未设置 LLM_API_KEY，AI 接口将返回 503。"
  echo "       可先执行: export LLM_API_KEY=你的Key"
fi

echo "============================================"
echo " DreamPet 后端启动中（真实 AI 模式）"
echo " 地址: http://localhost:8080"
echo " 按 Ctrl+C 停止"
echo "============================================"
export SPRING_PROFILES_ACTIVE=local
exec mvn spring-boot:run