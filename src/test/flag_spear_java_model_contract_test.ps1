$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mainRoot = Join-Path $projectRoot 'src/main'
$modelPath = Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/model/FlagSpearModel.java'
$rendererPath = Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/render/FlagSpearItemRenderer.java'
$itemPath = Join-Path $mainRoot 'java/com/uniye/mysticartifacts/item/impl/FlagSpearItem.java'

foreach ($path in @($modelPath, $rendererPath, $itemPath)) {
    if (-not (Test-Path $path)) {
        throw "Missing Java model integration file: $path"
    }
}

$modelText = Get-Content -Raw $modelPath
$rendererText = Get-Content -Raw $rendererPath
$itemText = Get-Content -Raw $itemPath

if ($modelText -notmatch 'class\s+FlagSpearModel') {
    throw 'Blockbench Java model class is missing'
}
if ($modelText -notmatch 'ModelPart' -or $modelText -notmatch 'renderToBuffer') {
    throw 'Flag spear model is not an original Java ModelPart renderer'
}
if ($rendererText -notmatch 'extends\s+BlockEntityWithoutLevelRenderer') {
    throw 'Flag spear item renderer is not a Forge custom item renderer'
}
if ($rendererText -notmatch 'FlagSpearModel') {
    throw 'Flag spear item renderer does not use the Blockbench Java model'
}
if ($rendererText -notmatch 'ItemDisplayContext') {
    throw 'Flag spear renderer does not handle item display contexts'
}
if ($itemText -notmatch 'initializeClient') {
    throw 'Flag spear item does not expose client model rendering'
}
if ($itemText -notmatch 'FlagSpearItemRenderer') {
    throw 'Flag spear item does not bind the Java model renderer'
}

Write-Output 'flag spear Java model contract: PASS'
