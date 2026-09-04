$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$itemPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java'
$eventsPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/event/KatanaEvents.java'

$itemText = Get-Content -Raw $itemPath
$eventsText = Get-Content -Raw $eventsPath
$failures = [System.Collections.Generic.List[string]]::new()

if ($itemText -match 'applyDirectHealthCost') {
    $failures.Add('MuramasaItem still contains the removed direct health cost')
}
if ($itemText -notmatch 'KatanaState\.addEnergy') {
    $failures.Add('MuramasaItem does not delegate energy changes to KatanaState')
}
if ($eventsText -notmatch 'KatanaState\.addEnergy\(stack, KatanaState\.BLOCK_ENERGY\)') {
    $failures.Add('KatanaEvents does not restore 10 energy for a successful block')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana contract: PASS'
