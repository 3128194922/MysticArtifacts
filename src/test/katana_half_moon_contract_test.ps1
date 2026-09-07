$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$meshPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashMesh.java'
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java'
$circleRendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaCircleSlashRenderer.java'

$meshText = Get-Content -Raw $meshPath
$rendererText = Get-Content -Raw $rendererPath
$circleRendererText = Get-Content -Raw $circleRendererPath
$failures = [System.Collections.Generic.List[string]]::new()

if ($meshText -notmatch 'renderHalfMoonLayer' -or
    $meshText -notmatch 'HALF_MOON_ANGLE_DEGREES' -or
    $meshText -notmatch 'ARC_SEGMENTS' -or
    $meshText -notmatch 'Math\.toRadians') {
    $failures.Add('Katana mesh does not define a procedural half-moon arc')
}
if ($meshText -match 'slash\.obj|SlashBlade|Math\.PI \* 2\.0[D|F]') {
    $failures.Add('Katana mesh still contains SlashBlade or full-circle mesh logic')
}
if ($rendererText -notmatch 'renderHalfMoonLayer' -or $rendererText -match 'renderSlashBlade') {
    $failures.Add('Katana slash renderer does not use the half-moon mesh')
}
if ($circleRendererText -notmatch 'renderHalfMoonLayer' -or $circleRendererText -match 'renderSlashBlade') {
    $failures.Add('Katana circle renderer does not use the half-moon mesh')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana half-moon contract: PASS'
