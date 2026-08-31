#!/usr/bin/env bash
# =============================================================================
# BioTree - Catalogue of Life 全量导入（动物界 + 植物界）
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-08-31 显式开启完整阶元与 DwC 扩展导入
# 用法：./scripts/import-col-full.sh
# 前置：JDK 21、MySQL（库 biotree）、网络（若需下载数据包）
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DWCA="$ROOT/data/import/col_latest_dwca.zip"
DWCA_URL="https://download.checklistbank.org/col/latest_dwca.zip"
BACKEND="$ROOT/backend"

echo
echo "[BioTree] Catalogue of Life full import"
echo "Root   : $ROOT"
echo "Package: $DWCA"
echo

if [[ ! -f "$BACKEND/gradlew" ]]; then
  echo "[ERROR] gradlew not found under backend/"
  exit 1
fi

mkdir -p "$ROOT/data/import"

if [[ ! -f "$DWCA" ]]; then
  echo "[INFO] DwC-A package missing, downloading..."
  echo "       $DWCA_URL"
  if command -v curl >/dev/null 2>&1; then
    curl -L --retry 3 --retry-delay 5 -o "$DWCA" "$DWCA_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$DWCA" "$DWCA_URL"
  else
    echo "[ERROR] Need curl or wget to download the package."
    exit 1
  fi
  if [[ ! -f "$DWCA" ]]; then
    echo "[ERROR] Download failed. Please save the zip to:"
    echo "        $DWCA"
    exit 1
  fi
  echo "[INFO] Download finished."
else
  echo "[INFO] Found existing package, skip download."
fi

echo
echo "[WARN] This will REPLACE existing taxon / i18n / media / distribution data."
echo "       Imports full ranks + vernaculars/synonyms/descriptions/distributions/media."
echo "       Full import may take a long time and use significant disk/CPU."
echo
read -r -p "Type YES to continue: " CONFIRM
if [[ "$CONFIRM" != "YES" ]]; then
  echo "[INFO] Cancelled."
  exit 0
fi

cd "$BACKEND"
chmod +x ./gradlew 2>/dev/null || true
echo
echo "[INFO] Starting import (rank-mode=full, max-per-rank=0, replace=true)..."
./gradlew bootRun --args="--app.import.enabled=true --app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=true --app.import.resume=false --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
EXITCODE=$?

echo
if [[ "$EXITCODE" -eq 0 ]]; then
  echo "[OK] Import process finished."
else
  echo "[ERROR] Import failed with exit code $EXITCODE."
fi

exit "$EXITCODE"
