@echo off
setlocal EnableExtensions

rem =============================================================================
rem BioTree - 启动后端开发服务（Spring Boot）
rem Author: chen-xiang
rem Created: 2026-08-31
rem 用法：双击或在仓库根目录执行 scripts\server-dev.bat
rem 前置：JDK 21、MySQL（库 biotree，见 application.yml）
rem =============================================================================

cd /d "%~dp0.."
set "ROOT=%CD%"
set "BACKEND=%ROOT%\backend"

echo.
echo [BioTree] server-dev
echo Root   : %ROOT%
echo Backend: %BACKEND%
echo.

if not exist "%BACKEND%\gradlew.bat" (
  echo [ERROR] backend\gradlew.bat not found.
  exit /b 1
)

cd /d "%BACKEND%"
echo [INFO] Starting Spring Boot at http://localhost:8080
echo        OpenAPI: http://localhost:8080/v3/api-docs
echo        Health : http://localhost:8080/api/health
echo        Dev admin: admin / admin123  ^(non-prod^)
echo.
call gradlew.bat bootRun
exit /b %ERRORLEVEL%
