@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 前后端测试（先后端后前端）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：scripts\test-all.bat
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"

echo.
echo [BioTree] test-all
echo.

set "BIOTREE_NESTED=1"
call "%ROOT%\scripts\test-backend.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  echo [ERROR] Backend tests failed.
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

set "BIOTREE_NESTED=1"
call "%ROOT%\scripts\test-frontend.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  echo [ERROR] Frontend checks failed.
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

echo.
echo [OK] All checks passed.
set "EXITCODE=0"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
