$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$modelPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/model/FlagSpearModel.java'
$rendererPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/render/FlagSpearItemRenderer.java'

$modelText = Get-Content -Raw $modelPath
$rendererText = Get-Content -Raw $rendererPath

if ($modelText -notmatch 'animateBanner\s*\(') {
    throw 'Flag spear model has no banner animation entry point'
}
if ($modelText -notmatch 'Mth\.sin') {
    throw 'Flag spear banner animation is not time-based'
}
if ($modelText -notmatch 'gold_hem_bottom_1_r1' -or
    $modelText -notmatch 'gold_hem_bottom_2_r1' -or
    $modelText -notmatch 'gold_hem_bottom_3_r1' -or
    $modelText -notmatch 'gold_hem_bottom_4_r1') {
    throw 'Flag spear banner animation does not address segmented flag parts'
}
if ($rendererText -notmatch 'animateBanner') {
    throw 'Flag spear item renderer does not drive banner animation'
}
if ($rendererText -notmatch 'Util\.getMillis') {
    throw 'Flag spear item renderer has no continuous client animation clock'
}

Write-Output 'flag spear flag animation contract: PASS'
