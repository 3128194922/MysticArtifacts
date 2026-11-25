$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$modelPath = Join-Path $projectRoot 'src/main/resources/assets/mysticartifacts/models/item/lightning_bottle.json'
$texturePath = Join-Path $projectRoot 'src/main/resources/assets/mysticartifacts/textures/item/bottled_lightning.png'
$modelText = Get-Content -Raw $modelPath

if ($modelText -notmatch 'mysticartifacts:item/bottled_lightning') {
    throw 'Lightning bottle model does not reference the mod texture namespace'
}
if ($modelText -match 'minecraft:item/bottled_lightning') {
    throw 'Lightning bottle model still references a missing vanilla texture'
}
if (-not (Test-Path $texturePath)) {
    throw 'Lightning bottle texture asset is missing'
}

Write-Output 'lightning bottle texture contract: PASS'
