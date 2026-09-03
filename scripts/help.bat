@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - Script index (prints help only)
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-08-31 Also list Linux scripts
rem Updated: 2026-09-01 Document that import is separate from the web process
rem Updated: 2026-09-01 Prefix launcher scripts with start-
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: scripts\help.bat
rem =============================================================================

set "EXITCODE=0"

echo.
echo BioTree scripts
echo ===============
echo.
echo Windows:
echo   scripts\start-web-dev.bat        Start Vite frontend ^(http://localhost:5173^)
echo   scripts\start-server-dev.bat     Start Spring Boot ^(http://localhost:8080^)
echo   scripts\install-frontend.bat     pnpm install
echo   scripts\test-backend.bat         ./gradlew test
echo   scripts\test-frontend.bat        typecheck + lint + test + build
echo   scripts\test-all.bat             Backend then frontend checks
echo   scripts\openapi-generate.bat     Generate schema.d.ts from yaml
echo   scripts\openapi-generate.bat live  Generate from running backend
echo   scripts\count-loc.bat            Source line counts
echo   scripts\import-col-full.bat      Full CoL import ^(replace, no web port^)
echo   scripts\import-col-resume.bat    Resume CoL import ^(no web port^)
echo   scripts\help.bat                 This help
echo.
echo Linux / macOS:
echo   ./scripts/start-web-dev.sh
echo   ./scripts/start-server-dev.sh
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
echo   - Run start-web-dev and start-server-dev in two separate terminals.
echo   - Import scripts use gradle importCol and do not bind port 8080.
echo   - Dev admin ^(non-prod^): admin / admin123
echo   - Windows .bat windows stay open ^(pause^) after they finish.
echo.

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
