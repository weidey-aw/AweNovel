@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"
title AweNovel - init database

if not exist "config\db.bat" (
  echo [ERROR] config\db.bat not found.
  pause
  exit /b 1
)
call "config\db.bat"

set "MYSQL_EXE="
for /f "delims=" %%i in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%i"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if not defined MYSQL_EXE (
  echo [ERROR] mysql.exe not found. Add MySQL bin directory to PATH.
  pause
  exit /b 1
)
echo [OK] MySQL client: %MYSQL_EXE%

echo.
echo [WARN] This will DROP and recreate database "%DB_NAME%" with default data.
echo        admin / admin123 will be recreated.
choice /c YN /m "Continue"
if errorlevel 2 exit /b 0

echo [..] Importing schema...
"%MYSQL_EXE%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 < "sql\init.sql"
if errorlevel 1 (
  echo [ERROR] Import failed. Check the MySQL account and make sure it can create databases.
  pause
  exit /b 1
)

echo [OK] Database initialized.
timeout /t 3 >nul
exit /b 0
