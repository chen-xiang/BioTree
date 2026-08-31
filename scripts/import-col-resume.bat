@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem =============================================================================
rem BioTree - Catalogue of Life 断点续跑（不 replace、不提示清空）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-08-31 显式开启完整阶元与 DwC 扩展导入
rem Updated: 2026-09-01 启动前解析 JDK 21
rem 用法：scripts\import-col-resume.bat
rem 前置：已有 checkpoint；DwC-A 包存在；勿与 replace=true 混用
rem =============================================================================

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
  exit /b 1
)

call "%~dp0ensure-java.bat"
if errorlevel 1 exit /b 1
call "%~dp0ensure-mysql.bat"
if errorlevel 1 exit /b 1

if not exist "%DWCA%" (
  echo [ERROR] DwC-A package not found:
  echo         %DWCA%
  echo         Run scripts\import-col-full.bat first, or place the zip manually.
  exit /b 1
)

cd /d "%BACKEND%"
echo [INFO] Starting resume import ^(replace=false, resume=true, rank-mode=full^)...
call gradlew.bat bootRun --args="--app.import.enabled=true --app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=false --app.import.resume=true --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
exit /b %ERRORLEVEL%
