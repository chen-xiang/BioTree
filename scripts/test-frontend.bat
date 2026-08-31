@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 前端 typecheck / lint / unit test / build
rem Author: chen-xiang
rem Created: 2026-08-31
rem 用法：scripts\test-frontend.bat
rem =============================================================================

cd /d "%~dp0.."
set "FRONTEND=%CD%\frontend"

echo.
echo [BioTree] test-frontend
echo.

if not exist "%FRONTEND%\package.json" (
  echo [ERROR] frontend\package.json not found.
  exit /b 1
)

where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found.
  exit /b 1
)

cd /d "%FRONTEND%"

if not exist "%FRONTEND%\node_modules" (
  echo [INFO] Installing dependencies...
  call pnpm install
  if errorlevel 1 exit /b 1
)

echo [1/4] typecheck
call pnpm typecheck
if errorlevel 1 exit /b 1

echo [2/4] lint
call pnpm lint
if errorlevel 1 exit /b 1

echo [3/4] test
call pnpm test
if errorlevel 1 exit /b 1

echo [4/4] build
call pnpm build
if errorlevel 1 exit /b 1

echo.
echo [OK] Frontend checks passed.
exit /b 0
