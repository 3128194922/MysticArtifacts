$ErrorActionPreference = 'Stop'

$rendererPath = Join-Path $PSScriptRoot '..\main\java\com\uniye\mysticartifacts\client\event\DeathEyeClientHandler.java'
$renderer = Get-Content -Raw $rendererPath

if ($renderer -notlike "*drawLine(poseStack, consumer, line.from(), progressEnd, 0.0f, 0.0f, 0.0f, 1.0f)*") {
    throw 'Black progress segment is missing.'
}

if ($renderer -notlike "*drawLine(poseStack, consumer, progressEnd, line.to(), 1.0f, 1.0f, 1.0f, 1.0f)*") {
    throw 'White remainder segment is missing.'
}

Write-Output 'PASS: Death Eye black progress is rendered as a non-overlapping segment'
