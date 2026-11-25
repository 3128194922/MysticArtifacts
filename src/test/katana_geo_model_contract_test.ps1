$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$geoPath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/geo/muramasa.geo.json'
$texturePath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/textures/item/muramasa.png'
$itemPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java'
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/MuramasaRenderer.java'
$animationPath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/animations/muramasa.animation.json'
$itemModelPath = Join-Path $projectRoot 'main/resources/assets/mysticartifacts/models/item/katana.json'

foreach ($path in @($geoPath, $texturePath, $itemPath, $rendererPath, $animationPath, $itemModelPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing required Muramasa Geo resource: $path"
    }
}

$geo = Get-Content -Raw $geoPath | ConvertFrom-Json
$identifier = $geo.'minecraft:geometry'[0].description.identifier
if ($identifier -ne 'geometry.blood_demon_katana') {
    throw "Unexpected Geo identifier: $identifier"
}

$boneNames = @($geo.'minecraft:geometry'[0].bones | ForEach-Object { $_.name })
foreach ($bone in @('normal_form', 'demon_form', 'demon_ornaments')) {
    if ($boneNames -notcontains $bone) {
        throw "Missing form bone: $bone"
    }
}

Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::FromFile($texturePath)
try {
    if ($bitmap.Width -ne 32 -or $bitmap.Height -ne 32) {
        throw "Muramasa texture must be 32x32 but is $($bitmap.Width)x$($bitmap.Height)"
    }
}
finally {
    $bitmap.Dispose()
}

$itemText = Get-Content -Raw $itemPath
$rendererText = Get-Content -Raw $rendererPath
$itemModel = Get-Content -Raw $itemModelPath | ConvertFrom-Json
$animation = Get-Content -Raw $animationPath | ConvertFrom-Json
if ($itemText -notmatch 'implements GeoItem' -or $itemText -notmatch 'GeoItem\.registerSyncedAnimatable') {
    throw 'MuramasaItem is not registered as a GeckoLib GeoItem'
}
if ($itemModel.parent -ne 'builtin/entity') {
    throw 'Katana item model must use builtin/entity to invoke the Geo item renderer'
}
foreach ($token in @('normal_form', 'demon_form', 'KatanaState\.isOpen')) {
    if ($rendererText -notmatch $token) {
        throw "MuramasaRenderer is missing form-state token: $token"
    }
}
foreach ($animationName in @('animation.muramasa.normal', 'animation.muramasa.demon')) {
    if ($null -eq $animation.animations.$animationName) {
        throw "Missing animation resource: $animationName"
    }
}
if ($geo | ConvertTo-Json -Depth 100 | Select-String -Pattern 'example') {
    throw 'Geo model contains forbidden example namespace/token'
}

Write-Output 'katana Geo model contract: PASS'
