param([ValidateSet('all','1.21.11','26.2')][string]$Target='all', [ValidateSet('default','opengl','vulkan')][string]$Backend='default', [ValidateSet('none','sodium','iris')][string]$Compatibility='none', [int]$Frames=0, [switch]$Multiplayer, [switch]$Photon)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'run-lock.ps1')
$skyRunLock=Enter-SkyLyricsRunLock $root
try {
$live=Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -like "*$root*" -and $_.CommandLine -like '*net.fabricmc.devlaunchinjector.Main*' }
if($live){throw 'Close the development client before running client game tests.'}
$env:GRADLE_USER_HOME=Join-Path $root '.gradle-home'
if($Photon){$Compatibility='iris'; & (Join-Path $PSScriptRoot 'fetch-photon.ps1')}
$testConfig=Join-Path $root 'test-runs/gametest-shared-config'
New-Item -ItemType Directory -Force $testConfig|Out-Null
$prefsPath=Join-Path $testConfig 'preferences.json'
if(Test-Path -LiteralPath $prefsPath){$testPrefs=Get-Content -LiteralPath $prefsPath -Raw|ConvertFrom-Json;$testPrefs.port=19586}
else {
    $tokenBytes=New-Object byte[] 32;$rng=[Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($tokenBytes);$rng.Dispose()
    $testPrefs=@{schema=1;enabled=$true;offsetMs=0;distance=12;width=440;scale=1;opacity=1;occlusion=$true;blur=$true;glow=$true;port=19586;bridgeToken=([BitConverter]::ToString($tokenBytes)).Replace('-','').ToLower();skyboxAnchor=$false}
}
$testPrefs|ConvertTo-Json|Set-Content -LiteralPath $prefsPath -Encoding UTF8
foreach($name in @('1.21.11','26.2')){
    if($Target -ne 'all' -and $Target -ne $name){continue}
    if($Compatibility -ne 'none'){
        $candidates=Get-Content (Join-Path $PSScriptRoot 'compatibility-candidates.json') -Raw|ConvertFrom-Json
        foreach($candidate in $candidates|Where-Object {$_.minecraft -eq $name -and ($_.mod -eq 'sodium' -or $Compatibility -eq 'iris')}){
            $modFile=Join-Path $PSScriptRoot "compat-mods/$name/$($candidate.file.filename)"
            if(!(Test-Path -LiteralPath $modFile)){throw "Missing test dependency: $modFile"}
            if((Get-FileHash -LiteralPath $modFile -Algorithm SHA512).Hash.ToLower() -ne $candidate.file.hashes.sha512){throw "Compatibility dependency checksum mismatch for $name"}
        }
    }
    $major=if($name -eq '1.21.11'){21}else{25}
    $env:JAVA_HOME=(Get-ChildItem -Directory (Join-Path $PSScriptRoot "jdk$major")|Select-Object -First 1).FullName
    if($name -eq '1.21.11' -and $Backend -eq 'vulkan'){throw 'Minecraft 1.21.11 has no native Vulkan backend'}
    $suffix=if($Backend -eq 'default' -and $Compatibility -eq 'none'){''}else{"-$Backend-$Compatibility"}
    if($Photon){$suffix+='-photon'}
    $run=Join-Path $root "test-runs/gametest-$name$suffix"
    New-Item -ItemType Directory -Force $run|Out-Null
    if($Photon){
        $photonLock=Get-Content (Join-Path $PSScriptRoot 'photon.lock.json') -Raw|ConvertFrom-Json
        New-Item -ItemType Directory -Force (Join-Path $run 'shaderpacks'),(Join-Path $run 'config')|Out-Null
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'shaderpacks/photon_v1.4.zip') -Destination (Join-Path $run 'shaderpacks/photon_v1.4.zip') -Force
        [IO.File]::WriteAllText((Join-Path $run 'config/iris.properties'),"shaderPack=photon_v1.4.zip`nenableShaders=true`nallowUnknownShaders=false`n")
    }
    if($name -eq '26.2'){
        $options=Join-Path $run 'options.txt'
        $content=if(Test-Path $options){[IO.File]::ReadAllText($options)}else{''}
        $line="preferredGraphicsBackend:`"$Backend`""
        if($content -match '(?m)^preferredGraphicsBackend:.*$'){$content=[regex]::Replace($content,'(?m)^preferredGraphicsBackend:.*$',$line)}else{$content+="`n$line`n"}
        [IO.File]::WriteAllText($options,$content)
    }
    & (Join-Path $PSScriptRoot 'gradle-9.5.1/bin/gradle.bat') -p (Join-Path $root "fabric-$name") runClientGameTest "-PskylyricsTestSuffix=$suffix" "-PskylyricsCompat=$Compatibility" "-PskylyricsCaptureFrames=$Frames" "-PskylyricsMultiplayer=$($Multiplayer.IsPresent.ToString().ToLower())" "-PskylyricsPhoton=$($Photon.IsPresent.ToString().ToLower())" --no-daemon --console=plain
    if($LASTEXITCODE -ne 0){throw "$name client game tests failed"}
}
} finally { $skyRunLock.ReleaseMutex();$skyRunLock.Dispose() }
