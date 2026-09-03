#!/usr/bin/env bash
# =============================================================================
# BioTree - Generate frontend schema.d.ts from offline openapi.yaml
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/openapi-generate.sh
# Optional: ./scripts/openapi-generate.sh live  (backend must be running)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$ROOT/frontend"
MODE="${1:-}"

echo
echo "[BioTree] openapi-generate"
echo "Mode: ${MODE:-yaml}"
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

if [[ "$MODE" == "live" ]]; then
  echo "[INFO] Generating from http://localhost:8080/v3/api-docs"
  exec pnpm openapi:generate:live
fi

echo "[INFO] Generating from openapi/openapi.yaml"
exec pnpm openapi:generate
