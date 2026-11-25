$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$rendererPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/lightning/LightningBottleRenderer.java'
$renderTypesPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/lightning/LightningBottleRenderTypes.java'

$renderer = Get-Content -Raw $rendererPath
$renderTypes = if (Test-Path $renderTypesPath) { Get-Content -Raw $renderTypesPath } else { '' }

foreach ($layer in @('outer', 'middle', 'inner')) {
    if ($renderer -notmatch "LightningBottleRenderTypes\.$layer\(\)") {
        throw "Lightning renderer has no $layer layer pass"
    }
    if ($renderTypes -notmatch "\b$layer\s*\(") {
        throw "Lightning render types has no $layer render type"
    }
}

foreach ($constant in @('OUTER_WIDTH', 'MIDDLE_WIDTH', 'INNER_WIDTH', 'OUTER_COLOR', 'MIDDLE_COLOR', 'INNER_COLOR')) {
    if ($renderer -notmatch $constant -and $renderTypes -notmatch $constant) {
        throw "Lightning layer constant missing: $constant"
    }
}

if ($renderer -notmatch 'gameTime' -or $renderer -notmatch 'seed') {
    throw 'Lightning layers are not time/seed driven'
}
if ($renderer -notmatch 'addQuad') {
    throw 'Lightning layers do not render controllable-width ribbons'
}
if ($renderer -notmatch 'OUTER_WIDTH\s*=\s*0\.09F' -or
    $renderer -notmatch 'MIDDLE_WIDTH\s*=\s*0\.0475F' -or
    $renderer -notmatch 'INNER_WIDTH\s*=\s*0\.021F') {
    throw 'Lightning layer widths are not reduced to half of the previous profile'
}
if ($renderTypes -notmatch 'ADDITIVE_TRANSPARENCY') {
    throw 'Lightning render types do not provide additive glow'
}

Write-Output 'lightning bottle layer contract: PASS'
