$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$sourceRoot = Join-Path $projectRoot 'src\main'

function Assert-Condition([bool]$condition, [string]$message) {
    if (-not $condition) {
        throw "Contract check failed: $message"
    }
}

Assert-Condition (-not (Test-Path (Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\util\ParticleTextAPI.java'))) 'ParticleTextAPI must be deleted'

$javaFiles = Get-ChildItem (Join-Path $sourceRoot 'java') -Recurse -Filter '*.java'
$javaText = ($javaFiles | Get-Content -Raw) -join "`n"
Assert-Condition (-not ($javaText -match 'ParticleTextAPI|SpawnTextIndicatorPacket|TextIndicatorParticle')) 'text particle chain must be absent'

$katanaEvent = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\event\KatanaBlockEvent.java'
$kubejsEvent = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\kubejs\KatanaKubeEvents.java'
$kubejsPlugin = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\kubejs\KubeJSKatanaPlugin.java'
$kubejsPayload = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\kubejs\KatanaBlockEventJS.java'

Assert-Condition (Test-Path $katanaEvent) 'Katana Forge event class is missing'
Assert-Condition (Test-Path $kubejsEvent) 'KatanaEvents KubeJS event group is missing'
Assert-Condition (Test-Path $kubejsPlugin) 'KubeJS plugin entrypoint is missing'
Assert-Condition (Test-Path $kubejsPayload) 'KubeJS event payload is missing'

$eventText = Get-Content -Raw $katanaEvent
$eventGroupText = Get-Content -Raw $kubejsEvent
$pluginText = Get-Content -Raw $kubejsPlugin
$payloadText = Get-Content -Raw $kubejsPayload
$pluginList = Get-Content -Raw (Join-Path $sourceRoot 'resources\kubejs.plugins.txt')

Assert-Condition ($eventText -match 'boolean.*perfect|isPerfect') 'Forge event must carry perfect-block flag'
Assert-Condition ($eventGroupText -match 'blocked') 'KubeJS event group must expose blocked callback'
Assert-Condition ($pluginText -match 'extends KubeJSPlugin') 'KubeJS plugin must extend KubeJSPlugin'
Assert-Condition ($payloadText -match 'getPlayer|getBlocker|getAttacker') 'KubeJS payload must expose entity fields'
Assert-Condition ($pluginList -match 'KubeJSKatanaPlugin') 'KubeJS plugin entrypoint is not registered'

$airburstEntity = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\entity\AirburstArrowEntity.java'
$airburstText = Get-Content -Raw $airburstEntity
Assert-Condition ($airburstText -match 'int count\s*=\s*Config\.AirBurstNumber\s*\+\s*this\.random\.nextInt\(Config\.AirBurstNumberRandom\)') 'Airburst split must use the configured 12 plus random 0-7 count'
Assert-Condition ($airburstText -match 'ScatterArrowDirection\.create\(this\.random,\s*i,\s*count\)') 'Airburst split must use shared scatter directions'

$explodingEntity = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\entity\ExplodingArrowEntity.java'
$explodingText = Get-Content -Raw $explodingEntity
Assert-Condition ($explodingText -match 'int count\s*=\s*Config\.AirBurstNumber2\s*\+\s*this\.random\.nextInt\(Config\.AirBurstNumber2Random\s*\+\s*1\)') 'Airburst I split must use the configured 3-6 count'
Assert-Condition ($explodingText -match 'ScatterArrowDirection\.create\(this\.random,\s*i,\s*count\)') 'Airburst I split must use shared scatter directions'

$scatterDirection = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\entity\ScatterArrowDirection.java'
$scatterText = Get-Content -Raw $scatterDirection
Assert-Condition ($scatterText -match 'double fullness\s*=\s*0\.8') 'Shared scatter direction must use fullness 0.8'
Assert-Condition ($scatterText -match 'turnFraction') 'Shared scatter direction must use golden-ratio distribution'
Assert-Condition ($scatterText -match 'add\(0\.0,\s*1\.0,\s*0\.0\)\.scale\(0\.5\)') 'Shared scatter direction must raise the first direction'
Assert-Condition ($scatterText -match 'scale\(0\.35\)') 'Shared scatter direction must use speed scale 0.35'

$finalExplodingEntity = Join-Path $sourceRoot 'java\com\uniye\mysticartifacts\entity\FinalExplodingArrowEntity.java'
$finalExplodingText = Get-Content -Raw $finalExplodingEntity
Assert-Condition (-not ($finalExplodingText -match 'addFreshEntity')) 'Airburst II must not scatter child entities'

Write-Output 'Contract check passed'
