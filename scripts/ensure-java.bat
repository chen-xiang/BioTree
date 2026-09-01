@echo off
rem =============================================================================
rem BioTree - 解析 JDK 21 并写入当前会话 JAVA_HOME / PATH
rem Author: chen-xiang
rem Created: 2026-09-01
rem Updated: 2026-09-01 单独双击时结束后 pause
rem 用法：由其它 scripts\*.bat 通过 call 调用（勿单独 setlocal，以便变量回传）
rem =============================================================================

set "EXITCODE=0"

if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" goto :ok
)

where java >nul 2>nul
if %ERRORLEVEL% EQU 0 goto :ok

for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
  if exist "%%D\bin\java.exe" (
    set "JAVA_HOME=%%D"
    goto :export
  )
)
for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do (
  if exist "%%D\bin\java.exe" (
    set "JAVA_HOME=%%D"
    goto :export
  )
)
for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do (
  if exist "%%D\bin\java.exe" (
    set "JAVA_HOME=%%D"
    goto :export
  )
)
for /d %%D in ("%ProgramFiles%\Amazon Corretto\jdk21*") do (
  if exist "%%D\bin\java.exe" (
    set "JAVA_HOME=%%D"
    goto :export
  )
)
for /d %%D in ("%USERPROFILE%\.jdks\temurin-21*") do (
  if exist "%%D\bin\java.exe" (
    set "JAVA_HOME=%%D"
    goto :export
  )
)

echo [ERROR] JDK 21 not found. Install Eclipse Temurin 21 and set JAVA_HOME.
echo         Example: winget install --id EclipseAdoptium.Temurin.21.JDK
set "EXITCODE=1"
goto :finish

:export
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [INFO] Using JAVA_HOME=%JAVA_HOME%

:ok
set "EXITCODE=0"

:finish
call "%~dp0finish.bat" %EXITCODE%
exit /b %EXITCODE%
