$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$packetPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/network/FlagSpearTrailPacket.java'
$statePath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearClientState.java'
$rendererPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderer.java'
$renderTypesPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderTypes.java'
$pathPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearSphericalPath.java'

$packet = Get-Content -Raw $packetPath
$state = Get-Content -Raw $statePath
$renderer = Get-Content -Raw $rendererPath
$renderTypes = Get-Content -Raw $renderTypesPath
$path = if (Test-Path $pathPath) { Get-Content -Raw $pathPath } else { '' }

if ($packet -notmatch 'long\s+seed' -or $packet -notmatch 'writeLong\(' -or $packet -notmatch 'readLong\(') {
    throw 'Flag spear packet does not carry a client trail seed'
}
if ($state -notmatch 'accept\s*\(\s*Vec3\s+center\s*,\s*float\s+yaw\s*,\s*long\s+seed') {
    throw 'Flag spear client state does not accept a seed'
}
if ($renderer -notmatch 'FlagSpearSphericalPath\.point\(') {
    throw 'Flag spear renderer does not use client spherical path generation'
}
if ($path -notmatch 'RADIUS' -or $path -notmatch 'MIN_ARC_LENGTH' -or
    $path -notmatch 'arcLength' -or $path -notmatch '>=\s*MIN_ARC_LENGTH') {
    throw 'Spherical path does not enforce an arc length at least equal to the sphere diameter'
}
if ($renderer -notmatch 'headProgress' -or $renderer -notmatch 'tailProgress') {
    throw 'Flag spear renderer has no moving head/tail window'
}
if ($renderer -match 'progress\s*\*\s*Math\.PI\s*\*\s*2') {
    throw 'Flag spear renderer still contains a full-circle ring calculation'
}
if ($path -match 'wobble|side\.scale\(') {
    throw 'Flag spear spherical path is not a straight-segment great-circle path'
}
if ($renderer -notmatch 'FlagSpearRenderTypes\.glow\(\)' -or
    $renderTypes -notmatch 'ADDITIVE_TRANSPARENCY') {
    throw 'Flag spear rendering has no explicit additive glow pass'
}

Write-Output 'flag spear spherical trail contract: PASS'
