@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - Install frontend dependencies
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: scripts\install-frontend.bat
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "FRONTEND=%CD%\frontend"

echo.
echo [BioTree] install-frontend
echo.

if not exist "%FRONTEND%\package.json" (
  echo [ERROR] frontend\package.json not found.
  set "EXITCODE=1"
  goto :finish
)

where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found. Install: npm i -g pnpm
  set "EXITCODE=1"
  goto :finish
)

cd /d "%FRONTEND%"
call pnpm install
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
