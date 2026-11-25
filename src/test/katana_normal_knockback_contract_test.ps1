$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$itemPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java'
$itemText = Get-Content -Raw $itemPath

if ($itemText -match 'CLOSED_KNOCKBACK_STRENGTH' -or
    $itemText -match 'target\.knockback\s*\(\s*CLOSED_KNOCKBACK_STRENGTH') {
    throw '普通形态仍在应用额外强击退'
}

if ($itemText -notmatch 'KatanaState\.addEnergy\(stack,\s*KatanaState\.CHARGE_PER_HIT\)') {
    throw '普通形态命中后的蓄力逻辑被意外移除'
}

Write-Output 'katana ordinary knockback contract: PASS'
