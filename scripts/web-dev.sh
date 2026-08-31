#!/usr/bin/env bash
# =============================================================================
# BioTree - 启动前端开发服务器（Vite）
# Author: chen-xiang
# Created: 2026-08-31
# 用法：./scripts/web-dev.sh
# 前置：Node.js 20+、pnpm
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$ROOT/frontend"

echo
echo "[BioTree] web-dev"
echo "Root    : $ROOT"
echo "Frontend: $FRONTEND"
echo

if [[ ! -f "$FRONTEND/package.json" ]]; then
  echo "[ERROR] frontend/package.json not found."
  exit 1
fi

if ! command -v pnpm >/dev/null 2>&1; then
  echo "[ERROR] pnpm not found. Install Node.js and: npm i -g pnpm"
  exit 1
fi

cd "$FRONTEND"

if [[ ! -d "$FRONTEND/node_modules" ]]; then
  echo "[INFO] node_modules missing, running pnpm install..."
  pnpm install
fi

echo "[INFO] Starting Vite at http://localhost:5173"
echo "       Proxy: /api and /files -> http://localhost:8080"
echo
exec pnpm dev
