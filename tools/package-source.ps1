$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$stage=Join-Path $root ('dist/source-staging/skylyrics-source-0.1.0-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $stage|Out-Null
foreach($name in @('core','presentation','fabric-1.21.11','fabric-26.2','spicetify-bridge','test-fixtures','docs','tools')){
    $source=Join-Path $root $name
    Get-ChildItem -LiteralPath $source -File -Recurse | ForEach-Object {
        $relative=$_.FullName.Substring($root.Length+1)
        if($name -eq 'tools' -and $_.Extension -notin @('.ps1','.mjs','.ts','.java','.json')){return}
        if($relative -match '\\(build|\.gradle|run|jdk21|jdk25|gradle-9\.5\.1|deno|ffmpeg|compat-mods|api|gametest-api)\\' -or $relative -match '\.(zip|tgz|log)$' -or $relative -match '^tools\\(pipelines-|.*metadata|gametest-threads)'){return}
        $target=Join-Path $stage $relative
        New-Item -ItemType Directory -Force (Split-Path $target)|Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $target -Force
    }
}
foreach($name in @('README.md','ATTRIBUTION.md','.gitignore','settings.gradle','build.gradle')){Copy-Item -LiteralPath (Join-Path $root $name) -Destination $stage -Force}
Compress-Archive -Path (Join-Path $stage '*') -DestinationPath (Join-Path $root 'dist/skylyrics-source-0.1.0.zip') -Force
Get-ChildItem (Join-Path $root 'dist') -File | Where-Object {$_.Extension -in @('.jar','.mjs','.zip')} | Get-FileHash -Algorithm SHA256 | ForEach-Object {"$($_.Hash.ToLower())  $([IO.Path]::GetFileName($_.Path))"} | Set-Content (Join-Path $root 'dist/SHA256SUMS.txt')
