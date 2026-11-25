$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$mixinConfigPath = Join-Path $projectRoot 'main/resources/mysticartifacts.mixins.json'
$mixinPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/mixin/EntityRenderDispatcherMixin.java'
$serverPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteServerHandler.java'
$sensorPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSensor.java'
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteRenderer.java'

$mixinConfig = Get-Content -Raw $mixinConfigPath
$mixin = if (Test-Path $mixinPath) { Get-Content -Raw $mixinPath } else { '' }
$server = Get-Content -Raw $serverPath
$sensor = Get-Content -Raw $sensorPath
$renderer = Get-Content -Raw $rendererPath
$failures = [System.Collections.Generic.List[string]]::new()

if ($mixinConfig -notmatch 'EntityRenderDispatcherMixin') {
    $failures.Add('Client mixin configuration does not register the entity render suppression mixin')
}
if ($mixin -notmatch '@Mixin\(EntityRenderDispatcher\.class\)' -or
    $mixin -notmatch 'method\s*=\s*"render(?:\(|")' -or
    $mixin -notmatch 'SculkSymbioteClientState\.isActive\(\)' -or
    $mixin -notmatch 'ci\.cancel\(\)') {
    $failures.Add('Entity render suppression does not cancel the dispatcher render path while active')
}
if ($server -match 'refreshEffect\(player' -or $server -match 'MobEffects\.(BLINDNESS|DARKNESS)') {
    $failures.Add('Sculk Symbiote still applies the old blindness/darkness visual restriction')
}
if ($server -notmatch 'PlayLevelSoundEvent\.AtEntity' -or
    $server -notmatch 'onEntitySound') {
    $failures.Add('Entity sound events are not forwarded to Sculk Symbiote markers')
}
if ($sensor -notmatch 'recordEntitySound') {
    $failures.Add('Sculk sensor has no direct entity sound marker path')
}
if ($sensor -notmatch 'markerSource\.position\(\)' -or
    $sensor -notmatch 'markerSource\.getUUID\(\)') {
    $failures.Add('Sculk vibration markers are not anchored to the emitting entity')
}
if ($renderer -notmatch 'NO_DEPTH_TEST_FUNCTION' -or
    $renderer -notmatch 'SCULK_MARKER_RENDER_TYPE') {
    $failures.Add('Sculk markers no longer use the dedicated through-wall render type')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'sculk symbiote client render contract: PASS'
