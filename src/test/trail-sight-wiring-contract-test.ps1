$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$handlerPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientHandler.java'
$text = if (Test-Path $handlerPath) { Get-Content -Raw $handlerPath } else { '' }
$checks = @(
    '*ClientPlayerNetworkEvent.LoggingOut*',
    '*LevelEvent.Unload*',
    '*SculkSymbioteClientState.clear()*',
    '*Dist.CLIENT*',
    '*Bus.FORGE*'
)
$failures = @()

if (-not (Test-Path $handlerPath)) {
    $failures += 'missing client lifecycle handler'
} else {
    foreach ($check in $checks) {
        if ($text -notlike $check) { $failures += "missing wiring contract: $check" }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'trail sight wiring contract: PASS'
