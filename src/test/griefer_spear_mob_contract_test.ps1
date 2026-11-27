$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mainRoot = Join-Path $projectRoot 'src/main'

$grieferItem = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/item/impl/GrieferSpearItem.java')
$spearItem = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/item/impl/SpearItem.java')
$mobMixin = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/mixin/MobSpearMixin.java')
$spawnEvent = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/event/SpearMobSpawnEvents.java')
$grieferModel = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/griefer_spear.json')
$zombieHolder = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/griefer_spear_zombie_in_hand.json')
$pseudoExplosion = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/util/PseudoExplosion.java')

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
if ($grieferItem -notmatch 'protected\s+boolean\s+onSpearHit') {
    throw 'Griefer Spear does not hook mob melee hits'
}
if ($grieferItem -match 'level\.explode') {
    throw 'Griefer Spear must not use the vanilla explosion (it destroys blocks and fires ExplosionEvent)'
}
if ($grieferItem -notmatch 'PseudoExplosion\.detonate') {
    throw 'Griefer Spear does not use the pseudo explosion for its area damage'
}
if ($grieferItem -notmatch 'attacker\s+instanceof\s+Player' -or $grieferItem -notmatch 'return\s+false') {
    throw 'Griefer Spear melee explosion must stay disabled for players'
}
if ($pseudoExplosion -match '\.explode\(' -or $pseudoExplosion -match 'ExplosionInteraction') {
    throw 'PseudoExplosion must stay a particle-only fake explosion'
}
if ($pseudoExplosion -notmatch 'ParticleTypes\.EXPLOSION_EMITTER' -or $pseudoExplosion -notmatch 'SoundEvents\.GENERIC_EXPLODE') {
    throw 'PseudoExplosion must still spawn explosion particles and sound'
}
if ($pseudoExplosion -notmatch 'getEntities\(' -or $pseudoExplosion -notmatch 'damageAt\(') {
    throw 'PseudoExplosion must resolve area damage through an AABB entity query'
}

Write-Output 'griefer spear mob contract: PASS'
