@echo off
rem =============================================================================
rem BioTree - 检查本机 MySQL 是否在 127.0.0.1:3306 监听
rem Author: chen-xiang
rem Created: 2026-09-01
rem Updated: 2026-09-01 单独双击时结束后 pause
rem 用法：由其它 scripts\*.bat 通过 call 调用
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
