$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail/TrailSightRenderer.java'
$text = if (Test-Path $rendererPath) { Get-Content -Raw $rendererPath } else { '' }
$checks = @(
    '*RenderLevelStageEvent*',
    '*AFTER_ENTITIES*',
    '*PoseStack*',
    '*getMainCamera*',
    '*RenderType.debugQuads*',
    '*getTracks*',
    '*directionX*',
    '*directionZ*',
    '*retention*',
    '*alpha*',
    '*endBatch*'
)
$failures = @()

if (-not (Test-Path $rendererPath)) {
    $failures += 'missing renderer source'
} else {
    foreach ($check in $checks) {
        if ($text -notlike $check) { $failures += "missing renderer contract: $check" }
    }
    if ($text -like '*NetworkHandler*' -or $text -like '*sendToServer*') {
        $failures += 'renderer must not send trail network packets'
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'trail sight render contract: PASS'
