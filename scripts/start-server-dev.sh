#!/usr/bin/env bash
# =============================================================================
# BioTree - Start the backend dev server (Spring Boot)
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-01 Point import to standalone scripts
# Updated: 2026-09-01 Rename script to start-server-dev
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/start-server-dev.sh
# Requires: JDK 21, MySQL (database biotree, see application.yml)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/backend"

echo
echo "[BioTree] start-server-dev"
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
echo "       Import is a separate process: ./scripts/import-col-full.sh / import-col-resume.sh"
echo
exec ./gradlew bootRun
