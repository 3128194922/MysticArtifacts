$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteExposure.java'
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('mysticartifacts-sculk-exposure-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    if (-not (Test-Path -LiteralPath $source)) { throw 'missing exposure source' }

    $harness = Join-Path $tempRoot 'ExposureHarness.java'
    @'
import com.uniye.mysticartifacts.sculk.SculkSymbioteExposure;

public final class ExposureHarness {
    public static void main(String[] args) {
        SculkSymbioteExposure exposure = new SculkSymbioteExposure(100);
        if (exposure.add(10) != 10 || exposure.isFull()) throw new AssertionError("first accumulation");
        if (exposure.add(90) != 100 || !exposure.isFull()) throw new AssertionError("full accumulation");
        exposure.reset();
        if (exposure.value() != 0 || exposure.isFull()) throw new AssertionError("reset");
        exposure.set(500);
        if (exposure.value() != 100) throw new AssertionError("upper clamp");
        exposure.set(-5);
        if (exposure.value() != 0) throw new AssertionError("lower clamp");
        System.out.println("sculk symbiote exposure: PASS");
    }
}
'@ | Set-Content -LiteralPath $harness -Encoding UTF8

    & javac -d $tempRoot $source $harness
    if ($LASTEXITCODE -ne 0) { throw 'ExposureHarness compilation failed' }
    & java -cp $tempRoot ExposureHarness
    if ($LASTEXITCODE -ne 0) { throw 'ExposureHarness failed' }
} finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
