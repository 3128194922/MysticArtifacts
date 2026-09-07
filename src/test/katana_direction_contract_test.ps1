$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$rendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java'
$circleRendererPath = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/render/KatanaCircleSlashRenderer.java'

$rendererText = Get-Content -Raw $rendererPath
$circleRendererText = Get-Content -Raw $circleRendererPath
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($entry in @(
    @{ Name = 'Katana slash'; Text = $rendererText },
    @{ Name = 'Katana circle slash'; Text = $circleRendererText }
)) {
    if ($entry.Text -notmatch 'FORWARD_ALIGNMENT_DEGREES\s*=\s*180\.0F' -or
        $entry.Text -notmatch '-\s*90\.0F\s*\+\s*FORWARD_ALIGNMENT_DEGREES') {
        $failures.Add("$($entry.Name) renderer does not align its half-moon with the player's forward direction")
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'katana direction contract: PASS'
