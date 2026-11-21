@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"
title AweNovel Test Server

set "APP_PORT=8088"
set "APP_JAR=AweNovel.jar"

echo ============================================================
echo  AweNovel Test Package
echo  Web  : http://localhost:%APP_PORT%/
echo  Admin: http://localhost:%APP_PORT%/admin/
echo ============================================================
echo.

rem ---------- 1. locate java ----------
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE if exist "jdk\bin\java.exe" set "JAVA_EXE=jdk\bin\java.exe"
if not defined JAVA_EXE for /f "delims=" %%i in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%i"
if not defined JAVA_EXE (
  echo [ERROR] Java not found. Please install JDK 17+ and set JAVA_HOME.
  echo         https://adoptium.net/
  echo.
  pause
  exit /b 1
)
echo [OK] Java: %JAVA_EXE%

rem ---------- 2. check mysql / redis ----------
powershell -NoProfile -ExecutionPolicy Bypass -Command "$bad=@(); foreach($p in 3306,6379){ if(-not (Test-NetConnection 127.0.0.1 -Port $p -WarningAction SilentlyContinue).TcpTestSucceeded){ $bad += $p } }; if($bad.Count -gt 0){ Write-Host ('[WARN] Port not listening: ' + ($bad -join ', ')); exit 1 } else { exit 0 }"
if errorlevel 1 (
  echo [WARN] MySQL 3306 or Redis 6379 is not running.
  echo        Start them first, or run "init-db.bat" after MySQL is ready.
  echo.
)

rem ---------- 3. start application ----------
echo [..] Starting AweNovel, first start may take 20-60 seconds...
start "AweNovel Server" /min "%JAVA_EXE%" -Dfile.encoding=UTF-8 -jar "%APP_JAR%" --spring.profiles.active=dev,test

powershell -NoProfile -ExecutionPolicy Bypass -Command "for($i=0;$i -lt 120;$i++){ if((Test-NetConnection 127.0.0.1 -Port %APP_PORT% -WarningAction SilentlyContinue).TcpTestSucceeded){ exit 0 }; Start-Sleep -Seconds 2 }; exit 1"
if errorlevel 1 (
  echo [ERROR] AweNovel failed to start in 240 seconds.
  echo         Check the "AweNovel Server" window for details.
  echo.
  pause
  exit /b 1
)

echo [OK] AweNovel is running.
start "" "http://localhost:%APP_PORT%/"
start "" "http://localhost:%APP_PORT%/admin/"

echo.
echo Default account: admin / admin123
echo Close this window does NOT stop the server, run stop.bat to stop it.
echo.
timeout /t 5 >nul
exit /b 0
