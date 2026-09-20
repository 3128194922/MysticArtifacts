$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mainRoot = Join-Path $projectRoot 'src/main'

$requiredFiles = @(
    'java/com/uniye/mysticartifacts/item/impl/FlagSpearItem.java',
    'java/com/uniye/mysticartifacts/event/FlagSpearEvents.java',
    'java/com/uniye/mysticartifacts/network/FlagSpearTrailPacket.java',
    'java/com/uniye/mysticartifacts/client/flag/FlagSpearClientState.java',
    'java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderer.java',
    'java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderTypes.java',
    'resources/assets/mysticartifacts/models/item/flag_spear.json'
)

foreach ($relativePath in $requiredFiles) {
    if (-not (Test-Path (Join-Path $mainRoot $relativePath))) {
        throw "Missing flag spear file: $relativePath"
    }
}

$itemsText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/init/ModItems.java')
$creativeTabText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/init/ModCreativeModTabs.java')
$eventText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/event/FlagSpearEvents.java')
$packetText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/network/FlagSpearTrailPacket.java')
$networkText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/network/NetworkHandler.java')
$handlerText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java')
$stateText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/flag/FlagSpearClientState.java')
$rendererText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderer.java')
$renderTypesText = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderTypes.java')
$modelText = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/flag_spear.json')
$zhText = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/lang/zh_cn.json')
$enText = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/lang/en_us.json')

if ($itemsText -notmatch 'FLAG_SPEAR\s*=\s*ITEMS\.register\("flag_spear"') {
    throw 'Flag spear is not registered as flag_spear'
}
if ($itemsText -notmatch 'new\s+FlagSpearItem') {
    throw 'Flag spear does not use FlagSpearItem'
}
if ($creativeTabText -notmatch 'ModItems\.FLAG_SPEAR\.get\(\)') {
    throw 'Flag spear is missing from the creative tab'
}
if ($eventText -notmatch 'AttackEntityEvent' -or $eventText -notmatch 'ServerPlayer') {
    throw 'Flag spear server attack event is missing'
}
if ($eventText -notmatch 'getMainHandItem\(\)\.is\(ModItems\.FLAG_SPEAR\.get\(\)\)') {
    throw 'Flag spear event does not check the main-hand item'
}
if ($eventText -notmatch 'getBoundingBox\(\)\.inflate\(5\.0D\)') {
    throw 'Flag spear event does not use the five-block search radius'
}
if ($eventText -notmatch 'candidate\s*!=\s*primaryTarget') {
    throw 'Flag spear event does not exclude the primary target'
}
if ($eventText -notmatch 'damageSources\(\)\.playerAttack\(player\)') {
    throw 'Flag spear event does not use player attack damage source'
}
if ($eventText -notmatch 'FlagSpearTrailPacket\.send') {
    throw 'Flag spear event does not send the trail packet'
}
if ($packetText -notmatch 'writeDouble' -or $packetText -notmatch 'writeFloat' -or $packetText -notmatch 'DistExecutor') {
    throw 'Flag spear packet does not encode and route client-side safely'
}
if ($packetText -notmatch 'PacketDistributor\.NEAR') {
    throw 'Flag spear packet does not use nearby-client distribution'
}
if ($networkText -notmatch 'FlagSpearTrailPacket\.class') {
    throw 'Flag spear packet is not registered'
}
if ($handlerText -notmatch 'handleFlagSpearTrail') {
    throw 'Client packet handler does not expose flag spear trail handling'
}
if ($stateText -notmatch 'LIFETIME_TICKS\s*=\s*10L' -or $stateText -notmatch 'MAX_TRAILS\s*=\s*32') {
    throw 'Flag spear client state limits are missing'
}
if ($rendererText -notmatch 'AFTER_ENTITIES' -or $rendererText -notmatch 'TRAIL_COUNT') {
    throw 'Flag spear renderer does not render five trails after entities'
}
if ($renderTypesText -notmatch 'NO_DEPTH_TEST' -or $renderTypesText -notmatch 'NO_CULL') {
    throw 'Flag spear render type is not configured for visible emissive trails'
}
if ($renderTypesText -notmatch '1\.0F,\s*0\.0F,\s*0\.0F') {
    throw 'Flag spear render type is not red'
}
if ($modelText -notmatch 'mysticartifacts:item/spear') {
    throw 'Flag spear model does not reuse the spear texture'
}
if ($zhText -notmatch 'item\.mysticartifacts\.flag_spear.*旗枪') {
    throw 'Missing Chinese flag spear translation'
}
if ($enText -notmatch 'item\.mysticartifacts\.flag_spear.*Flag Spear') {
    throw 'Missing English flag spear translation'
}

Write-Output 'flag spear contract: PASS'
