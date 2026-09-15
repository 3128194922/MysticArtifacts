$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$handlerPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteServerHandler.java'
$text = Get-Content -Raw $handlerPath
$failures = @()

if ($text -notlike '*new MobEffectInstance(effect, 40, 0*') {
    $failures += 'effect refresh duration must keep a 40 tick client buffer'
}
if ($text -notlike '*current.getDuration() <= 20*') {
    $failures += 'effect refresh threshold must be 20 ticks or earlier'
}
if ($text -like '*new MobEffectInstance(effect, 10, 0*' -or $text -like '*current.getDuration() <= 5*') {
    $failures += 'short effect refresh window can cause client-side flicker'
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote effect refresh contract: PASS'
