@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 安装前端依赖
rem Author: chen-xiang
rem Created: 2026-08-31
rem 用法：scripts\install-frontend.bat
rem =============================================================================

cd /d "%~dp0.."
set "FRONTEND=%CD%\frontend"

echo.
echo [BioTree] install-frontend
echo.

if not exist "%FRONTEND%\package.json" (
  echo [ERROR] frontend\package.json not found.
  exit /b 1
)

where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found. Install: npm i -g pnpm
  exit /b 1
)

cd /d "%FRONTEND%"
call pnpm install
exit /b %ERRORLEVEL%
