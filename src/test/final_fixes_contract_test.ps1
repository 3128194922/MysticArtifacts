#Requires -Version 7.0
param([string]$BaselineRef = 'dc96968')

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$javaRoot = Join-Path $repoRoot 'src/main/java/com/uniye/mysticartifacts'
$renderer = Get-Content -Raw (Join-Path $javaRoot 'client/render/SwordSwarmCharmRenderer.java')
$charm = Get-Content -Raw (Join-Path $javaRoot 'item/impl/SwordSwarmCharm.java')
$phantom = Get-Content -Raw (Join-Path $javaRoot 'entity/SwordPhantomEntity.java')
$spirit = Get-Content -Raw (Join-Path $javaRoot 'entity/ArtifactSpiritEntity.java')
$failures = [System.Collections.Generic.List[string]]::new()
$script:passed = 0

function Assert-Contract([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Check([string]$Name, [scriptblock]$Body) {
    try {
        & $Body
        $script:passed++
        Write-Output "PASS $Name"
    } catch {
        $failures.Add($Name)
        Write-Output "FAIL ${Name}: $($_.Exception.Message)"
    }
}

function Get-Method([string]$Source, [string]$Name) {
    $match = [regex]::Match($Source, "(?m)^    (?:public|private|protected)[^\r\n]*\b$Name\([^\r\n]*\)\s*\{")
    if (-not $match.Success) { throw "Missing method: $Name" }
    $start = $Source.IndexOf('{', $match.Index)
    $depth = 0
    for ($i = $start; $i -lt $Source.Length; $i++) {
        if ($Source[$i] -eq '{') { $depth++ }
        if ($Source[$i] -eq '}') { $depth-- }
        if ($depth -eq 0) { return $Source.Substring($start, $i - $start + 1).Replace("`r`n", "`n") }
    }
    throw "Unclosed method: $Name"
}

function Get-Baseline([string]$Path) {
    $lines = & git -C $repoRoot show "${BaselineRef}:$Path"
    if ($LASTEXITCODE -ne 0) { throw "Cannot read baseline: $Path" }
    return $lines -join "`n"
}

Check 'I1 single Curios animation clock' {
    Assert-Contract ($renderer -match 'float animationTime = ageInTicks;') 'Curios ageInTicks must be used directly'
    Assert-Contract ($renderer -notmatch 'ageInTicks\s*\+\s*partialTicks|animationTime\s*\+\s*partialTicks') 'Duplicate partial tick interpolation'
    $rendererCode = $renderer -replace '(?m)//[^\r\n]*', ''
    Assert-Contract ([regex]::Matches($rendererCode, '\bageInTicks\b').Count -eq 2) 'All animation consumers must share animationTime'
    Assert-Contract ($renderer -match 'Mth.rotLerp\(partialTicks, entity.yBodyRotO, entity.yBodyRot\)') 'Body yaw interpolation must remain'
}

Check 'I2 queue seeded on server before regeneration early returns' {
    $tick = Get-Method $charm 'curioTick'
    Assert-Contract ($tick -match '(?s)if \(entity == null \|\| entity.level\(\).isClientSide\) return;.*seedQueue\(stack, entity.level\(\)\);') 'Missing server equipment initialization'
    $seed = $tick.IndexOf('seedQueue(stack, entity.level());')
    Assert-Contract ($seed -ge 0 -and $seed -lt $tick.IndexOf('if (current >= max)') -and $seed -lt $tick.IndexOf('if (timer > 0)')) 'Full/waiting charms must also seed the queue'
    $seedMethod = Get-Method $charm 'seedQueue'
    Assert-Contract ($seedMethod -match '(?s)contains\("DisplayQueue", Tag.TAG_LIST\).*?isEmpty\(\)\)\s*\{\s*return;') 'Existing nonempty queue must remain unchanged'
}

Check 'I2 renderer and queue getters stay read-only' {
    Assert-Contract ($renderer -notmatch 'getOrCreateTag|seedQueue|popNextAndAppendRandom|\.put\("DisplayQueue"') 'Renderer mutates charm state'
    foreach ($name in @('getDisplayQueue', 'getDevouredList', 'getStoredSwords')) {
        Assert-Contract ((Get-Method $charm $name) -notmatch 'getOrCreateTag|seedQueue|\.put\(') "$name mutates charm state"
    }
}

Check 'I3 every entity hit permanently stops server view tracking' {
    $tick = Get-Method $phantom 'tick'
    $hit = Get-Method $phantom 'onHitEntity'
    Assert-Contract ($phantom -match 'private boolean stoppedTracking;') 'Missing stopped-tracking state'
    Assert-Contract ($tick -match '!this.level\(\).isClientSide && !this.inGround && !this.isRemoved\(\) && !this.stoppedTracking') 'Tracking lacks stopped state guard'
    Assert-Contract ($hit.IndexOf('this.stoppedTracking = true;') -ge 0 -and $hit.IndexOf('this.stoppedTracking = true;') -lt $hit.IndexOf('target.hurt(')) 'Rejected hits must also stop tracking'
    Assert-Contract ($tick -match 'owner instanceof Player player' -and $tick -match 'lerp\(target, 0.15D\)' -and $tick -match 'direction.scale\(current.length\(\)\)') 'Existing tracking owner/smoothing/speed changed'
    Assert-Contract ((Get-Method $phantom 'addAdditionalSaveData') -match 'super.addAdditionalSaveData\(tag\);[\s\S]*putBoolean\("StoppedTracking", this.stoppedTracking\)') 'Stopped tracking must survive saving'
    Assert-Contract ((Get-Method $phantom 'readAdditionalSaveData') -match 'super.readAdditionalSaveData\(tag\);[\s\S]*this.stoppedTracking = tag.getBoolean\("StoppedTracking"\)') 'Stopped tracking must survive loading'
}

Check 'I3 accepted-hit and rejected-hit bounce logic preserved' {
    $before = Get-Method (Get-Baseline 'src/main/java/com/uniye/mysticartifacts/entity/SwordPhantomEntity.java') 'onHitEntity'
    $after = (Get-Method $phantom 'onHitEntity') -replace '(?m)^\s*this.stoppedTracking = true;\n', ''
    Assert-Contract ($after -eq $before) 'Hit logic changed beyond stoppedTracking assignment'
}

Check 'I4 strict JSON parsing of every item model' {
    $models = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'src/main/resources/assets/mysticartifacts/models/item') -Filter '*.json' -Recurse -File)
    foreach ($model in $models) {
        $document = [System.Text.Json.JsonDocument]::Parse([string](Get-Content -Raw -LiteralPath $model.FullName))
        $document.Dispose()
    }
    Write-Output "Parsed $($models.Count) item models with System.Text.Json (strict defaults)"
}

