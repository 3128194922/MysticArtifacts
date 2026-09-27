$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts'
$assetRoot = Join-Path $projectRoot 'main/resources/assets/mysticartifacts'

function Read-Required([string] $relativePath) {
    $path = Join-Path $projectRoot $relativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing Death Scythe file: $relativePath"
    }
    return Get-Content -LiteralPath $path -Raw
}

function Require-Match([string] $source, [string] $pattern, [string] $label) {
    if ($source -notmatch $pattern) {
        throw "Death Scythe contract failed: $label"
    }
}

function Require-NoMatch([string] $source, [string] $pattern, [string] $label) {
    if ($source -match $pattern) {
        throw "Death Scythe contract failed: $label"
    }
}

function Remove-JavaComments([string] $source) {
    $pattern = '(?s)(?<literal>"(?:\\.|[^"\\])*"|''(?:\\.|[^''\\])*'')|(?<comment>/\*.*?\*/|//[^\r\n]*)'
    return [regex]::Replace($source, $pattern,
        [System.Text.RegularExpressions.MatchEvaluator] {
            param($match)
            if ($match.Groups['comment'].Success) { return ' ' }
            return $match.Value
        })
}

$items = Read-Required 'main/java/com/uniye/mysticartifacts/init/ModItems.java'
$tabs = Read-Required 'main/java/com/uniye/mysticartifacts/init/ModCreativeModTabs.java'
$config = Read-Required 'main/java/com/uniye/mysticartifacts/Config.java'
$item = Read-Required 'main/java/com/uniye/mysticartifacts/item/impl/DeathScytheItem.java'
$logic = Read-Required 'main/java/com/uniye/mysticartifacts/util/DeathScytheLogic.java'
$network = Read-Required 'main/java/com/uniye/mysticartifacts/network/NetworkHandler.java'
$packet = Read-Required 'main/java/com/uniye/mysticartifacts/network/DeathScytheSlashPacket.java'
$renderer = Read-Required 'main/java/com/uniye/mysticartifacts/client/render/DeathScytheRenderer.java'
$clientEvents = Read-Required 'main/java/com/uniye/mysticartifacts/client/ClientModEvents.java'

Require-Match $items 'DEATH_SCYTHE\s*=\s*ITEMS\.register\("death_scythe"' 'item registry ID'
Require-Match $items 'new DeathScytheItem\(new Item\.Properties\(\)\.stacksTo\(1\)\)' 'item registry factory'
Require-Match $tabs 'pOutput\.accept\(ModItems\.DEATH_SCYTHE\.get\(\)\)' 'creative tab entry'
Require-Match $config 'defineInRange\("DeathScytheEnergyTicks",\s*100,' 'energy config default'
Require-Match $config 'defineInRange\("DeathScytheRightClickCooldown",\s*20,' 'cooldown config default'
Require-Match $config 'DeathScytheEnergyTicks\s*=\s*DEATH_SCYTHE_ENERGY_TICKS\.get\(\)' 'energy config load'
Require-Match $config 'DeathScytheRightClickCooldown\s*=\s*DEATH_SCYTHE_RIGHT_CLICK_COOLDOWN\.get\(\)' 'cooldown config load'
Require-Match $logic 'ENERGY_TICKS\s*=\s*100\s*;' '100 tick energy logic'
Require-Match $logic 'RIGHT_CLICK_COOLDOWN_TICKS\s*=\s*20\s*;' '20 tick cooldown logic'
Write-Output 'PASS registry/config (death_scythe, 100, 20)'

