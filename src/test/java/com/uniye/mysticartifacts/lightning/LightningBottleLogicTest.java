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
        requireFloatEquals(1.0F, LightningBottleLogic.wetnessMultiplier(-1),
                "no wetness keeps the original damage");
        requireFloatEquals(1.2F, LightningBottleLogic.wetnessMultiplier(0),
                "wetness I adds twenty percent");
        requireFloatEquals(1.4F, LightningBottleLogic.wetnessMultiplier(1),
                "wetness II adds forty percent");
        requireFloatEquals(5.6F, LightningBottleLogic.primaryBonus(4.0F, false, 1),
                "non-lightning source applies wetness to the bottle bonus");
        requireFloatEquals(4.0F, LightningBottleLogic.primaryBonus(4.0F, true, 1),
                "lightning source leaves the bonus to Cataclysm's global wetness pass");

        System.out.println("PASS: Lightning Bottle target count rules");
    }

    private static void requireEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireFloatEquals(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.0001F) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
