$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteRenderer.java'
$text = if (Test-Path $rendererPath) { Get-Content -Raw $rendererPath } else { '' }
$failures = @()

if (-not (Test-Path $rendererPath)) {
    $failures += 'missing renderer source'
} else {
    foreach ($check in @(
        '*SCULK_MARKER_RENDER_TYPE*',
        '*setDepthTestState*',
        '*NO_DEPTH_TEST*',
        '*getBuffer(SCULK_MARKER_RENDER_TYPE)*',
        '*endBatch(SCULK_MARKER_RENDER_TYPE)*'
    )) {
        if ($text -notlike $check) { $failures += "missing depth-independent renderer contract: $check" }
    }
    if ($text -like '*RenderType.debugQuads*') {
        $failures += 'renderer must not use the depth-tested debug quad layer'
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote render depth contract: PASS'
