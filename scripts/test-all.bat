@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - Run backend then frontend tests
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: scripts\test-all.bat
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"

echo.
echo [BioTree] test-all
echo.

set "BIOTREE_NESTED=1"
call "%ROOT%\scripts\test-backend.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  echo [ERROR] Backend tests failed.
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

set "BIOTREE_NESTED=1"
call "%ROOT%\scripts\test-frontend.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  echo [ERROR] Frontend checks failed.
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

echo.
echo [OK] All checks passed.
set "EXITCODE=0"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
