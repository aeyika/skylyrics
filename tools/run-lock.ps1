function Enter-SkyLyricsRunLock([string]$projectRoot) {
    $sha=[Security.Cryptography.SHA256]::Create()
    try { $id=[BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectRoot.ToLowerInvariant()))).Replace('-','').Substring(0,16) } finally { $sha.Dispose() }
    $mutex=New-Object Threading.Mutex($false,"Local\SkyLyrics-$id")
    try { $acquired=$mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $acquired=$true }
    if(!$acquired){$mutex.Dispose();throw 'A Sky Lyrics build or test client is already running. Wait for it to finish before starting another.'}
    return $mutex
}
