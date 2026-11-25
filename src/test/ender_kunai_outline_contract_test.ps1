$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$kunaiPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/entity/EnderKunaiEntity.java'
$kunaiText = Get-Content -Raw $kunaiPath
$failures = [System.Collections.Generic.List[string]]::new()

if ($kunaiText -notmatch 'EntityDataAccessor<Integer>\s+OUTLINE_COLOR' -or
    $kunaiText -notmatch 'EntityDataSerializers\.INT') {
    $failures.Add('末影苦无没有同步到客户端的描边颜色状态')
}
if ($kunaiText -notmatch 'RED_OUTLINE\s*=\s*0xFF0000' -or
    $kunaiText -notmatch 'RED_OUTLINE_LEAD_TICKS\s*=\s*20') {
    $failures.Add('末影苦无没有定义消失前的红色描边窗口')
}
if ($kunaiText -notmatch 'getCustomOutlineColor\(\)[\s\S]*?entityData\.get\(OUTLINE_COLOR\)') {
    $failures.Add('末影苦无渲染端没有读取同步描边颜色')
}
if ($kunaiText -notmatch 'lifetimeTicks\s*>=\s*MAX_LIFETIME_TICKS\s*-\s*RED_OUTLINE_LEAD_TICKS') {
    $failures.Add('末影苦无没有在生命周期末段切换为红色')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'ender kunai outline contract: PASS'
