@echo off
setlocal EnableExtensions
chcp 65001 >nul
title AweNovel - stop

set "APP_PORT=8088"

powershell -NoProfile -ExecutionPolicy Bypass -Command "$c = Get-NetTCPConnection -LocalPort %APP_PORT% -State Listen -ErrorAction SilentlyContinue; if ($c) { $c | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; Write-Host '[OK] AweNovel stopped.' } else { Write-Host '[INFO] AweNovel is not running.' }"

timeout /t 2 >nul
exit /b 0
