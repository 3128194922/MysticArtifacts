$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$meshPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashMesh.java'
$slashPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java'
$circlePath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaCircleSlashRenderer.java'
$texturePath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/textures/entity/katana_slash.png'
$mesh = Get-Content -Raw $meshPath
$slash = Get-Content -Raw $slashPath
$circle = Get-Content -Raw $circlePath
$failures = @()

foreach ($check in @(
    @{ Text = $mesh; Name = 'blood arc mesh'; Tokens = @('renderBloodArcLayer', 'segmentAlpha', 'drawSide', 'OUTER_SPIKE') },
    @{ Text = $slash; Name = 'straight slash renderer'; Tokens = @('STYLE_GHOST', 'renderBloodArcLayer', 'renderEchoArc', 'renderCoreArc') },
    @{ Text = $circle; Name = 'circle slash renderer'; Tokens = @('renderCircleBloodArc', 'pulse', 'renderCoreRing') }
)) {
    foreach ($token in $check.Tokens) {
        if ($check.Text -notlike "*$token*") {
            $failures += "$($check.Name) is missing visual token: $token"
        }
    }
}

if (-not (Test-Path -LiteralPath $texturePath)) {
    $failures += 'katana slash texture is missing'
} else {
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $texturePath))
    try {
        if ($bitmap.Width -ne 64 -or $bitmap.Height -ne 64) {
            $failures += "katana slash texture must remain 64x64, got $($bitmap.Width)x$($bitmap.Height)"
        }
        $transparentPixels = 0
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            for ($y = 0; $y -lt $bitmap.Height; $y++) {
                if ($bitmap.GetPixel($x, $y).A -lt 255) { $transparentPixels++ }
            }
        }
        if ($transparentPixels -lt 64) {
            $failures += 'katana slash texture must retain a transparent fade area'
        }
    } finally {
        $bitmap.Dispose()
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana art direction contract: PASS'
