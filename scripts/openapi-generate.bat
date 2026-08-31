@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 从离线 openapi.yaml 生成前端类型 schema.d.ts
rem Author: chen-xiang
rem Created: 2026-08-31
rem 用法：scripts\openapi-generate.bat
rem 可选：scripts\openapi-generate.bat live  （需后端已启动）
rem =============================================================================

cd /d "%~dp0.."
set "FRONTEND=%CD%\frontend"
set "MODE=%~1"

echo.
echo [BioTree] openapi-generate
echo Mode: %MODE%
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

if /I "%MODE%"=="live" (
  echo [INFO] Generating from http://localhost:8080/v3/api-docs
  call pnpm openapi:generate:live
) else (
  echo [INFO] Generating from openapi\openapi.yaml
  call pnpm openapi:generate
)

exit /b %ERRORLEVEL%
