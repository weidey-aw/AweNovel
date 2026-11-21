# ============================================================================
# Build a one-click Windows test package for AweNovel.
#
#   .\scripts\build-test-package.ps1
#
# Output:
#   build\test-package\AweNovel-test\          (runnable folder)
#   build\AweNovel-test-<date>.zip             (zip for distribution)
#
# Steps:
#   1. build web (base /) + admin (base /admin/)
#   2. copy dist into weidey-admin/src/main/resources/static (temporary, gitignored)
#   3. mvn package WITHOUT application-local.yml (avoid shipping secrets inside jar)
#   4. assemble jar + launchers + config + sql + upload dir
# ============================================================================
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$resDir = Join-Path $root 'weidey-admin\src\main\resources'
$staticDir = Join-Path $resDir 'static'
$localYml = Join-Path $resDir 'application-local.yml'
$localBak = Join-Path $env:TEMP 'awenovel-application-local.package-bak'
$buildDir = Join-Path $root 'build\test-package'
$outDir = Join-Path $buildDir 'AweNovel-test'
$pkgSrc = Join-Path $root 'scripts\package'

function Parse-Yaml([string]$path) {
  $map = @{}
  if (-not (Test-Path $path)) { return $map }
  $stack = New-Object System.Collections.ArrayList
  foreach ($line in [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)) {
    if ($line -match '^\s*(#|$)') { continue }
    if ($line -match '^(\s*)([^:#]+):\s*(.*)$') {
      $indent = $Matches[1].Length
      $key = $Matches[2].Trim()
      $val = $Matches[3].Trim().Trim("'").Trim('"')
      while ($stack.Count -gt 0 -and $stack[$stack.Count - 1].Indent -ge $indent) { $stack.RemoveAt($stack.Count - 1) }
      $keys = @($stack | ForEach-Object { $_.Key }) + $key
      $full = ($keys -join '.')
      if ($val -ne '') { $map[$full] = $val }
      [void]$stack.Add([pscustomobject]@{ Indent = $indent; Key = $key })
      continue
    }
  }
  return $map
}

Write-Host '[1/5] Building frontends...' -ForegroundColor Cyan
Push-Location (Join-Path $root 'web-ui')
try {
  & pnpm --filter '@gal/web' build
  if ($LASTEXITCODE -ne 0) { throw 'web build failed' }
  $env:ADMIN_BASE = '/admin/'
  & pnpm --filter '@gal/admin' build
  if ($LASTEXITCODE -ne 0) { throw 'admin build failed' }
} finally {
  Remove-Item Env:\ADMIN_BASE -ErrorAction SilentlyContinue
  Pop-Location
}

Write-Host '[2/5] Preparing static resources...' -ForegroundColor Cyan
if (Test-Path $staticDir) { Remove-Item $staticDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $staticDir | Out-Null
Copy-Item (Join-Path $root 'web-ui\web\dist\*') $staticDir -Recurse -Force
New-Item -ItemType Directory -Force -Path (Join-Path $staticDir 'admin') | Out-Null
Copy-Item (Join-Path $root 'web-ui\admin\dist\*') (Join-Path $staticDir 'admin') -Recurse -Force

Write-Host '[3/5] Packaging jar (local secrets excluded)...' -ForegroundColor Cyan
$movedLocal = $false
if (Test-Path $localYml) {
  Move-Item $localYml $localBak -Force
  $movedLocal = $true
}
# remove stale copies from previous builds so secrets never end up in the jar
Get-ChildItem (Join-Path $root 'weidey-admin\target\classes') -Filter 'application-local*' -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem (Join-Path $root 'weidey-admin\target\classes') -Filter '*.package-bak' -ErrorAction SilentlyContinue | Remove-Item -Force
# remove stale static classes so Maven incremental copy cannot keep old/truncated files
$targetStatic = Join-Path $root 'weidey-admin\target\classes\static'
if (Test-Path $targetStatic) { Remove-Item $targetStatic -Recurse -Force }
try {
  & 'D:\Code\Dependent\Maven\apache-maven-3.9.11\bin\mvn.cmd' -q package -DskipTests
  if ($LASTEXITCODE -ne 0) { throw 'maven package failed' }
} finally {
  if ($movedLocal -and (Test-Path $localBak)) { Move-Item $localBak $localYml -Force }
  if (Test-Path $staticDir) { Remove-Item $staticDir -Recurse -Force }
}

Write-Host '[4/5] Assembling package...' -ForegroundColor Cyan
if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $outDir 'config') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $outDir 'sql') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $outDir 'upload') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $outDir 'logs') | Out-Null

