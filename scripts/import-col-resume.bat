@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem =============================================================================
rem BioTree - Catalogue of Life resume import (no replace, no wipe prompt)
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-08-31 Enable full ranks and DwC extension import
rem Updated: 2026-09-01 Resolve JDK 21 before start
rem Updated: 2026-09-01 Switch to gradle importCol, no web server
rem Updated: 2026-09-01 Pause at the end so a double-clicked window stays open
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: scripts\import-col-resume.bat
rem Requires: an existing checkpoint; DwC-A zip present; do not mix with replace=true
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
