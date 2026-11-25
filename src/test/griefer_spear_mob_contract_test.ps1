$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mainRoot = Join-Path $projectRoot 'src/main'

$grieferItem = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/item/impl/GrieferSpearItem.java')
$spearItem = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/item/impl/SpearItem.java')
$mobMixin = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/mixin/MobSpearMixin.java')
$spawnEvent = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/event/SpearMobSpawnEvents.java')
$grieferModel = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/griefer_spear.json')
$zombieHolder = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/griefer_spear_zombie_in_hand.json')

if ($grieferItem -notmatch 'class\s+GrieferSpearItem\s+extends\s+SpearItem') {
    throw 'GrieferSpearItem must inherit SpearItem so mob melee uses spear AI'
}
if ($mobMixin -notmatch 'instanceof SpearItem') {
    throw 'MobSpearMixin must recognize all SpearItem subclasses'
}
if ($spawnEvent -notmatch 'ModItems\.GRIEFER_SPEAR\.get\(\)') {
    throw 'Mob spawn event does not include the Griefer Spear'
}
if ($spawnEvent -notmatch 'SpearMobGrieferChance') {
    throw 'Griefer Spear mob spawn chance is not wired'
}
if ($grieferModel -notmatch 'mysticartifacts:zombie_holding' -or $grieferModel -notmatch 'griefer_spear_zombie') {
    throw 'Griefer Spear is missing its zombie-held model override'
}
if ($zombieHolder -notmatch 'mysticartifacts:item/custom/spear_zombie') {
    throw 'Griefer Spear zombie model does not reuse the zombie hold transform'
}
if ($spearItem -notmatch 'onSpearHit' -or $spearItem -notmatch 'stopAfterTarget\s*=\s*onSpearHit') {
    throw 'Spear melee does not invoke the per-hit effect hook'
}
if ($grieferItem -notmatch 'protected\s+boolean\s+onSpearHit' -or $grieferItem -notmatch 'level\.explode') {
    throw 'Griefer Spear does not explode on mob melee hits'
}
if ($grieferItem -notmatch 'attacker\s+instanceof\s+Player' -or $grieferItem -notmatch 'return\s+false') {
    throw 'Griefer Spear melee explosion must stay disabled for players'
}

Write-Output 'griefer spear mob contract: PASS'
