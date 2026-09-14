$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sculkRoot = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk'
$sensor = Get-Content -Raw (Join-Path $sculkRoot 'SculkSymbioteSensor.java')
$handler = Get-Content -Raw (Join-Path $sculkRoot 'SculkSymbioteServerHandler.java')
$config = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/Config.java')
$failures = @()

foreach ($check in @(
    '*implements VibrationSystem, VibrationSystem.User*',
    '*DynamicGameEventListener<VibrationSystem.Listener>*',
    '*new VibrationSystem.Listener(this)*',
    '*dynamicListener.add(level)*',
    '*dynamicListener.move(level)*',
    '*VibrationSystem.Ticker.tick*',
    '*GameEventTags.VIBRATIONS*',
    '*context.sourceEntity() != player*',
    '*SculkSymbioteVibrationPacket.sendTo*'
)) {
    if ($sensor -notlike $check -and $handler -notlike $check) { $failures += "missing server contract: $check" }
}

foreach ($check in @(
    '*SculkSymbioteVibrationRadius", 16*',
    '*SculkSymbioteExposurePerEvent", 10*',
    '*SculkSymbioteExposureMax", 100*',
    '*SculkSymbioteSonicBoomCooldown", 40*',
    '*SculkSymbioteMaxTrackedSources", 128*',
    '*SculkSymbioteMaxMarkersPerTick", 32*'
)) {
    if ($config -notlike $check) { $failures += "missing config contract: $check" }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote server contract: PASS'
