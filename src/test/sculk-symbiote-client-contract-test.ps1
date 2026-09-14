$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteClientState.java'
$hiddenPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/HiddenEntityRenderer.java'
$configPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientConfig.java'
$state = Get-Content -Raw $statePath
$hidden = Get-Content -Raw $hiddenPath
$config = Get-Content -Raw $configPath
$failures = @()

foreach ($check in @(
    '*TrailSightItem.isWearing*',
    '*setRenderHitBoxes(false)*',
    '*setRenderShadow(false)*',
    '*HiddenEntityRenderer.wrap*',
    '*dispatcher.renderers*',
    '*getSkinMap()*',
    '*MARKER_LIFETIME*',
    '*acceptMarkers*',
    '*trim*'
)) {
    if ($state -notlike $check -and $hidden -notlike $check -and $config -notlike $check) {
        $failures += "missing client contract: $check"
    }
}

if ($config -notlike '*sculkSymbioteMarkerLifetime", 20*') {
    $failures += 'marker lifetime must default to 20 ticks'
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote client contract: PASS'
