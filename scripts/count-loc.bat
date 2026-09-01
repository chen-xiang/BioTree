@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem =============================================================================
rem BioTree - 代码行统计（源码，不含依赖与构建产物）
rem Author: chen-xiang
rem Created: 2026-08-31
rem Updated: 2026-09-01 结束后 pause，避免双击窗口立刻关闭
rem 用法：scripts\count-loc.bat
rem 说明：优先使用 cloc；若无则用 PowerShell 按扩展名粗算
rem =============================================================================

set "EXITCODE=0"
cd /d "%~dp0.."
set "ROOT=%CD%"

echo.
echo [BioTree] count-loc
echo Root: %ROOT%
echo.

where cloc >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo [INFO] Using cloc...
  cloc "%ROOT%\backend\src" "%ROOT%\frontend\src" "%ROOT%\docs" "%ROOT%\scripts" --quiet
  set "EXITCODE=%ERRORLEVEL%"
  goto :finish
)

echo [INFO] cloc not found, using PowerShell fallback...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$root='%ROOT%';" ^
  "$dirs=@(" ^
  "  (Join-Path $root 'backend\src')," ^
  "  (Join-Path $root 'frontend\src')," ^
  "  (Join-Path $root 'docs')," ^
  "  (Join-Path $root 'scripts')" ^
  ");" ^
  "$extGroups=@{ " ^
  "  'Java'=@('*.java');" ^
  "  'TypeScript'=@('*.ts','*.tsx');" ^
  "  'Vue'=@('*.vue');" ^
  "  'SQL'=@('*.sql');" ^
  "  'Markdown'=@('*.md');" ^
  "  'YAML'=@('*.yml','*.yaml');" ^
  "  'Batch'=@('*.bat','*.cmd');" ^
  "  'CSS'=@('*.css')" ^
  "};" ^
  "$totalFiles=0; $totalLines=0;" ^
  "'Language'.PadRight(14) + 'Files'.PadLeft(8) + 'Lines'.PadLeft(12);" ^
  "'-' * 34;" ^
  "foreach ($name in ($extGroups.Keys | Sort-Object)) {" ^
  "  $files=0; $lines=0;" ^
  "  foreach ($dir in $dirs) {" ^
  "    if (-not (Test-Path $dir)) { continue }" ^
  "    foreach ($pat in $extGroups[$name]) {" ^
  "      Get-ChildItem -Path $dir -Recurse -File -Filter $pat -ErrorAction SilentlyContinue |" ^
  "        Where-Object { $_.FullName -notmatch '\\node_modules\\|\\dist\\|\\build\\|\\.git\\' } |" ^
  "        ForEach-Object {" ^
  "          $files++;" ^
  "          $n=(Get-Content -LiteralPath $_.FullName -ErrorAction SilentlyContinue | Measure-Object -Line).Lines;" ^
  "          if ($n) { $lines += $n }" ^
  "        }" ^
  "    }" ^
  "  }" ^
  "  if ($files -gt 0) {" ^
  "    $totalFiles += $files; $totalLines += $lines;" ^
  "    ($name.PadRight(14) + ([string]$files).PadLeft(8) + ([string]$lines).PadLeft(12))" ^
  "  }" ^
  "};" ^
  "'-' * 34;" ^
  "('TOTAL'.PadRight(14) + ([string]$totalFiles).PadLeft(8) + ([string]$totalLines).PadLeft(12));" ^
  "Write-Host ''; Write-Host '[HINT] Install cloc for more accurate counts: https://github.com/AlDanial/cloc'"

set "EXITCODE=%ERRORLEVEL%"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
