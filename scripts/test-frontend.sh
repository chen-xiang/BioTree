#!/usr/bin/env bash
# =============================================================================
# BioTree - Frontend typecheck / lint / unit test / build
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/test-frontend.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$ROOT/frontend"

echo
echo "[BioTree] test-frontend"
echo

if [[ ! -f "$FRONTEND/package.json" ]]; then
  echo "[ERROR] frontend/package.json not found."
  exit 1
fi

if ! command -v pnpm >/dev/null 2>&1; then
  echo "[ERROR] pnpm not found."
  exit 1
fi

cd "$FRONTEND"

if [[ ! -d "$FRONTEND/node_modules" ]]; then
  echo "[INFO] Installing dependencies..."
  pnpm install
fi

echo "[1/4] typecheck"
pnpm typecheck

echo "[2/4] lint"
pnpm lint

echo "[3/4] test"
pnpm test

echo "[4/4] build"
pnpm build

echo
echo "[OK] Frontend checks passed."
