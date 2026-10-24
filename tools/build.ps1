param([ValidateSet('all','core','1.21.11','26.2')][string]$Target = 'all', [switch]$RunClient)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'run-lock.ps1')
$skyRunLock=Enter-SkyLyricsRunLock $root
try {
$liveClients = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {
    $_.CommandLine -like "*$root*" -and $_.CommandLine -like '*net.fabricmc.devlaunchinjector.Main*'
}
if ($liveClients) { throw 'Close the Sky Lyrics development client before rebuilding. Replacing a loaded core jar can crash Minecraft.' }
$gradle = Join-Path $PSScriptRoot 'gradle-9.5.1/bin/gradle.bat'
$env:GRADLE_USER_HOME = Join-Path $root '.gradle-home'
function Build-Target([string]$name, [int]$javaVersion) {
    $jdk = Get-ChildItem -Directory (Join-Path $PSScriptRoot "jdk$javaVersion") | Select-Object -First 1
    if (!$jdk) { throw "Missing tools/jdk$javaVersion. Run tools/bootstrap.ps1." }
    $env:JAVA_HOME = $jdk.FullName
    $tasks = @('build', ':core:test', '--no-daemon', '--console=plain')
    if ($RunClient) { $tasks = @('runClient', '--no-daemon', '--console=plain') }
    $projectDir = if ($name -eq 'core') { $root } else { Join-Path $root "fabric-$name" }
    & $gradle -p $projectDir @tasks
    if ($LASTEXITCODE -ne 0) { throw "$name build failed ($LASTEXITCODE)" }
    if (!$RunClient -and $name -ne 'core') {
        New-Item -ItemType Directory -Force (Join-Path $root 'dist') | Out-Null
        Copy-Item (Join-Path $projectDir "build/libs/skylyrics-0.1.0+mc$name.jar") (Join-Path $root 'dist')
    }
}
if ($Target -in @('all','core','1.21.11')) { Build-Target $(if ($Target -eq 'core') {'core'} else {'1.21.11'}) 21 }
if ($Target -in @('all','26.2')) { Build-Target '26.2' 25 }
} finally { $skyRunLock.ReleaseMutex();$skyRunLock.Dispose() }
