@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem =============================================================================
rem BioTree - Catalogue of Life 断点续跑（不 replace、不提示清空）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-08-31 显式开启完整阶元与 DwC 扩展导入
rem Updated: 2026-09-01 启动前解析 JDK 21
rem Updated: 2026-09-01 改为 gradle importCol，不启动 Web
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：scripts\import-col-resume.bat
rem 前置：已有 checkpoint；DwC-A 包存在；勿与 replace=true 混用
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"
set "DWCA=%ROOT%\data\import\col_latest_dwca.zip"
set "BACKEND=%ROOT%\backend"

echo.
echo [BioTree] CoL import resume
echo Root   : %ROOT%
echo Package: %DWCA%
echo.

if not exist "%BACKEND%\gradlew.bat" (
  echo [ERROR] gradlew.bat not found under backend\
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

if not exist "%DWCA%" (
  echo [ERROR] DwC-A package not found:
  echo         %DWCA%
  echo         Run scripts\import-col-full.bat first, or place the zip manually.
  set "EXITCODE=1"
  goto :finish
)

cd /d "%BACKEND%"
echo [INFO] Starting resume import via gradle importCol ^(no web port, replace=false^)...
call gradlew.bat importCol --args="--app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=false --app.import.resume=true --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
