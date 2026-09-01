@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 后端单元/集成测试
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 启动前解析 JDK 21
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：scripts\test-backend.bat
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "BACKEND=%CD%\backend"

echo.
echo [BioTree] test-backend
echo.

if not exist "%BACKEND%\gradlew.bat" (
  echo [ERROR] backend\gradlew.bat not found.
  set "EXITCODE=1"
  goto :finish
)

set "BIOTREE_NESTED=1"
call "%~dp0ensure-java.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

cd /d "%BACKEND%"
call gradlew.bat test --no-daemon
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
