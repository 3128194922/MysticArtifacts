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
Assert-Condition ($airburstText -match 'int numPoints\s*=\s*17') 'Airburst split must use 17 scatter points'
Assert-Condition ($airburstText -match 'double fullness\s*=\s*0\.8') 'Airburst split must use scatter fullness 0.8'
Assert-Condition ($airburstText -match 'scale\(0\.35') 'Airburst split must use scatter speed scale 0.35'
Assert-Condition (-not ($airburstText -match 'Config\.AirBurstNumber\s*\+\s*this\.random\.nextInt')) 'Airburst split must not use the old random count'

Write-Output 'Contract check passed'
