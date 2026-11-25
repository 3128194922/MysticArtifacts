$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mainRoot = Join-Path $projectRoot 'src/main'

$localPlayerMixin = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/mixin/LocalPlayerMixin.java')
$spearEvents = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/event/SpearEvents.java')

if ($localPlayerMixin -notmatch 'method = "aiStep"') {
    throw 'Spear use slowdown bypass is missing from LocalPlayer.aiStep'
}
if ($localPlayerMixin -notmatch 'method = "canStartSprinting"') {
    throw 'Spear sprint start bypass is missing from LocalPlayer.canStartSprinting'
}
if (([regex]::Matches($localPlayerMixin, 'getUseItem\(\)\.getItem\(\) instanceof SpearItem')).Count -lt 2) {
    throw 'Both client sprint redirects must be limited to SpearItem'
}
if ($spearEvents -notmatch 'TickEvent\.PlayerTickEvent') {
    throw 'Server-side spear speed tick handler is missing'
}
if ($spearEvents -notmatch 'Attributes\.MOVEMENT_SPEED') {
    throw 'Spear speed handler does not modify movement speed'
}
if ($spearEvents -notmatch 'addTransientModifier' -or $spearEvents -notmatch 'removeModifier') {
    throw 'Spear speed modifier must be added and removed with the use state'
}
if ($spearEvents -notmatch 'AttributeModifier\.Operation\.MULTIPLY_TOTAL') {
    throw 'Spear speed modifier should use a multiplicative movement bonus'
}

Write-Output 'spear sprint contract: PASS'
