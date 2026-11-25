$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$eventPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/event/LightningBottleEvents.java'
$soundsPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/init/ModSounds.java'
$soundsJsonPath = Join-Path $projectRoot 'src/main/resources/assets/mysticartifacts/sounds.json'
$assetPath = Join-Path $projectRoot 'src/main/resources/assets/mysticartifacts/sounds/lightning_bottle_zap.ogg'
$eventText = Get-Content -Raw $eventPath
$soundsText = Get-Content -Raw $soundsPath
$soundsJson = Get-Content -Raw $soundsJsonPath

if ($eventText -notmatch 'ModSounds\.LIGHTNING_BOTTLE_ZAP\.get\(\)') {
    throw 'Lightning bottle does not use the downloaded electric zap sound'
}
if ($eventText -notmatch 'SoundSource\.PLAYERS') {
    throw 'Lightning bottle sound is not routed through the player sound source'
}
if ($eventText -notmatch '\.playSound\(') {
    throw 'Lightning bottle trigger does not play a sound'
}
if ($soundsText -notmatch 'LIGHTNING_BOTTLE_ZAP' -or
    $soundsText -notmatch 'entity\.lightning_bottle_zap') {
    throw 'Lightning bottle zap sound is not registered'
}
if ($soundsJson -notmatch 'entity\.lightning_bottle_zap' -or
    $soundsJson -notmatch 'mysticartifacts:lightning_bottle_zap') {
    throw 'Lightning bottle zap sound is missing from sounds.json'
}
if (-not (Test-Path $assetPath)) {
    throw 'Downloaded lightning bottle zap OGG asset is missing'
}

Write-Output 'lightning bottle sound contract: PASS'
