#!/usr/bin/env bash
# =============================================================================
# BioTree - Catalogue of Life 断点续跑（不 replace）
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-08-31 显式开启完整阶元与 DwC 扩展导入
# 用法：./scripts/import-col-resume.sh
# 前置：已有 checkpoint；DwC-A 包存在；勿与 replace=true 混用
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
echo "[INFO] Starting resume import (replace=false, resume=true, rank-mode=full)..."
exec ./gradlew bootRun --args="--app.import.enabled=true --app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=false --app.import.resume=true --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
