package com.uniye.mysticartifacts.client.render;

public final class KatanaSlashRenderProfileTest {
    private KatanaSlashRenderProfileTest() {
    }

    public static void main(String[] args) {
        requireEquals(40, KatanaSlashRenderProfile.segmentsForDistanceSqr(16.0D * 16.0D),
                "near sword qi keeps the high-detail mesh");
        requireEquals(24, KatanaSlashRenderProfile.segmentsForDistanceSqr(16.0D * 16.0D + 1.0D),
                "far sword qi uses the reduced mesh");
        requireEquals(40, KatanaSlashRenderProfile.nearSegments(),
                "near profile exposes the high-detail segment count");
        requireEquals(24, KatanaSlashRenderProfile.farSegments(),
                "far profile exposes the reduced segment count");

        System.out.println("PASS: Katana sword qi render LOD profile");
    }

    private static void requireEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
