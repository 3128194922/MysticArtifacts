$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$itemPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java'
$eventsPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/event/KatanaEvents.java'
$slashPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java'
$circlePath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java'
$entitiesPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/init/ModEntities.java'
$mainPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/MysticArtifacts.java'
$modelPath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/models/item/katana.json'
$slashTexturePath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/textures/entity/katana_slash.png'
$circleTexturePath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/textures/entity/katana_circle_slash.png'
$sheathedTexturePath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/textures/item/katana_sheathed.png'
$meshPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashMesh.java'

$itemText = Get-Content -Raw $itemPath
$eventsText = Get-Content -Raw $eventsPath
$slashText = if (Test-Path $slashPath) { Get-Content -Raw $slashPath } else { '' }
$circleText = if (Test-Path $circlePath) { Get-Content -Raw $circlePath } else { '' }
$entitiesText = Get-Content -Raw $entitiesPath
$mainText = Get-Content -Raw $mainPath
$modelText = Get-Content -Raw $modelPath
$meshText = if (Test-Path $meshPath) { Get-Content -Raw $meshPath } else { '' }
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
if ($itemText -notmatch 'KatanaSlashEntity\.createDash') {
    $failures.Add('MuramasaItem does not create the dash slash entity')
}
if ($itemText -notmatch 'KatanaCircleSlashEntity\.create') {
    $failures.Add('MuramasaItem does not create the circle slash entity')
}
if ($itemText -notmatch 'triggerOpenSlash') {
    $failures.Add('MuramasaItem does not expose the open slash trigger')
}
if ($eventsText -notmatch 'AttackEntityEvent') {
    $failures.Add('KatanaEvents does not handle open left-click entity attacks')
}
if (-not (Test-Path $slashTexturePath) -or -not (Test-Path $circleTexturePath) -or -not (Test-Path $sheathedTexturePath)) {
    $failures.Add('Katana texture resources are incomplete')
}
if ($mainText -notmatch 'EntityRenderers\.register\(ModEntities\.KATANA_SLASH' -or
    $mainText -notmatch 'EntityRenderers\.register\(ModEntities\.KATANA_CIRCLE_SLASH') {
    $failures.Add('Katana entity renderers are not registered')
}
if ($meshText -notmatch 'renderArc' -or $meshText -notmatch 'renderRing' -or $meshText -notmatch 'segments') {
    $failures.Add('Katana renderer does not use continuous slash mesh geometry')
}
if ($modelText -notmatch 'mysticartifacts:open' -or $modelText -notmatch 'katana_sheathed' -or
    $modelText -notmatch 'katana_open') {
    $failures.Add('Katana item model does not switch between sheathed and open textures')
}
if ($slashText -match 'SlashBlade|resharped' -or $circleText -match 'SlashBlade|resharped') {
    $failures.Add('Katana slash entities reference SlashBlade resources')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana contract: PASS'
