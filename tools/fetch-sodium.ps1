$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
$items=Get-Content (Join-Path $PSScriptRoot 'compatibility-candidates.json') -Raw|ConvertFrom-Json
foreach($item in $items|Where-Object {$_.mod -eq 'sodium'}){
    $directory=Join-Path $PSScriptRoot "compat-mods/$($item.minecraft)"
    New-Item -ItemType Directory -Force $directory|Out-Null
    $file=Join-Path $directory $item.file.filename
    if(!(Test-Path -LiteralPath $file)){Invoke-WebRequest -Uri $item.file.url -OutFile $file}
    if((Get-FileHash -LiteralPath $file -Algorithm SHA512).Hash.ToLower() -ne $item.file.hashes.sha512){throw "Sodium checksum mismatch: $file"}
    Write-Output "Verified $($item.file.filename)"
}
