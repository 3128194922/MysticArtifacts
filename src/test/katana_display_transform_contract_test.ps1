$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$modelPath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/models/item/katana.json'
$model = Get-Content -Raw $modelPath | ConvertFrom-Json

if ($model.parent -ne 'builtin/entity') {
    throw 'Katana item model must continue using builtin/entity'
}

$expected = @{
    thirdperson_righthand = @{ rotation = @(0, -90, 0); translation = @(0, 1.25, 1.75) }
    thirdperson_lefthand  = @{ rotation = @(0, 90, 0); translation = @(0, 1.25, 1.75) }
    firstperson_righthand = @{ rotation = @(0, -90, 0) }
    firstperson_lefthand  = @{ rotation = @(0, 90, 0) }
    ground                = @{ translation = @(0, 7, 0) }
    gui                   = @{ rotation = @(0, 0, -45); translation = @(-5, -4.5, 0) }
    fixed                 = @{ rotation = @(0, 0, 45); translation = @(5.25, -6.25, 0) }
}

foreach ($displayName in $expected.Keys) {
    $actual = $model.display.$displayName
    if ($null -eq $actual) {
        throw "Katana model is missing display transform: $displayName"
    }

    foreach ($propertyName in $expected[$displayName].Keys) {
        $actualValues = @($actual.$propertyName)
        $expectedValues = @($expected[$displayName][$propertyName])
        if (($actualValues -join ',') -ne ($expectedValues -join ',')) {
            throw "Unexpected $displayName.$propertyName transform"
        }
    }
}

Write-Output 'katana display transform contract: PASS'
