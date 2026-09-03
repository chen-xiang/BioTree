#!/usr/bin/env bash
# =============================================================================
# BioTree - Run backend then frontend tests
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-03 English comments only, avoid cmd mojibake
# Usage: ./scripts/test-all.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo
echo "[BioTree] test-all"
echo

"$ROOT/scripts/test-backend.sh"
"$ROOT/scripts/test-frontend.sh"

echo
echo "[OK] All checks passed."