Check 'I4 griefer spear root object unchanged' {
    $path = 'src/main/resources/assets/mysticartifacts/models/item/griefer_spear.json'
    $before = Get-Baseline $path
    $root = [regex]::Match($before, '(?s)\A.*?\n\}').Value
    $after = (Get-Content -Raw (Join-Path $repoRoot $path)).Replace("`r`n", "`n").TrimEnd()
    Assert-Contract ($root.Length -gt 0 -and $root -eq $after) 'Model changes extend beyond trailing text removal'
}

Check 'FUEL charge 5 only at 20-tick firing boundaries' {
    Assert-Contract ($spirit -match 'CLANGING_HOWL_FUEL_PER_CONSUMPTION = 5;' -and $spirit -match 'CLANGING_HOWL_FUEL_INTERVAL_TICKS = 20;') 'Fuel amount/interval incorrect'
    Assert-Contract ($spirit -notmatch 'CLANGING_HOWL_FUEL_PER_TICK') 'Obsolete per-tick fuel constant remains'
    $machine = Get-Method $spirit 'tickClangingHowlStateMachine'
    Assert-Contract ($machine -match '(?s)case 2:.*if \(clangingHowlPhaseTimer % CLANGING_HOWL_FUEL_INTERVAL_TICKS == 0\)\s*\{\s*if \(clangingHowlFuel < CLANGING_HOWL_FUEL_PER_CONSUMPTION\).*refillClangingHowlFuelFromEnderChest\(owner\).*return;.*clangingHowlFuel -= CLANGING_HOWL_FUEL_PER_CONSUMPTION;\s*\}\s*// Shoot flame every tick\s*shootClangingHowlFlame\(owner, muzzle\);\s*clangingHowlPhaseTimer--;') 'Refill/debit must be gated together; projectile must remain outside that gate'
    Assert-Contract ((Get-Method $spirit 'shootClangingHowlFlame') -notmatch 'clangingHowlFuel') 'Per-projectile fuel consumption remains'
    Assert-Contract ([regex]::Matches($spirit, 'clangingHowlFuel\s*-=').Count -eq 1) 'Fuel must have a single debit site'
    Assert-Contract ($spirit -match 'CLANGING_HOWL_MAX_FIRING_TICKS = 120;' -and $spirit -match 'CLANGING_HOWL_MAX_FUEL = 1600;' -and $spirit -match 'CLANGING_HOWL_FUEL_PER_CYLINDER = 1600;') 'Existing duration/capacity changed'
}

Check 'FUEL other spirit weapon logic and flame behavior preserved' {
    $before = Get-Baseline 'src/main/java/com/uniye/mysticartifacts/entity/ArtifactSpiritEntity.java'
    foreach ($method in @('handleRangedAttack', 'fireSingleProjectile', 'fireArrow', 'fireCrossbowShot', 'firePotion', 'firePotatoCannonShot', 'consumeAmmoFromEnderChest', 'consumePotatoCannonAmmo', 'refillClangingHowlFuelFromEnderChest')) {
        Assert-Contract ((Get-Method $spirit $method) -eq (Get-Method $before $method)) "Unrelated behavior changed: $method"
    }
    $originalFlame = (Get-Method $before 'shootClangingHowlFlame') -replace '(?m)^\s*clangingHowlFuel -= CLANGING_HOWL_FUEL_PER_TICK;\n', ''
    Assert-Contract ((Get-Method $spirit 'shootClangingHowlFlame') -eq $originalFlame) 'Flame behavior changed beyond fuel debit removal'
}

Check 'DOC corrected fuel units and animation clock' {
    $design = Get-Content -Raw (Join-Path $repoRoot 'docs/superpowers/specs/2026-09-04-mystic-artifacts-enhancements-design.md')
    $plan = Get-Content -Raw (Join-Path $repoRoot 'docs/superpowers/plans/2026-09-04-mystic-artifacts-enhancements-plan.md')
    foreach ($doc in @($design, $plan)) {
        Assert-Contract ($doc -notmatch '5 fuel/tick|每 tick 消耗 5|每生成一次.*扣除 5') 'Outdated fuel unit remains'
        Assert-Contract ($doc -match '每 20 tick 消耗 5 fuel') 'Corrected fuel unit missing'
    }
    Assert-Contract ($plan -match 'float animationTime = ageInTicks;' -and $plan -notmatch 'ageInTicks \+ partialTicks') 'Plan retains double interpolation'
}

Write-Output "Contracts: $script:passed passed, $($failures.Count) failed"
if ($failures.Count -gt 0) { exit 1 }
