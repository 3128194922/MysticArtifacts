$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mainRoot = Join-Path $projectRoot 'src/main'

$clientEvents = Get-Content -Raw (Join-Path $mainRoot 'java/com/uniye/mysticartifacts/client/ClientModEvents.java')
$spearModel = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/spear.json')
$grieferModel = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/griefer_spear.json')
$zombiePose = Get-Content -Raw (Join-Path $mainRoot 'resources/assets/mysticartifacts/models/item/custom/spear_zombie.json')

foreach ($text in @($clientEvents, $spearModel, $grieferModel, $zombiePose)) {
    if (-not $text) {
        throw 'Missing zombie spear pose source or model'
    }
}

if ($clientEvents -notmatch 'new ResourceLocation\(MODID, "zombie_holding"\)') {
    throw 'zombie_holding item property is not registered'
}
if ($clientEvents -notmatch 'holder instanceof Zombie && !\(holder instanceof ZombifiedPiglin\)') {
    throw 'zombie_holding must target Zombie models without hijacking zombified piglins'
}
if (([regex]::Matches($clientEvents, 'zombie_holding')).Count -lt 2) {
    throw 'zombie_holding must be registered for both spear items'
}

foreach ($model in @($spearModel, $grieferModel)) {
    $zombieIndex = $model.IndexOf('mysticartifacts:zombie_holding')
    $mobIndex = $model.IndexOf('mysticartifacts:mob_holding')
    if ($zombieIndex -lt 0 -or $mobIndex -lt 0 -or $zombieIndex -gt $mobIndex) {
        throw 'zombie_holding override must be listed before mob_holding'
    }
}

if ($zombiePose -notmatch '"thirdperson_righthand"\s*:\s*\{\s*"rotation"\s*:\s*\[\s*-45\s*,\s*-90\s*,\s*0\s*\]') {
    throw 'Right-hand zombie spear is not aligned with the forward arm'
}
if ($zombiePose -notmatch '"thirdperson_lefthand"\s*:\s*\{\s*"rotation"\s*:\s*\[\s*-45\s*,\s*90\s*,\s*0\s*\]') {
    throw 'Left-hand zombie spear is not mirrored with the forward arm'
}

Write-Output 'zombie spear pose contract: PASS'
