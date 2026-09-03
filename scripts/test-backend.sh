#!/usr/bin/env bash
# =============================================================================
# BioTree - Backend unit and integration tests
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/test-backend.sh
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
