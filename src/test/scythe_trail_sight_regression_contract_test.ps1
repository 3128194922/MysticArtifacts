$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $projectRoot
$outline = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/deathscythe/DeathScytheOutlineRenderer.java')
$mixin = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/mixin/EntityRenderDispatcherMixin.java')
$packet = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/network/SculkSymbioteVibrationPacket.java')
$serverHandler = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteServerHandler.java')
$sensor = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSensor.java')
$clientState = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteClientState.java')
$trailSight = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/TrailSightItem.java')
$soundSource = Get-Content -Raw (Join-Path $repoRoot 'docs/assets/death_scythe_zap_source.txt')
$failures = [System.Collections.Generic.List[string]]::new()

if ($outline -notmatch 'endOutlineBatch\(\)') {
    $failures.Add('death scythe outline must flush the vanilla outline batch')
}
if ($mixin -notmatch 'render\(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I\)V' -or
    $mixin -notmatch 'TrailSightItem\.isWearing') {
    $failures.Add('trail sight entity suppression must use the explicit dispatcher render descriptor and direct wear check')
}
if ($packet -notmatch 'exposure' -or $packet -notmatch 'exposureMaximum' -or
    $packet -notmatch 'writeVarInt\(message\.exposure') {
    $failures.Add('sculk vibration packet must synchronize exposure progress')
}
if ($serverHandler -notmatch 'SculkSymbioteVibrationPacket\.sendTo\(player, markers, sensor\.maxExposure\(\)') {
    $failures.Add('server handler must send sculk exposure progress with every sync tick')
}
if ($sensor -notmatch 'maxExposure\(' -or $sensor -notmatch 'exposure\.value\(\)') {
    $failures.Add('sculk sensor must expose the highest tracked source progress')
}
if ($clientState -notmatch 'setExposure\(' -or $clientState -notmatch 'exposureValue\(') {
    $failures.Add('client sculk state must retain synchronized exposure progress')
}
if ($trailSight -notmatch 'isBarVisible' -or $trailSight -notmatch 'getBarWidth' -or
    $trailSight -notmatch 'SculkSymbioteClientState') {
    $failures.Add('trail sight must display the synchronized exposure as an item bar')
}
if ($soundSource -notmatch 'CC0' -or $soundSource -notmatch 'death') {
    $failures.Add('death scythe sound source must document a death-themed CC0 asset')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'scythe and trail sight regression contract: PASS'
