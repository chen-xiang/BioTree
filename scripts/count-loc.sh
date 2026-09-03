#!/usr/bin/env bash
# =============================================================================
# BioTree - Count source lines (excludes deps and build output)
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/count-loc.sh
# Note: prefers cloc; falls back to find/wc per-extension counts
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo
echo "[BioTree] count-loc"
echo "Root: $ROOT"
echo

DIRS=("$ROOT/backend/src" "$ROOT/frontend/src" "$ROOT/docs" "$ROOT/scripts")

if command -v cloc >/dev/null 2>&1; then
  echo "[INFO] Using cloc..."
  cloc "${DIRS[@]}" --quiet
  exit $?
fi

echo "[INFO] cloc not found, using find/wc fallback..."
printf '%-14s%8s%12s\n' "Language" "Files" "Lines"
printf '%s\n' "----------------------------------"

count_group() {
  local name="$1"
  shift
  local files=0 lines=0
  local dir pat f n
  for dir in "${DIRS[@]}"; do
    [[ -d "$dir" ]] || continue
    for pat in "$@"; do
      while IFS= read -r -d '' f; do
        case "$f" in
          */node_modules/*|*/dist/*|*/build/*|*/.git/*) continue ;;
        esac
        files=$((files + 1))
        n=$(wc -l < "$f" | tr -d ' ')
        lines=$((lines + n))
      done < <(find "$dir" -type f -name "$pat" -print0 2>/dev/null)
    done
  done
  if [[ "$files" -gt 0 ]]; then
    printf '%-14s%8d%12d\n' "$name" "$files" "$lines"
    TOTAL_FILES=$((TOTAL_FILES + files))
    TOTAL_LINES=$((TOTAL_LINES + lines))
  fi
}

TOTAL_FILES=0
TOTAL_LINES=0

count_group "Java" "*.java"
count_group "TypeScript" "*.ts" "*.tsx"
count_group "Vue" "*.vue"
count_group "SQL" "*.sql"
count_group "Markdown" "*.md"
count_group "YAML" "*.yml" "*.yaml"
count_group "Shell" "*.sh"
count_group "Batch" "*.bat" "*.cmd"
count_group "CSS" "*.css"

printf '%s\n' "----------------------------------"
printf '%-14s%8d%12d\n' "TOTAL" "$TOTAL_FILES" "$TOTAL_LINES"
echo
echo "[HINT] Install cloc for more accurate counts: https://github.com/AlDanial/cloc"
