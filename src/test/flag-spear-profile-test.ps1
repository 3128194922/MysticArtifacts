$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$outputDir = Join-Path $projectRoot 'build/flag-spear-profile-test'
$source = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearTrailProfile.java'
$test = Join-Path $projectRoot 'src/test/java/com/uniye/mysticartifacts/flag/FlagSpearTrailProfileTest.java'

New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
& 'C:/Program Files/Java/jdk-17/bin/javac.exe' -encoding UTF-8 -d $outputDir $source $test
if ($LASTEXITCODE -ne 0) { throw 'Flag spear trail profile test compilation failed' }

& 'C:/Program Files/Java/jdk-17/bin/java.exe' -cp $outputDir com.uniye.mysticartifacts.flag.FlagSpearTrailProfileTest
if ($LASTEXITCODE -ne 0) { throw 'Flag spear trail profile test failed' }

Write-Output 'flag spear trail profile contract: PASS'
