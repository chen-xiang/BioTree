@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 后端单元/集成测试
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 启动前解析 JDK 21
rem 用法：scripts\test-backend.bat
rem =============================================================================

cd /d "%~dp0.."
set "BACKEND=%CD%\backend"

echo.
echo [BioTree] test-backend
echo.

if not exist "%BACKEND%\gradlew.bat" (
  echo [ERROR] backend\gradlew.bat not found.
  exit /b 1
)

call "%~dp0ensure-java.bat"
if errorlevel 1 exit /b 1

cd /d "%BACKEND%"
call gradlew.bat test --no-daemon
exit /b %ERRORLEVEL%
