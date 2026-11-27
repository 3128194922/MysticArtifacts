$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$eventPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/event/LightningBottleEvents.java'
$logicPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/lightning/LightningBottleLogic.java'
$tagPath = Join-Path $projectRoot 'src/main/resources/data/minecraft/tags/damage_type/is_lightning.json'

if (-not (Test-Path $tagPath)) {
    throw 'lightning_bottle_chain is not registered in the mod is_lightning tag'
}
$tag = Get-Content -Raw $tagPath
if ($tag -notmatch 'mysticartifacts:lightning_bottle_chain') {
    throw 'lightning_bottle_chain is missing from the mod is_lightning tag'
}

$logic = Get-Content -Raw $logicPath
if ($logic -notmatch 'wetnessMultiplier' -or $logic -notmatch '\*\s*0\.2F') {
    throw 'wetness multiplier formula is missing from LightningBottleLogic'
}
if ($logic -notmatch 'primaryBonus') {
    throw 'primary bonus wetness rule is missing from LightningBottleLogic'
}

$events = Get-Content -Raw $eventPath
if ($events -notmatch 'DamageTypeTags\.IS_LIGHTNING') {
    throw 'LightningBottleEvents does not avoid double-amplifying lightning sources'
}
if ($events -notmatch 'LightningBottleLogic\.primaryBonus' -or $events -notmatch 'primaryBonus') {
    throw 'LightningBottleEvents does not apply wetness to the primary bonus'
}
if ($events -notmatch 'cataclysm.*wetness') {
    throw 'Cataclysm wetness effect lookup is missing'
}

Write-Output 'lightning bottle wetness contract: PASS'
