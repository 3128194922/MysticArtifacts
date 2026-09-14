$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts'
$resourcesRoot = Join-Path $projectRoot 'main/resources/assets/mysticartifacts'
$failures = @()

$itemsText = Get-Content -Raw (Join-Path $javaRoot 'init/ModItems.java')
$itemPath = Join-Path $javaRoot 'item/impl/TrailSightItem.java'
$itemText = if (Test-Path $itemPath) { Get-Content -Raw $itemPath } else { '' }
$configPath = Join-Path $javaRoot 'client/trail/TrailSightClientConfig.java'
$configText = if (Test-Path $configPath) { Get-Content -Raw $configPath } else { '' }
$mainText = Get-Content -Raw (Join-Path $javaRoot 'MysticArtifacts.java')
$tabsText = Get-Content -Raw (Join-Path $javaRoot 'init/ModCreativeModTabs.java')
$modelPath = Join-Path $resourcesRoot 'models/item/trail_sight.json'
$zhText = Get-Content -Raw (Join-Path $resourcesRoot 'lang/zh_cn.json')
$enText = Get-Content -Raw (Join-Path $resourcesRoot 'lang/en_us.json')

$hasItemName = $itemsText -like '*TRAIL_SIGHT*'
$hasItemRegistration = $itemsText -like '*ITEMS.register*trail_sight*'
if (-not $hasItemName) { $failures += 'missing item name' }
if (-not $hasItemRegistration) { $failures += 'missing item registration' }
if (-not (Test-Path $itemPath)) { $failures += 'missing item source' }
if ($itemText -notlike '*implements ICurioItem*') { $failures += 'missing ICurioItem' }
if ($itemText -notlike '*canEquipFromUse*') { $failures += 'missing auto equip' }
if (-not (Test-Path $configPath)) { $failures += 'missing client config' }
if ($configText -notlike '*ForgeConfigSpec*') { $failures += 'missing ForgeConfigSpec' }
if ($configText -notlike '*MARKER_LIFETIME*') { $failures += 'missing marker lifetime config' }
if ($mainText -notlike '*ModConfig.Type.CLIENT*') { $failures += 'missing client config registration' }
if ($mainText -notlike '*TrailSightClientConfig.SPEC*') { $failures += 'missing config spec registration' }
if ($tabsText -notlike '*ModItems.TRAIL_SIGHT.get()*') { $failures += 'missing creative tab item' }
if (-not (Test-Path $modelPath)) { $failures += 'missing item model' }
if ($zhText -notlike '*幽匿共生体*') { $failures += 'missing zh sculk symbiote name' }
if ($enText -notlike '*Sculk Symbiote*') { $failures += 'missing en sculk symbiote name' }

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'trail sight registration contract: PASS'
