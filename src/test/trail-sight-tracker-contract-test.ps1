$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$trackerPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightTracker.java'
$configPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientConfig.java'
$text = if (Test-Path $trackerPath) { Get-Content -Raw $trackerPath } else { '' }
$configText = if (Test-Path $configPath) { Get-Content -Raw $configPath } else { '' }
$checks = @(
    '*ClientLevel*',
    '*LocalPlayer*',
    '*ClientTickEvent*',
    '*UUID*',
    '*entitiesForRendering*',
    '*SAMPLE_INTERVAL*',
    '*MIN_STEP*',
    '*RANGE*',
    '*MAX_ENTITIES*',
    '*TrailSightItem.isWearing*',
    '*shouldRecordMovement*',
    '*horizontalDistanceSqr <= 0.0D*',
    '*selectNearestEntities*',
    '*clear()*',
    '*getTracks*'
)
$failures = @()

if (-not (Test-Path $trackerPath)) {
    $failures += 'missing tracker source'
} else {
    foreach ($check in $checks) {
        if ($text -notlike $check) { $failures += "missing tracker contract: $check" }
    }
}

if (-not (Test-Path $configPath)) {
    $failures += 'missing client config source'
} elseif ($configText -notlike '*trailSightMinStep", 0.0D, 0.0D*') {
    $failures += 'minimum movement distance must default to zero'
}

if (-not (Test-Path $configPath)) {
    $failures += 'missing retention config source'
} elseif ($configText -notlike '*trailSightRetentionTicks", 300, 20, 1200*') {
    $failures += 'trail retention must default to 300 ticks and allow up to 1200 ticks'
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'trail sight tracker contract: PASS'
