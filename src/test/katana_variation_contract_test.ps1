$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$slashPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java'
$circlePath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java'

$slashText = Get-Content -Raw $slashPath
$circleText = Get-Content -Raw $circlePath
$failures = [System.Collections.Generic.List[string]]::new()

if ($slashText -notmatch 'VISUAL_ROLL_RANGE_DEGREES' -or
    $slashText -notmatch 'VISUAL_OFFSET_RANGE_DEGREES' -or
    $slashText -notmatch 'VISUAL_SIZE_MIN' -or
    $slashText -notmatch 'VISUAL_SIZE_MAX') {
    $failures.Add('Katana slash entity does not define bounded visual variation constants')
}
if ($slashText -notmatch 'createDash[\s\S]*setRotationRoll[\s\S]*setRotationOffset[\s\S]*setBaseSize') {
    $failures.Add('Dash slash creation does not assign synchronized visual variation')
}
if ($slashText -notmatch 'createGhostSlash[\s\S]*setRotationRoll[\s\S]*setRotationOffset[\s\S]*setBaseSize') {
    $failures.Add('Ghost slash creation does not assign synchronized visual variation')
}
if ($circleText -notmatch 'VISUAL_ROLL_RANGE_DEGREES' -or
    $circleText -notmatch 'VISUAL_OFFSET_RANGE_DEGREES' -or
    $circleText -notmatch 'VISUAL_SIZE_MIN' -or
    $circleText -notmatch 'VISUAL_SIZE_MAX' -or
    $circleText -notmatch 'void setRotationRoll' -or
    $circleText -notmatch 'void setBaseSize' -or
    $circleText -notmatch 'create[\s\S]*setRotationRoll[\s\S]*setRotationOffset[\s\S]*setBaseSize') {
    $failures.Add('Circle slash creation does not assign synchronized visual variation')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana variation contract: PASS'
