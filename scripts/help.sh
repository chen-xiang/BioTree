#!/usr/bin/env bash
# =============================================================================
# BioTree - 常用脚本索引（仅打印帮助）
# Author: chen-xiang
# Created: 2026-08-31
# 用法：./scripts/help.sh
# =============================================================================

cat <<'EOF'

BioTree scripts
===============

Linux / macOS:
  ./scripts/web-dev.sh              Start Vite frontend (http://localhost:5173)
  ./scripts/server-dev.sh           Start Spring Boot (http://localhost:8080)
  ./scripts/install-frontend.sh     pnpm install
  ./scripts/test-backend.sh         ./gradlew test
  ./scripts/test-frontend.sh        typecheck + lint + test + build
  ./scripts/test-all.sh             Backend then frontend checks
  ./scripts/openapi-generate.sh     Generate schema.d.ts from yaml
  ./scripts/openapi-generate.sh live  Generate from running backend
  ./scripts/count-loc.sh            Source line counts
  ./scripts/import-col-full.sh      Full CoL import (replace)
  ./scripts/import-col-resume.sh    Resume CoL import
  ./scripts/help.sh                 This help

Windows:
  scripts\web-dev.bat
  scripts\server-dev.bat
  scripts\install-frontend.bat
  scripts\test-backend.bat
  scripts\test-frontend.bat
  scripts\test-all.bat
  scripts\openapi-generate.bat [live]
  scripts\count-loc.bat
  scripts\import-col-full.bat
  scripts\import-col-resume.bat
  scripts\help.bat

Tips:
  - Run web-dev and server-dev in two separate terminals.
  - Dev admin (non-prod): admin / admin123

EOF
