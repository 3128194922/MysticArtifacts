$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$itemPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java'
$eventsPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/event/KatanaEvents.java'
$slashPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java'
$circlePath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java'
$entitiesPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/init/ModEntities.java'

$itemText = Get-Content -Raw $itemPath
$eventsText = Get-Content -Raw $eventsPath
$slashText = if (Test-Path $slashPath) { Get-Content -Raw $slashPath } else { '' }
$circleText = if (Test-Path $circlePath) { Get-Content -Raw $circlePath } else { '' }
$entitiesText = Get-Content -Raw $entitiesPath
$failures = [System.Collections.Generic.List[string]]::new()

if ($itemText -match 'applyDirectHealthCost') {
    $failures.Add('MuramasaItem still contains the removed direct health cost')
}
if ($itemText -notmatch 'KatanaState\.addEnergy') {
    $failures.Add('MuramasaItem does not delegate energy changes to KatanaState')
}
if ($eventsText -notmatch 'KatanaState\.addEnergy\(stack, KatanaState\.BLOCK_ENERGY\)') {
    $failures.Add('KatanaEvents does not restore 10 energy for a successful block')
}
if (-not (Test-Path $slashPath) -or $slashText -notmatch 'playerAttack') {
    $failures.Add('KatanaSlashEntity is missing or does not use playerAttack damage')
}
if (-not (Test-Path $circlePath) -or $circleText -notmatch 'playerAttack') {
    $failures.Add('KatanaCircleSlashEntity is missing or does not use playerAttack damage')
}
if ($entitiesText -notmatch 'KATANA_SLASH' -or $entitiesText -notmatch 'KATANA_CIRCLE_SLASH') {
    $failures.Add('ModEntities does not register both katana slash entity types')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana contract: PASS'
