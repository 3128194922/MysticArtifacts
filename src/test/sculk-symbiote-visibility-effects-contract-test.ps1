$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$serverPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteServerHandler.java'
$clientPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteClientState.java'
$server = Get-Content -Raw $serverPath
$client = Get-Content -Raw $clientPath
$failures = @()

foreach ($check in @(
    '*MobEffects.BLINDNESS*',
    '*MobEffects.DARKNESS*',
    '*MobEffectInstance*',
    '*addEffect*'
)) {
    if ($server -notlike $check) { $failures += "missing effect contract: $check" }
}

foreach ($check in @(
    '*setActive*',
    '*acceptMarkers*',
    '*MARKER_LIFETIME*'
)) {
    if ($client -notlike $check) { $failures += "missing marker state contract: $check" }
}

$hiddenRendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/HiddenEntityRenderer.java'
$hasHiddenBehavior = $client -like '*setRenderHitBoxes(false)*' -or $client -like '*setRenderShadow(false)*' -or $client -like '*HiddenEntityRenderer*' -or (Test-Path -LiteralPath $hiddenRendererPath)
if ($hasHiddenBehavior) {
    $failures += 'wearing must not hide entity models, shadows, or hitboxes'
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote visibility/effects contract: PASS'
