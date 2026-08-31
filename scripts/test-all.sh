#!/usr/bin/env bash
# =============================================================================
# BioTree - 前后端测试（先后端后前端）
# Author: chen-xiang
# Created: 2026-08-31
# 用法：./scripts/test-all.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo
echo "[BioTree] test-all"
echo

"$ROOT/scripts/test-backend.sh"
"$ROOT/scripts/test-frontend.sh"

echo
echo "[OK] All checks passed."