foreach ($entry in @(
    @('TAG_TARGET_UUID', 'TargetUUID'),
    @('TAG_ENERGY_UNTIL', 'EnergyUntil'),
    @('TAG_SLASH_SEQUENCE', 'SlashSequence')
)) {
    Require-Match $item ("{0}\s*=\s*`"{1}`"" -f $entry[0], $entry[1]) "NBT key $($entry[1])"
}
Require-Match $item 'putUUID\(TAG_TARGET_UUID,\s*targetUUID\)' 'target UUID persistence'
Require-Match $item 'putLong\(TAG_ENERGY_UNTIL,\s*energyUntil\)' 'energy expiry persistence'
Require-Match $item 'tag\.remove\(TAG_TARGET_UUID\)' 'target cleanup'
Require-Match $item 'tag\.remove\(TAG_ENERGY_UNTIL\)' 'energy cleanup'
Require-Match $item 'tag\.remove\(TAG_SLASH_SEQUENCE\)' 'sequence cleanup'
Write-Output 'PASS NBT keys and cleanup'

Require-Match $item 'implements GeoItem' 'GeoItem implementation'
Require-Match $item 'GeoItem\.registerSyncedAnimatable\(this\)' 'synced GeoItem registration'
Require-Match $item 'triggerAnim\(player,' 'server Geo animation trigger'
Require-Match $renderer 'geo/death_scythe\.geo\.json' 'Geo model path'
Require-Match $renderer 'textures/item/death_scythe\.png' 'Geo texture path'
Require-Match $renderer 'animations/death_scythe\.animation\.json' 'Geo animation path'
Require-Match $clientEvents 'registerDeathScytheRenderer' 'client renderer registration'
Require-Match $network '(?s)DeathScytheSlashPacket::handle,\s*java\.util\.Optional\.of\(net\.minecraftforge\.network\.NetworkDirection\.PLAY_TO_CLIENT\)' 'slash packet direction'
Require-Match $packet 'DistExecutor\.unsafeRunWhenOn\(Dist\.CLIENT' 'packet client dispatch gate'
Require-Match $item 'if \(level\.isClientSide\)' 'logical client gate'
Require-Match $item 'level instanceof ServerLevel serverLevel' 'server level gate'
Require-Match $item 'target\.hurt\(level\.damageSources\(\)\.playerAttack\(player\),\s*damage\)' 'server attack damage'
Require-Match $item 'public boolean isDamageable\(ItemStack stack\)\s*\{\s*return false;' 'real durability disabled'
Require-NoMatch (Remove-JavaComments $item) '\b(hurtAndBreak|setDamageValue|damageItem)\s*\(' 'no durability consumption'
Write-Output 'PASS Geo/packet/client gate/no durability'

foreach ($relativePath in @(
    'main/java/com/uniye/mysticartifacts/item/impl/DeathScytheItem.java',
    'main/java/com/uniye/mysticartifacts/network/DeathScytheSlashPacket.java',
    'main/java/com/uniye/mysticartifacts/util/DeathScytheLogic.java'
)) {
    $code = Remove-JavaComments (Read-Required $relativePath)
    Require-NoMatch $code '\bnet\.minecraft\.client\b' "$relativePath imports/references client classes"
    Require-NoMatch $code '\bexample\b' "$relativePath contains example"
}
Write-Output 'PASS common Java comment-stripped client/example scan (3 files)'

$geo = Read-Required 'main/resources/assets/mysticartifacts/geo/death_scythe.geo.json' | ConvertFrom-Json
$animation = Read-Required 'main/resources/assets/mysticartifacts/animations/death_scythe.animation.json' | ConvertFrom-Json
$model = Read-Required 'main/resources/assets/mysticartifacts/models/item/death_scythe.json' | ConvertFrom-Json
$geometry = @($geo.'minecraft:geometry')
if ($geometry.Count -ne 1 -or $geometry[0].description.identifier -ne 'geometry.death_scythe') {
    throw 'Death Scythe contract failed: Geo identifier'
}
$boneNames = @($geometry[0].bones | ForEach-Object { $_.name })
foreach ($bone in @('root', 'handle', 'blade', 'energy_core', 'runes')) {
    if ($boneNames -cnotcontains $bone) { throw "Death Scythe contract failed: missing Geo bone $bone" }
}
if ($geometry[0].description.texture_width -ne 64 -or $geometry[0].description.texture_height -ne 64) {
    throw 'Death Scythe contract failed: Geo texture dimensions'
}
foreach ($name in @('animation.death_scythe.idle', 'animation.death_scythe.slash')) {
    if ($null -eq $animation.animations.PSObject.Properties[$name]) {
        throw "Death Scythe contract failed: missing animation $name"
    }
    foreach ($animatedBone in $animation.animations.PSObject.Properties[$name].Value.bones.PSObject.Properties.Name) {
        if ($boneNames -cnotcontains $animatedBone) {
            throw "Death Scythe contract failed: animation references missing bone $animatedBone"
        }
    }
}
if ($model.parent -ne 'builtin/entity' -or $model.textures.particle -ne 'mysticartifacts:item/death_scythe') {
    throw 'Death Scythe contract failed: item model resource'
}
$texturePath = Join-Path $assetRoot 'textures/item/death_scythe.png'
if (-not (Test-Path -LiteralPath $texturePath -PathType Leaf)) { throw 'Death Scythe contract failed: missing PNG' }
$png = [System.IO.File]::ReadAllBytes($texturePath)
if ($png.Length -lt 24 -or [System.BitConverter]::ToString($png[0..7]) -ne '89-50-4E-47-0D-0A-1A-0A') {
    throw 'Death Scythe contract failed: PNG signature'
}
$width = [System.Net.IPAddress]::NetworkToHostOrder([System.BitConverter]::ToInt32($png, 16))
$height = [System.Net.IPAddress]::NetworkToHostOrder([System.BitConverter]::ToInt32($png, 20))
if ($width -ne 64 -or $height -ne 64) { throw "Death Scythe contract failed: PNG ${width}x${height}" }
Write-Output "PASS JSON/Geo/animation/PNG (5 bones, ${width}x${height})"
