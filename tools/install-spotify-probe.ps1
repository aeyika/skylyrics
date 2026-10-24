param([ValidateSet('Install','Restore')][string]$Mode='Install',[string]$BridgeFile='')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$probeRoot=Join-Path $root 'test-results/live-spotify'
New-Item -ItemType Directory -Force (Join-Path $probeRoot 'backups')|Out-Null
foreach($install in @(@{Name='local';Path="$env:LOCALAPPDATA/spicetify/Extensions/beautiful-lyrics-skylyrics.mjs"},@{Name='roaming';Path="$env:APPDATA/spicetify/Extensions/beautiful-lyrics-skylyrics.mjs"})){
    if(!(Test-Path -LiteralPath $install.Path)){continue}
    $saved=Join-Path $probeRoot "backups/$($install.Name)-bridge.mjs"
    if($Mode -eq 'Restore'){if(Test-Path -LiteralPath $saved){Copy-Item -LiteralPath $saved -Destination $install.Path -Force};continue}
    if(!(Test-Path -LiteralPath $saved)){Copy-Item -LiteralPath $install.Path -Destination $saved}
    $key=[IO.File]::ReadAllText((Join-Path $probeRoot 'control-key'))
    $prelude=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'spotify-diagnostics.js')).Replace('__DIAGNOSTIC_KEY__',$key)
    $source=if($BridgeFile){(Resolve-Path -LiteralPath $BridgeFile).Path}else{$saved}
    [IO.File]::WriteAllText($install.Path,$prelude+"`n"+[IO.File]::ReadAllText($source))
}
& spicetify -n apply
if($LASTEXITCODE -ne 0){throw 'Spicetify apply failed'}
& spicetify restart
