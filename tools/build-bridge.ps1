$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$upstream = Join-Path $root 'upstream/beautiful-lyrics'
$revision = (& git -C $upstream rev-parse HEAD).Trim()
if ($revision -ne '61ac582da092311e893423269ca7f09003108705') { throw "Unexpected Beautiful Lyrics revision: $revision" }
& node (Join-Path $PSScriptRoot 'patch-upstream.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Upstream patch failed' }
$env:DENO_DIR = Join-Path $root '.deno-cache'
$lock=Join-Path $root 'spicetify-bridge/deno.lock'
if(Test-Path -LiteralPath $lock){Copy-Item -LiteralPath $lock -Destination (Join-Path $upstream 'Extension/deno.lock') -Force}
Push-Location (Join-Path $upstream 'Extension')
try {
    & (Join-Path $PSScriptRoot 'deno/deno.exe') run --allow-read --allow-write --allow-net --allow-env --allow-run --allow-sys --config deno.json (Join-Path $root 'spicetify-bridge/build.ts')
    if ($LASTEXITCODE -ne 0) { throw 'Bridge build failed' }
} finally { Pop-Location }
& node --test (Join-Path $root 'spicetify-bridge/test/normalize.test.mjs') (Join-Path $root 'spicetify-bridge/test/playback.test.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Bridge tests failed' }
