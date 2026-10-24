$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = Split-Path $PSScriptRoot -Parent
$lock = Get-Content (Join-Path $PSScriptRoot 'toolchains.lock.json') -Raw | ConvertFrom-Json
function Get-Archive($item, [string]$name, [string]$destination) {
    $zip = Join-Path $PSScriptRoot "$name.zip"
    if (!(Test-Path -LiteralPath $zip)) { Invoke-WebRequest -Uri $item.url -OutFile $zip }
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLower() -ne $item.sha256) { throw "Checksum mismatch for $name" }
    if (!(Test-Path -LiteralPath $destination)) { Expand-Archive -LiteralPath $zip -DestinationPath $destination -Force }
}
Get-Archive $lock.jdk21 'jdk21' (Join-Path $PSScriptRoot 'jdk21')
Get-Archive $lock.jdk25 'jdk25' (Join-Path $PSScriptRoot 'jdk25')
Get-Archive $lock.deno 'deno' (Join-Path $PSScriptRoot 'deno')
$gradleDir = Join-Path $PSScriptRoot "gradle-$($lock.gradle.version)"
if (!(Test-Path -LiteralPath $gradleDir)) {
    $zip = Join-Path $PSScriptRoot "gradle-$($lock.gradle.version)-bin.zip"
    if (!(Test-Path -LiteralPath $zip)) { Invoke-WebRequest -Uri $lock.gradle.url -OutFile $zip }
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLower() -ne $lock.gradle.sha256) { throw 'Gradle checksum mismatch' }
    Expand-Archive -LiteralPath $zip -DestinationPath $PSScriptRoot -Force
}
$upstream = Join-Path $root 'upstream/beautiful-lyrics'
if (!(Test-Path -LiteralPath (Join-Path $upstream '.git'))) {
    & git clone $lock.upstream.url $upstream
    if ($LASTEXITCODE -ne 0) { throw 'Beautiful Lyrics clone failed' }
    & git -C $upstream checkout --detach $lock.upstream.revision
    if ($LASTEXITCODE -ne 0) { throw 'Could not pin Beautiful Lyrics' }
}
if ((& git -C $upstream rev-parse HEAD).Trim() -ne $lock.upstream.revision) { throw 'Unexpected Beautiful Lyrics revision; preserve local changes before repinning' }
$fontLock=Get-Content (Join-Path $PSScriptRoot 'fonts.lock.json') -Raw|ConvertFrom-Json
foreach($font in $fontLock.files){
    $fontPath=Join-Path $root "presentation/src/main/resources/assets/skylyrics/fonts/$($font.name)"
    if(!(Test-Path -LiteralPath $fontPath)){Invoke-WebRequest "https://raw.githubusercontent.com/awdev1/beautitifullyricsfont/$($fontLock.revision)/$($font.name)" -OutFile $fontPath}
    if((Get-FileHash -LiteralPath $fontPath -Algorithm SHA256).Hash.ToLower() -ne $font.sha256){throw "Font checksum mismatch: $($font.name)"}
}
Push-Location $root
try { & (Join-Path $PSScriptRoot 'deno/deno.exe') run --allow-read --allow-write --allow-env tools/prepare-font.ts; if($LASTEXITCODE -ne 0){throw 'Font preparation failed'} } finally {Pop-Location}
Write-Output 'Pinned Java 21/25, Gradle, Deno, and Beautiful Lyrics are ready.'
