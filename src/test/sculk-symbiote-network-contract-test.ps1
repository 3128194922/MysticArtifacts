$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$packet = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/network/SculkSymbioteVibrationPacket.java')
$sonicBoom = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSonicBoom.java')
$network = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/network/NetworkHandler.java')
$failures = @()

foreach ($check in @(
    '*writeVarInt*',
    '*writeDouble*',
    '*writeUUID*',
    '*readVarInt*',
    '*MAX_MARKERS*',
    '*PLAY_TO_CLIENT*',
    '*ParticleTypes.SONIC_BOOM*',
    '*SoundEvents.WARDEN_SONIC_BOOM*',
    '*damageSources().sonicBoom*',
    '*Attributes.KNOCKBACK_RESISTANCE*'
)) {
    if ($packet -notlike $check -and $sonicBoom -notlike $check -and $network -notlike $check) {
        $failures += "missing network contract: $check"
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote network contract: PASS'
