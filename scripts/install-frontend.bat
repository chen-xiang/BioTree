@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 安装前端依赖
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：scripts\install-frontend.bat
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
