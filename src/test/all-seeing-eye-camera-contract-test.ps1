$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$serverEvents = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/event/AllSeeingEyeEvents.java'
$clientHandler = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/event/AllSeeingEyeClientHandler.java'
$statePacket = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/network/SpectateStatePacket.java'
$cameraEntity = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/camera/SpectateCameraEntity.java'

$serverText = Get-Content -Raw $serverEvents
$clientText = Get-Content -Raw $clientHandler

if ($serverText -match 'GameType\.SPECTATOR|changeGameModeForPlayer') {
    throw '占卜球观战不应切换玩家游戏模式。'
}
if ($serverText -match 'setCamera\(target\)') {
    throw '占卜球观战不应把目标玩家设置为原版相机实体。'
}
if (-not (Test-Path $statePacket)) {
    throw '缺少服务端到客户端的观战状态同步包。'
}
if (-not (Test-Path $cameraEntity)) {
    throw '缺少独立观战相机锚点实体。'
}
if ($clientText -notmatch 'RenderHandEvent') {
    throw '观战时需要隐藏观察者自己的第一人称手部。'
}
if ($clientText -notmatch 'SpectateCameraEntity') {
    throw '客户端需要使用观战相机锚点。'
}

Write-Output '占卜球观战相机契约检查通过。'
