@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 启动前端开发服务器（Vite）
rem Author: chen-xiang
rem Created: 2026-08-31
rem 用法：双击或在仓库根目录执行 scripts\web-dev.bat
rem 前置：Node.js 20+、pnpm
rem =============================================================================

cd /d "%~dp0.."
set "ROOT=%CD%"
set "FRONTEND=%ROOT%\frontend"

echo.
echo [BioTree] web-dev
echo Root    : %ROOT%
echo Frontend: %FRONTEND%
echo.

if not exist "%FRONTEND%\package.json" (
  echo [ERROR] frontend\package.json not found.
  exit /b 1
)

where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found. Install Node.js and: npm i -g pnpm
  exit /b 1
)

cd /d "%FRONTEND%"

if not exist "%FRONTEND%\node_modules" (
  echo [INFO] node_modules missing, running pnpm install...
  call pnpm install
  if errorlevel 1 (
    echo [ERROR] pnpm install failed.
    exit /b 1
  )
)

echo [INFO] Starting Vite at http://localhost:5173
echo        Proxy: /api and /files -^> http://localhost:8080
echo.
call pnpm dev
exit /b %ERRORLEVEL%
