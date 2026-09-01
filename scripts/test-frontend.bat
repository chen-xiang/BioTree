@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 前端 typecheck / lint / unit test / build
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：scripts\test-frontend.bat
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "FRONTEND=%CD%\frontend"

echo.
echo [BioTree] test-frontend
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

if not exist "%FRONTEND%\node_modules" (
  echo [INFO] Installing dependencies...
  call pnpm install
  if errorlevel 1 (
    set "EXITCODE=1"
    goto :finish
  )
)

echo [1/4] typecheck
call pnpm typecheck
if errorlevel 1 (
  set "EXITCODE=1"
  goto :finish
)

echo [2/4] lint
call pnpm lint
if errorlevel 1 (
  set "EXITCODE=1"
  goto :finish
)

echo [3/4] test
call pnpm test
if errorlevel 1 (
  set "EXITCODE=1"
  goto :finish
)

echo [4/4] build
call pnpm build
if errorlevel 1 (
  set "EXITCODE=1"
  goto :finish
)

echo.
echo [OK] Frontend checks passed.
set "EXITCODE=0"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
