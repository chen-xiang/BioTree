@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 前后端测试（先后端后前端）
rem Author: chen-xiang
rem Created: 2026-08-31
rem 用法：scripts\test-all.bat
rem =============================================================================

cd /d "%~dp0.."
set "ROOT=%CD%"

echo.
echo [BioTree] test-all
echo.

call "%ROOT%\scripts\test-backend.bat"
if errorlevel 1 (
  echo [ERROR] Backend tests failed.
  exit /b 1
)

call "%ROOT%\scripts\test-frontend.bat"
if errorlevel 1 (
  echo [ERROR] Frontend checks failed.
  exit /b 1
)

echo.
echo [OK] All checks passed.
exit /b 0
