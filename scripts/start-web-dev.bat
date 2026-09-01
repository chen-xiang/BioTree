@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 启动前端开发服务器（Vite）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 脚本改名为 start-web-dev
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：双击或在仓库根目录执行 scripts\start-web-dev.bat
rem 前置：Node.js 20+、pnpm
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"
set "FRONTEND=%ROOT%\frontend"

echo.
echo [BioTree] start-web-dev
echo Root    : %ROOT%
echo Frontend: %FRONTEND%
echo.

if not exist "%FRONTEND%\package.json" (
  echo [ERROR] frontend\package.json not found.
  set "EXITCODE=1"
  goto :finish
)

where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found. Install Node.js and: npm i -g pnpm
  set "EXITCODE=1"
  goto :finish
)

cd /d "%FRONTEND%"

if not exist "%FRONTEND%\node_modules" (
  echo [INFO] node_modules missing, running pnpm install...
  call pnpm install
  if errorlevel 1 (
    echo [ERROR] pnpm install failed.
    set "EXITCODE=1"
    goto :finish
  )
)

echo [INFO] Starting Vite at http://localhost:5173
echo        Proxy: /api and /files -^> http://localhost:8080
echo.
call pnpm dev
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