Copy-Item (Join-Path $root 'weidey-admin\target\weidey-admin.jar') (Join-Path $outDir 'AweNovel.jar') -Force
Copy-Item (Join-Path $pkgSrc 'start.bat') $outDir -Force
Copy-Item (Join-Path $pkgSrc 'stop.bat') $outDir -Force
Copy-Item (Join-Path $pkgSrc 'init-db.bat') $outDir -Force
Copy-Item (Join-Path $root 'sql\awe_novel.sql') (Join-Path $outDir 'sql\init.sql') -Force

# README as UTF-8 with BOM so Notepad shows Chinese correctly
$readme = [System.IO.File]::ReadAllText((Join-Path $pkgSrc 'README-package.txt'), [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $outDir 'README.txt'), $readme, (New-Object System.Text.UTF8Encoding($true)))

# ---- read local credentials (fallback to placeholders) ----
$local = Parse-Yaml $localYml
$dbUser = $local['spring.datasource.druid.master.username']; if (-not $dbUser) { $dbUser = 'root' }
$dbPass = $local['spring.datasource.druid.master.password']; if (-not $dbPass) { $dbPass = 'your-db-password' }
$mailUser = $local['spring.mail.username']; if (-not $mailUser) { $mailUser = '' }
$mailPass = $local['spring.mail.password']; if (-not $mailPass) { $mailPass = '' }
$aiKey = $local['ai.deepseek.api-key']; if (-not $aiKey) { $aiKey = '' }

$yml = @"
# AweNovel test package configuration (generated at package time)
server:
  port: 8088
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
awenovel:
  name: AweNovel
  version: test
  copyrightYear: 2024
  profile: ./upload
  addressEnabled: true
  captchaType: math
spring:
  datasource:
    druid:
      master:
        driverClassName: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/awe_novel?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
        username: $dbUser
        password: $dbPass
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      password:
      timeout: 10s
      lettuce:
        pool:
          min-idle: 0
          max-idle: 8
          max-active: 8
          max-wait: -1ms
  mail:
    host: smtp.qq.com
    port: 465
    username: $mailUser
    password: $mailPass
ai:
  deepseek:
    api-key: $aiKey
    base-url: https://api.deepseek.com
    model: deepseek-chat
logging:
  level:
    com.weidey: info
"@
[System.IO.File]::WriteAllText((Join-Path $outDir 'config\application-test.yml'), $yml, (New-Object System.Text.UTF8Encoding($false)))

$dbBat = @"
@echo off
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_NAME=awe_novel"
set "DB_USER=$dbUser"
set "DB_PASS=$dbPass"
"@
[System.IO.File]::WriteAllText((Join-Path $outDir 'config\db.bat'), $dbBat, (New-Object System.Text.UTF8Encoding($false)))

Write-Host '[5/5] Creating zip...' -ForegroundColor Cyan
$stamp = Get-Date -Format 'yyyyMMdd-HHmm'
$zip = Join-Path $root "build\AweNovel-test-$stamp.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $outDir '*') -DestinationPath $zip -CompressionLevel Optimal

$size = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host ''
Write-Host "Done. Folder : $outDir"
Write-Host "      Zip    : $zip ($size MB)"
Write-Host 'Run: start.bat  (init-db.bat on first use)' -ForegroundColor Green
