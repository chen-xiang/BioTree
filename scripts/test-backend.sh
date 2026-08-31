#!/usr/bin/env bash
# =============================================================================
# BioTree - 后端单元/集成测试
# Author: chen-xiang
# Created: 2026-08-31
# 用法：./scripts/test-backend.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/backend"

echo
echo "[BioTree] test-backend"
echo

if [[ ! -f "$BACKEND/gradlew" ]]; then
  echo "[ERROR] backend/gradlew not found."
  exit 1
fi

cd "$BACKEND"
chmod +x ./gradlew 2>/dev/null || true
exec ./gradlew test --no-daemon
