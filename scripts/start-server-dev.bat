@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 启动后端开发服务（Spring Boot）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 启动前解析 JDK 21
rem Updated: 2026-09-01 提示导入走独立脚本
rem Updated: 2026-09-01 脚本改名为 start-server-dev
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：双击或在仓库根目录执行 scripts\start-server-dev.bat
rem 前置：JDK 21、MySQL（库 biotree，见 application.yml）
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"
set "BACKEND=%ROOT%\backend"

echo.
echo [BioTree] start-server-dev
echo Root   : %ROOT%
echo Backend: %BACKEND%
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
set "BIOTREE_NESTED=1"
call "%~dp0ensure-mysql.bat"
set "CALL_ERR=%ERRORLEVEL%"
set "BIOTREE_NESTED="
if not "%CALL_ERR%"=="0" (
  set "EXITCODE=%CALL_ERR%"
  goto :finish
)

cd /d "%BACKEND%"
echo [INFO] Starting Spring Boot at http://localhost:8080
echo        OpenAPI: http://localhost:8080/v3/api-docs
echo        Health : http://localhost:8080/api/health
echo        Dev admin: admin / admin123  ^(non-prod^)
echo        Import is a separate process: scripts\import-col-full.bat / import-col-resume.bat
echo.
call gradlew.bat bootRun
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
