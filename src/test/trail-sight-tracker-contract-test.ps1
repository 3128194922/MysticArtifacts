$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$trackerPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightTracker.java'
$text = if (Test-Path $trackerPath) { Get-Content -Raw $trackerPath } else { '' }
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

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'trail sight tracker contract: PASS'
