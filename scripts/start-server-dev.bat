@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - Start the backend dev server (Spring Boot)
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 Resolve JDK 21 before start
rem Updated: 2026-09-01 Point import to standalone scripts
rem Updated: 2026-09-01 Rename script to start-server-dev
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: double-click, or run scripts\start-server-dev.bat from the repo root
rem Requires: JDK 21, MySQL (database biotree, see application.yml)
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"
set "BACKEND=%ROOT%\backend"

echo.
echo [BioTree] start-server-dev
echo Root   : %ROOT%
echo Backend: %BACKEND%
echo.

if not exist "%BACKEND%\gradlew.bat" (
  echo [ERROR] backend\gradlew.bat not found.
  set "EXITCODE=1"
  goto :finish
)

set "BIOTREE_NESTED=1"
call "%~dp0ensure-java.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)
set "BIOTREE_NESTED=1"
call "%~dp0ensure-mysql.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

cd /d "%BACKEND%"
echo [INFO] Starting Spring Boot at http://localhost:8080
echo        OpenAPI: http://localhost:8080/v3/api-docs
echo        Health : http://localhost:8080/api/health
echo        Dev admin: admin / admin123  ^(non-prod^)
echo        Import is a separate process: scripts\import-col-full.bat / import-col-resume.bat
echo.
call gradlew.bat bootRun
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
