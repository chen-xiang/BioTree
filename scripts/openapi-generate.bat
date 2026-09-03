@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - Generate frontend schema.d.ts from offline openapi.yaml
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: scripts\openapi-generate.bat
rem Optional: scripts\openapi-generate.bat live  (backend must be running)
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "FRONTEND=%CD%\frontend"
set "MODE=%~1"

echo.
echo [BioTree] openapi-generate
echo Mode: %MODE%
echo.

if not exist "%FRONTEND%\package.json" (
  echo [ERROR] frontend\package.json not found.
  set "EXITCODE=1"
  goto :finish
)

where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found.
  set "EXITCODE=1"
  goto :finish
)

cd /d "%FRONTEND%"

if /I "%MODE%"=="live" (
  echo [INFO] Generating from http://localhost:8080/v3/api-docs
  call pnpm openapi:generate:live
) else (
  echo [INFO] Generating from openapi\openapi.yaml
  call pnpm openapi:generate
)
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
