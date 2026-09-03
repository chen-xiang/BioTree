@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem =============================================================================
rem BioTree - Catalogue of Life full import (CoL seven kingdoms)
rem Author: chen-xiang
rem Updated: 2026-08-31 Enable full ranks and DwC extension import
rem Updated: 2026-09-01 Resolve JDK 21 (JAVA_HOME / common install paths)
rem Updated: 2026-09-01 Switch to gradle importCol, no web server
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-01 Default import scope is CoL seven kingdoms
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: double-click, or run scripts\import-col-full.bat from the repo root
rem Requires: JDK 21, MySQL (database biotree), network if the archive must be downloaded
rem =============================================================================

set "EXITCODE=0"
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
    set "EXITCODE=1"
    goto :finish
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
  set "EXITCODE=0"
  goto :finish
)

cd /d "%BACKEND%"
echo.
echo [INFO] Starting import via gradle importCol ^(no web port, rank-mode=full, replace=true^)...
echo        Web server can keep running; replace=true will empty taxon tables while importing.
call gradlew.bat importCol --args="--app.import.dwca-path=../data/import/col_latest_dwca.zip --app.import.replace=true --app.import.resume=false --app.import.max-per-rank=0 --app.import.rank-mode=full --app.import.import-vernaculars=true --app.import.import-synonyms=true --app.import.import-descriptions=true --app.import.import-distributions=true --app.import.import-media=true"
set "EXITCODE=%ERRORLEVEL%"

echo.
if "%EXITCODE%"=="0" (
  echo [OK] Import process finished.
) else (
  echo [ERROR] Import failed with exit code %EXITCODE%.
)

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
