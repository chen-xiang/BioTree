#!/usr/bin/env bash
# =============================================================================
# BioTree - 常用脚本索引（仅打印帮助）
# Author: chen-xiang
# Created: 2026-08-31
# Updated: 2026-09-01 说明导入与 Web 启动分离
# Updated: 2026-09-01 启动脚本加 start- 前缀
# Updated: 2026-09-01 说明 Windows bat 结束后 pause
# 用法：./scripts/help.sh
# =============================================================================

cat <<'EOF'

BioTree scripts
===============

Linux / macOS:
  ./scripts/start-web-dev.sh        Start Vite frontend (http://localhost:5173)
  ./scripts/start-server-dev.sh     Start Spring Boot (http://localhost:8080)
  ./scripts/install-frontend.sh     pnpm install
  ./scripts/test-backend.sh         ./gradlew test
  ./scripts/test-frontend.sh        typecheck + lint + test + build
  ./scripts/test-all.sh             Backend then frontend checks
  ./scripts/openapi-generate.sh     Generate schema.d.ts from yaml
  ./scripts/openapi-generate.sh live  Generate from running backend
  ./scripts/count-loc.sh            Source line counts
  ./scripts/import-col-full.sh      Full CoL import (replace, no web port)
  ./scripts/import-col-resume.sh    Resume CoL import (no web port)
  ./scripts/help.sh                 This help

Windows:
  scripts\start-web-dev.bat
  scripts\start-server-dev.bat
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
  - Run start-web-dev and start-server-dev in two separate terminals.
  - Import scripts use gradle importCol and do not bind port 8080.
  - Dev admin (non-prod): admin / admin123
  - Windows .bat windows stay open (pause) after they finish.

EOF
