#!/usr/bin/env bash
# =============================================================================
# BioTree - pause-exit.sh 行为测试（嵌套 / CI 不 pause）
# Author: chen-xiang
# Created: 2026-09-01
# =============================================================================
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

code=0
BIOTREE_NESTED=1 "$HERE/pause-exit.sh" 3 || code=$?
if [[ "$code" -ne 3 ]]; then
  echo "[ERROR] nested pause-exit should return 3, got $code"
  exit 1
fi

code=0
CI=1 "$HERE/pause-exit.sh" 5 || code=$?
if [[ "$code" -ne 5 ]]; then
  echo "[ERROR] CI pause-exit should return 5, got $code"
  exit 1
fi

echo "[OK] pause-exit.sh"
exit 0
