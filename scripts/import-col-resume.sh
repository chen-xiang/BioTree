#!/usr/bin/env bash
# =============================================================================
# BioTree - Catalogue of Life resume import (no replace)
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-08-31 Enable full ranks and DwC extension import
# Updated: 2026-09-01 Switch to gradle importCol, no web server
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/import-col-resume.sh
# Requires: an existing checkpoint; DwC-A zip present; do not mix with replace=true
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DWCA="$ROOT/data/import/col_latest_dwca.zip"
BACKEND="$ROOT/backend"

echo
echo "[BioTree] CoL import resume"
echo "Root   : $ROOT"
echo "Package: $DWCA"
echo

if [[ ! -f "$BACKEND/gradlew" ]]; then
  echo "[ERROR] gradlew not found under backend/"
  exit 1
fi

if [[ ! -f "$DWCA" ]]; then
  echo "[ERROR] DwC-A package not found:"
  echo "        $DWCA"
  echo "        Run ./scripts/import-col-full.sh first, or place the zip manually."
  exit 1
fi

cd "$BACKEND"
chmod +x ./gradlew 2>/dev/null || true
echo "[INFO] Starting resume import via gradle importCol (no web port, replace=false)..."
exec ./gradlew importCol --args="--app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=false --app.import.resume=true --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
