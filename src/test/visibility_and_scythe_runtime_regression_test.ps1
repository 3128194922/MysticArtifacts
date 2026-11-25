$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts'

function Read-Required([string] $relativePath) {
    $path = Join-Path $projectRoot $relativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing runtime regression file: $relativePath"
    }
    return Get-Content -LiteralPath $path -Raw
}

$visibility = Read-Required 'main/java/com/uniye/mysticartifacts/client/sculk/SculkEntityVisibilityHandler.java'
$mixin = Read-Required 'main/java/com/uniye/mysticartifacts/mixin/EntityRenderDispatcherMixin.java'
$config = Read-Required 'main/java/com/uniye/mysticartifacts/Config.java'
$outline = Read-Required 'main/java/com/uniye/mysticartifacts/client/deathscythe/DeathScytheOutlineRenderer.java'
$item = Read-Required 'main/java/com/uniye/mysticartifacts/item/impl/DeathScytheItem.java'
$network = Read-Required 'main/java/com/uniye/mysticartifacts/network/NetworkHandler.java'
$targetPacket = Read-Required 'main/java/com/uniye/mysticartifacts/network/DeathScytheTargetPacket.java'
$clientState = Read-Required 'main/java/com/uniye/mysticartifacts/client/deathscythe/DeathScytheTargetClientState.java'

if ($visibility -notmatch 'RenderLivingEvent\.Pre' -or
    $visibility -notmatch 'event\.setCanceled\(true\)' -or
    $visibility -notmatch 'RenderPlayerEvent\.Pre' -or
    $visibility -notmatch 'RenderNameTagEvent' -or
    $visibility -notmatch 'event\.setResult\(Event\.Result\.DENY\)' -or
    $visibility -match 'onRenderNameTag[\s\S]*event\.setCanceled\(true\)') {
    throw 'Trail Sight must cancel living/player renders and deny name-tag rendering without canceling RenderNameTagEvent'
}
if ($mixin -notmatch 'TrailSightItem\.isWearing\(minecraft\.player\)' -or
    $mixin -notmatch 'SculkSymbioteClientState\.isActive\(\)') {
    throw 'Entity dispatcher suppression must retain both active-state and direct wear checks'
}
if ($config -notmatch 'event\.getConfig\(\)\.getSpec\(\)\s*!=\s*SPEC\s*\)\s*\{\s*return') {
    throw 'Common config loader must ignore the client config event'
}
if ($outline -notmatch 'event\.getCamera\(\)\.getPosition\(\)' -or
    $outline -notmatch 'poseStack\.translate\(-camera\.x,\s*-camera\.y,\s*-camera\.z\)' -or
    $outline -notmatch 'target\.getPosition\(event\.getPartialTick\(\)\)' -or
    $outline -notmatch 'DeathScytheTargetClientState') {
    throw 'Death Scythe outline must use camera-relative interpolated target rendering and client target state'
}
if ($item -notmatch 'DeathScytheTargetPacket' -or
    $item -notmatch 'DeathScytheTargetPacket\.clear|new DeathScytheTargetPacket') {
    throw 'Death Scythe must synchronize target acquisition and clearing to the client'
}
if ($network -notmatch 'DeathScytheTargetPacket::handle') {
    throw 'Death Scythe target packet must be registered'
}
if ($network -notmatch '(?s)DeathScytheTargetPacket::handle,\s*java\.util\.Optional\.of\(net\.minecraftforge\.network\.NetworkDirection\.PLAY_TO_CLIENT\)' -or
    $targetPacket -notmatch 'writeUUID|writeBoolean' -or
    $targetPacket -notmatch 'DistExecutor\.unsafeRunWhenOn\(Dist\.CLIENT') {
    throw 'Death Scythe target packet must be server-to-client and client-gated'
}
if ($clientState -notmatch 'setTarget' -or $clientState -notmatch 'UUID') {
    throw 'Death Scythe client state must retain the synchronized target UUID'
}

Write-Output 'visibility and scythe runtime regression contract: PASS'
