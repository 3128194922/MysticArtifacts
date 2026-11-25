package com.uniye.mysticartifacts.lightning;

public final class LightningBottleLogicTest {
    private LightningBottleLogicTest() {
    }

    public static void main(String[] args) {
        requireEquals(5, LightningBottleLogic.effectiveMaxTargets(5, false),
                "normal weather uses the configured maximum");
        requireEquals(10, LightningBottleLogic.effectiveMaxTargets(5, true),
                "rain doubles the configured maximum");
        requireEquals(0, LightningBottleLogic.effectiveMaxTargets(0, true),
                "zero disables chain targets");

        System.out.println("PASS: Lightning Bottle target count rules");
    }

    private static void requireEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
