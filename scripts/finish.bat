@echo off
rem =============================================================================
rem BioTree - 顶层 bat 结束时 pause，避免双击窗口立刻关闭
rem Author: chen-xiang
rem Created: 2026-09-01
rem 用法：call "%~dp0finish.bat" %EXITCODE%
rem 说明：若调用方设置了 BIOTREE_NESTED=1，则跳过 pause（供 call 子脚本）
rem =============================================================================

set "CODE=%~1"
if "%CODE%"=="" set "CODE=0"
if defined BIOTREE_NESTED exit /b %CODE%
echo.
pause
exit /b %CODE%
