param([ValidateSet('all','1.21.11','26.2')][string]$Target='all')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
foreach($name in @('1.21.11','26.2')){
    if($Target -ne 'all' -and $Target -ne $name){continue}
    & (Join-Path $PSScriptRoot 'ffmpeg/package/ffmpeg.exe') -y -framerate 10 -i (Join-Path $root "test-results/ingame-$name/footage-%04d.png") -vf 'pad=ceil(iw/2)*2:ceil(ih/2)*2' -c:v libx264 -pix_fmt yuv420p -movflags +faststart (Join-Path $root "dist/skylyrics-demo-mc$name.mp4")
    if($LASTEXITCODE -ne 0){throw "Could not encode $name footage"}
}
