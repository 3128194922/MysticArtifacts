package com.uniye.mysticartifacts.flag;

import com.uniye.mysticartifacts.client.flag.FlagSpearTrailProfile;

public final class FlagSpearTrailProfileTest {
    private FlagSpearTrailProfileTest() {
    }

    public static void main(String[] args) {
        requireEquals(5, FlagSpearTrailProfile.TRAIL_COUNT, "trail count");
        requireEquals(24, FlagSpearTrailProfile.SAMPLES_PER_TRAIL, "sample count");
        requireClose(5.0D, FlagSpearTrailProfile.radiusAt(0), "first sample radius");
        requireClose(5.0D, FlagSpearTrailProfile.radiusAt(23), "last sample radius");
        requireClose(0.0D, FlagSpearTrailProfile.phaseFor(0), "first phase");
        requireClose(Math.PI * 2.0D, FlagSpearTrailProfile.phaseFor(4), "last phase");
    }

    private static void requireEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireClose(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 1.0E-6D) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
