$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$mainPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java'
$mainText = Get-Content -Raw $mainPath

if ($mainText -match 'net\.minecraft\.client') {
    throw 'Main mod entry still references client-only classes.'
}

$requiredSources = @(
    'src/main/java/com/uniye/mysticartifacts/init/ModItems.java',
    'src/main/java/com/uniye/mysticartifacts/init/ModEntities.java',
    'src/main/java/com/uniye/mysticartifacts/init/ModSounds.java',
    'src/main/java/com/uniye/mysticartifacts/revolver/RevolverRegistries.java'
)

foreach ($relativePath in $requiredSources) {
    $absolutePath = Join-Path $projectRoot $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath)) {
        throw "Required registration source is missing: $relativePath"
    }
}

$javaFiles = Get-ChildItem (Join-Path $projectRoot 'src/main/java') -Recurse -Filter '*.java'
$exampleMatches = $javaFiles | Select-String -Pattern 'package\s+[^;]*example' -CaseSensitive
if ($exampleMatches) {
    throw 'Java package path contains forbidden example token.'
}

$registeredIds = @('jiba_revolver', 'revolver_bullet', 'revolver_cylinder', 'mysticartifacts')
foreach ($id in $registeredIds) {
    $matches = $javaFiles | Select-String -Pattern [regex]::Escape($id) -CaseSensitive
    if (-not $matches) {
        throw "Required registration/resource id is missing: $id"
    }
}

Write-Output 'Project structure contract passed.'
