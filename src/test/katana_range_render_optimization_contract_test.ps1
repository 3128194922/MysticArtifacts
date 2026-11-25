$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/Config.java'
$entityPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java'
$meshPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashMesh.java'
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java'
$configText = Get-Content -Raw $configPath
$entityText = Get-Content -Raw $entityPath
$meshText = Get-Content -Raw $meshPath
$rendererText = Get-Content -Raw $rendererPath

if ($configText -notmatch 'KatanaGhostSlashRange') {
    throw 'Katana ghost slash range is not configurable'
}
if ($configText -notmatch 'defineInRange\("KatanaGhostSlashRange",\s*12\.0') {
    throw 'Katana ghost slash range default is not 12 blocks'
}
if ($entityText -match 'GHOST_RANGE\s*=\s*5\.0D') {
    throw 'Katana slash still uses the old five-block hardcoded range'
}
if ($entityText -notmatch 'Config\.KatanaGhostSlashRange') {
    throw 'Katana slash entity does not use the configurable range'
}
if ($meshText -notmatch 'KatanaSlashRenderProfile') {
    throw 'Katana mesh does not use the precomputed render profile'
}
if ($rendererText -notmatch 'segmentsForDistanceSqr') {
    throw 'Katana renderer does not select a distance-based LOD'
}
if ($rendererText -notmatch 'KatanaGhostSlashRange') {
    throw 'Katana renderer does not keep the sword qi visible through the configured range'
}

Write-Output 'katana range and render optimization contract: PASS'
