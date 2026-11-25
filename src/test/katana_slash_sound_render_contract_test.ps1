$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sounds = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/init/ModSounds.java')
$soundsJson = Get-Content -Raw (Join-Path $projectRoot 'main/resources/assets/mysticartifacts/sounds.json')
$muramasa = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java')
$renderer = Get-Content -Raw (Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java')
$source = Join-Path (Split-Path -Parent $projectRoot) 'docs/assets/katana_slash_source.txt'
$failures = [System.Collections.Generic.List[string]]::new()

if ($sounds -notmatch 'KATANA_SLASH\s*=\s*SOUNDS\.register\("entity\.katana_slash"') {
    $failures.Add('katana slash sound is not registered')
}
if ($soundsJson -notmatch '"entity\.katana_slash"' -or
    ([regex]::Matches($soundsJson, 'mysticartifacts:katana_slash_[123]')).Count -lt 3) {
    $failures.Add('katana slash sound event does not expose three variants')
}
if ($muramasa -notmatch 'ModSounds\.KATANA_SLASH\.get\(\)' -or
    $muramasa -notmatch 'fireGhostSlash') {
    $failures.Add('ghost slash does not trigger the katana slash sound on the server')
}
if ($renderer -notmatch 'AFTERIMAGE_COUNT' -or
    $renderer -notmatch 'renderAfterimage' -or
    $renderer -notmatch 'glowPulse' -or
    $renderer -notmatch '0xFFFFFF') {
    $failures.Add('katana ghost slash lacks the layered afterimage and white-hot core treatment')
}
if (-not (Test-Path $source) -or
    (Get-Content -Raw $source) -notmatch 'CC0' -or
    (Get-Content -Raw $source) -notmatch 'opengameart\.org') {
    $failures.Add('katana slash sound source/license record is missing')
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana slash sound and render contract: PASS'
