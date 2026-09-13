$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot 'main/java/com/uniye/mysticartifacts/client/trail'
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('mysticartifacts-trail-test-' + [guid]::NewGuid().ToString('N'))
$harness = Join-Path $tempRoot 'TrailBufferHarness.java'

New-Item -ItemType Directory -Path $tempRoot | Out-Null
try {
    $harnessContent = @'
import com.uniye.mysticartifacts.client.trail.TrailBuffer;
import com.uniye.mysticartifacts.client.trail.TrailSample;

public final class TrailBufferHarness {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        TrailBuffer buffer = new TrailBuffer(2, 100);
        buffer.add(new TrailSample(0, 0, 0, 1, 0, 0));
        buffer.add(new TrailSample(1, 0, 0, 1, 0, 50));
        buffer.add(new TrailSample(2, 0, 0, 1, 0, 101));

        check(buffer.samples().size() == 2, "retains bounded samples");
        check(buffer.samples().get(0).tick() == 50, "drops the oldest sample at capacity");
        buffer.prune(202);
        check(buffer.samples().isEmpty(), "expires after retention ticks");

        boolean immutable = false;
        try {
            buffer.samples().clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        check(immutable, "returns an immutable snapshot");
        System.out.println("trail sight buffer: PASS");
    }
}
'@
    [System.IO.File]::WriteAllText($harness, $harnessContent, [System.Text.UTF8Encoding]::new($false))

    & javac -d $tempRoot (Join-Path $sourceRoot 'TrailSample.java') (Join-Path $sourceRoot 'TrailBuffer.java') $harness
    if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
    & java -cp $tempRoot TrailBufferHarness
    if ($LASTEXITCODE -ne 0) { throw 'TrailBufferHarness failed' }
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
