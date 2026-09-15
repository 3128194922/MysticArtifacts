$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteClientState.java'
$configPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientConfig.java'
$state = Get-Content -Raw $statePath
$config = Get-Content -Raw $configPath
$failures = @()

foreach ($check in @(
    '*TrailSightItem.isWearing*',
    '*MARKER_LIFETIME*',
    '*acceptMarkers*',
    '*trim*',
    '*isActive*'
)) {
    if ($state -notlike $check -and $config -notlike $check) {
        $failures += "missing client contract: $check"
    }
}

$hasHiddenBehavior = $state -like '*setRenderHitBoxes*' -or $state -like '*setRenderShadow*' -or $state -like '*HiddenEntityRenderer*'
if ($hasHiddenBehavior) {
    $failures += 'wearing must not hide entity models, shadows, or hitboxes'
}

if ($config -notlike '*sculkSymbioteMarkerLifetime", 20*') {
    $failures += 'marker lifetime must default to 20 ticks'
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote client contract: PASS'
