@echo off
rem =============================================================================
rem BioTree - Pause when a top-level bat finishes so a double-clicked window stays open
rem Author: chen-xiang
rem Created: 2026-09-01
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: call "%~dp0finish.bat" %EXITCODE%
rem Note: skip pause when the caller sets BIOTREE_NESTED=1 (nested call)
rem =============================================================================

set "CODE=%~1"
if "%CODE%"=="" set "CODE=0"
if defined BIOTREE_NESTED exit /b %CODE%
echo.
pause
exit /b %CODE%
