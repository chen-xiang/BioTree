@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem =============================================================================
rem BioTree - Catalogue of Life 全量导入（动物界 + 植物界）
rem Author: chen-xiang
rem Updated: 2026-08-31 显式开启完整阶元与 DwC 扩展导入
rem Updated: 2026-09-01 启动前解析 JDK 21（JAVA_HOME / 常见安装路径）
rem 用法：在资源管理器中双击，或在仓库根目录执行 scripts\import-col-full.bat
rem 前置：JDK 21、MySQL（库 biotree）、网络（若需下载数据包）
rem =============================================================================

cd /d "%~dp0.."
set "ROOT=%CD%"
set "DWCA=%ROOT%\data\import\col_latest_dwca.zip"
set "DWCA_URL=https://download.checklistbank.org/col/latest_dwca.zip"
set "BACKEND=%ROOT%\backend"

echo.
echo [BioTree] Catalogue of Life full import
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

if not exist "%ROOT%\data\import" (
  mkdir "%ROOT%\data\import" 2>nul
)

if not exist "%DWCA%" (
  echo [INFO] DwC-A package missing, downloading...
  echo        %DWCA_URL%
  where curl >nul 2>nul
  if !ERRORLEVEL! EQU 0 (
    curl -L --retry 3 --retry-delay 5 -o "%DWCA%" "%DWCA_URL%"
  ) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "Invoke-WebRequest -Uri '%DWCA_URL%' -OutFile '%DWCA%'"
  )
  if not exist "%DWCA%" (
    echo [ERROR] Download failed. Please save the zip to:
    echo         %DWCA%
    exit /b 1
  )
  echo [INFO] Download finished.
) else (
  echo [INFO] Found existing package, skip download.
)

echo.
echo [WARN] This will REPLACE existing taxon / i18n / media / distribution data.
echo        Imports full ranks + vernaculars/synonyms/descriptions/distributions/media.
echo        Full import may take a long time and use significant disk/CPU.
echo.
set /p CONFIRM=Type YES to continue: 
if /I not "%CONFIRM%"=="YES" (
  echo [INFO] Cancelled.
  exit /b 0
)

cd /d "%BACKEND%"
echo.
echo [INFO] Starting import ^(rank-mode=full, max-per-rank=0, replace=true^)...
call gradlew.bat bootRun --args="--app.import.enabled=true --app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=true --app.import.resume=false --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
set "EXITCODE=%ERRORLEVEL%"

echo.
if "%EXITCODE%"=="0" (
  echo [OK] Import process finished.
) else (
  echo [ERROR] Import failed with exit code %EXITCODE%.
)

exit /b %EXITCODE%
