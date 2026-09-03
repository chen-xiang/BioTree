@echo off
rem =============================================================================
rem BioTree - Check that MySQL is listening on 127.0.0.1:3306
rem Author: chen-xiang
rem Created: 2026-09-01
rem Updated: 2026-09-01 Pause when double-clicked on its own
rem Updated: 2026-09-03 English comments only, avoid cmd mojibake
rem Usage: called from other scripts\*.bat via call
rem =============================================================================

set "EXITCODE=0"

powershell -NoProfile -Command ^
  "try { $c = New-Object System.Net.Sockets.TcpClient; $iar = $c.BeginConnect('127.0.0.1', 3306, $null, $null); if (-not $iar.AsyncWaitHandle.WaitOne(2000, $false)) { $c.Close(); exit 1 }; $c.EndConnect($iar); $c.Close(); exit 0 } catch { exit 1 }"
if errorlevel 1 (
  echo [ERROR] MySQL is not listening on 127.0.0.1:3306.
  echo         Start the MySQL84 Windows service, then create database/user biotree/biotree.
  echo         Example: net start MySQL84
  set "EXITCODE=1"
  goto :finish
)
set "EXITCODE=0"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
