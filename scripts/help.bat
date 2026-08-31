@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 常用脚本索引（仅打印帮助）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-08-31 同步列出 Linux 脚本
rem 用法：scripts\help.bat
rem =============================================================================

echo.
echo BioTree scripts
echo ===============
echo.
echo Windows:
echo   scripts\web-dev.bat              Start Vite frontend ^(http://localhost:5173^)
echo   scripts\server-dev.bat           Start Spring Boot ^(http://localhost:8080^)
echo   scripts\install-frontend.bat     pnpm install
echo   scripts\test-backend.bat         ./gradlew test
echo   scripts\test-frontend.bat        typecheck + lint + test + build
echo   scripts\test-all.bat             Backend then frontend checks
echo   scripts\openapi-generate.bat     Generate schema.d.ts from yaml
echo   scripts\openapi-generate.bat live  Generate from running backend
echo   scripts\count-loc.bat            Source line counts
echo   scripts\import-col-full.bat      Full CoL import ^(replace^)
echo   scripts\import-col-resume.bat    Resume CoL import
echo   scripts\help.bat                 This help
echo.
echo Linux / macOS:
echo   ./scripts/web-dev.sh
echo   ./scripts/server-dev.sh
echo   ./scripts/install-frontend.sh
echo   ./scripts/test-backend.sh
echo   ./scripts/test-frontend.sh
echo   ./scripts/test-all.sh
echo   ./scripts/openapi-generate.sh [live]
echo   ./scripts/count-loc.sh
echo   ./scripts/import-col-full.sh
echo   ./scripts/import-col-resume.sh
echo   ./scripts/help.sh
echo.
echo Tips:
echo   - Run web-dev and server-dev in two separate terminals.
echo   - Dev admin ^(non-prod^): admin / admin123
echo.

exit /b 0
