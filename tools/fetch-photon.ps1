$ErrorActionPreference = 'Stop'
$photonLock = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'photon.lock.json') | ConvertFrom-Json
$packDirectory = Join-Path $PSScriptRoot 'shaderpacks'
$packPath = Join-Path $packDirectory $photonLock.filename
New-Item -ItemType Directory -Force -Path $packDirectory | Out-Null
if (-not (Test-Path -LiteralPath $packPath)) {
    Invoke-WebRequest -Uri $photonLock.url -OutFile $packPath -UseBasicParsing
}
$actualHash = (Get-FileHash -LiteralPath $packPath -Algorithm SHA512).Hash.ToLowerInvariant()
if ($actualHash -ne $photonLock.sha512) {
    throw "Photon checksum mismatch: $packPath. Expected $($photonLock.sha512), got $actualHash."
}
Write-Output "Verified Photon $($photonLock.version): $packPath"
