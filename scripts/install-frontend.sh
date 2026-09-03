#!/usr/bin/env bash
# =============================================================================
# BioTree - Install frontend dependencies
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/install-frontend.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$ROOT/frontend"

echo
echo "[BioTree] install-frontend"
echo

if [[ ! -f "$FRONTEND/package.json" ]]; then
  echo "[ERROR] frontend/package.json not found."
  exit 1
fi

if ! command -v pnpm >/dev/null 2>&1; then
  echo "[ERROR] pnpm not found. Install: npm i -g pnpm"
  exit 1
fi

cd "$FRONTEND"
exec pnpm install
