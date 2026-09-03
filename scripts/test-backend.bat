@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - Backend unit and integration tests
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 Resolve JDK 21 before start
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: scripts\test-backend.bat
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "BACKEND=%CD%\backend"

echo.
echo [BioTree] test-backend
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

cd /d "%BACKEND%"
call gradlew.bat test --no-daemon
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
