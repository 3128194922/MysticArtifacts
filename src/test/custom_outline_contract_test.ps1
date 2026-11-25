$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts'
$rendererPath = Join-Path $javaRoot 'client/render/CustomOutlineRenderer.java'
$outlineEntityPath = Join-Path $javaRoot 'entity/CustomOutlineEntity.java'
$arrowRendererPath = Join-Path $javaRoot 'client/render/ModArrowRenderer.java'
$pokerRendererPath = Join-Path $javaRoot 'client/render/PokerCardRenderer.java'
$kunaiEntityPath = Join-Path $javaRoot 'entity/EnderKunaiEntity.java'
$cardEntityPath = Join-Path $javaRoot 'entity/PokerCardEntity.java'

$rendererText = if (Test-Path $rendererPath) { Get-Content -Raw $rendererPath } else { '' }
$outlineEntityText = if (Test-Path $outlineEntityPath) { Get-Content -Raw $outlineEntityPath } else { '' }
$arrowRendererText = Get-Content -Raw $arrowRendererPath
$pokerRendererText = Get-Content -Raw $pokerRendererPath
$kunaiEntityText = Get-Content -Raw $kunaiEntityPath
$cardEntityText = Get-Content -Raw $cardEntityPath
$failures = [System.Collections.Generic.List[string]]::new()

if (-not (Test-Path $rendererPath) -or
    $rendererText -notmatch 'OutlineBufferSource' -or
    $rendererText -notmatch 'RenderType\.outline' -or
    $rendererText -notmatch 'setColor') {
    $failures.Add('通用描边渲染器未使用 OutlineBufferSource、描边 RenderType 和自定义颜色')
}
if (-not (Test-Path $outlineEntityPath) -or
    $outlineEntityText -notmatch 'interface CustomOutlineEntity' -or
    $outlineEntityText -notmatch 'getCustomOutlineColor') {
    $failures.Add('缺少通用实体描边颜色接口')
}
if ($arrowRendererText -notmatch 'extends CustomOutlineRenderer') {
    $failures.Add('ModArrowRenderer 未接入通用描边渲染器')
}
if ($pokerRendererText -notmatch 'extends CustomOutlineRenderer') {
    $failures.Add('PokerCardRenderer 未接入通用描边渲染器')
}
if ($kunaiEntityText -notmatch 'CustomOutlineEntity' -or
    $kunaiEntityText -notmatch 'hasCustomOutlineRendering' -or
    $kunaiEntityText -notmatch 'getCustomOutlineColor' -or
    $kunaiEntityText -match 'setGlowingTag\(true\)') {
    $failures.Add('末影苦无仍依赖原版队伍/发光描边，或未提供自定义颜色')
}
if ($cardEntityText -notmatch 'CustomOutlineEntity' -or
    $cardEntityText -notmatch 'hasCustomOutlineRendering' -or
    $cardEntityText -notmatch 'getCustomOutlineColor' -or
    $cardEntityText -match 'Scoreboard|PlayerTeam|GimmeThat_|setGlowingTag\(true\)') {
    $failures.Add('扑克牌仍依赖原版队伍/发光描边，或未提供自定义颜色')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'custom outline contract: PASS'
