$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$itemPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java'
$itemText = Get-Content -Raw $itemPath

if ($itemText -match 'applyForgeHandTransform') {
    throw 'MuramasaItem still overrides Forge first-person hand transforms and can bypass the vanilla sword attack animation'
}

if ($itemText -notmatch 'getCustomRenderer') {
    throw 'MuramasaItem lost its custom Geo item renderer registration'
}

Write-Output 'katana first-person attack contract: PASS'
