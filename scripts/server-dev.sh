#!/usr/bin/env bash
# =============================================================================
# BioTree - 启动后端开发服务（Spring Boot）
# Author: chen-xiang
# Created: 2026-08-31
# 用法：./scripts/server-dev.sh
# 前置：JDK 21、MySQL（库 biotree，见 application.yml）
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/backend"

echo
echo "[BioTree] server-dev"
echo "Root   : $ROOT"
echo "Backend: $BACKEND"
echo

if [[ ! -x "$BACKEND/gradlew" && ! -f "$BACKEND/gradlew" ]]; then
  echo "[ERROR] backend/gradlew not found."
  exit 1
fi

cd "$BACKEND"
chmod +x ./gradlew 2>/dev/null || true

echo "[INFO] Starting Spring Boot at http://localhost:8080"
echo "       OpenAPI: http://localhost:8080/v3/api-docs"
echo "       Health : http://localhost:8080/api/health"
echo "       Dev admin: admin / admin123  (non-prod)"
echo
exec ./gradlew bootRun
