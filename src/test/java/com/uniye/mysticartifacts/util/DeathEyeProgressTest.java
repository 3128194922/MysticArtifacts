package com.uniye.mysticartifacts.util;

public final class DeathEyeProgressTest {
    private DeathEyeProgressTest() {
    }

    public static void main(String[] args) {
        DeathEyeProgress.StrikeResult accumulated = DeathEyeProgress.apply(5.0, 3.0f, false);
        requireClose(accumulated.accumulatedDamage(), 8.0, "non-execution hit accumulates damage");
        requireClose(accumulated.damage(), 3.0, "non-execution hit keeps original damage");
        require(!accumulated.executed(), "non-execution hit does not execute");

        DeathEyeProgress.StrikeResult executed = DeathEyeProgress.apply(8.0, 4.0f, true);
        requireClose(executed.accumulatedDamage(), 0.0, "execution consumes accumulated damage");
        requireClose(executed.damage(), 12.0, "execution adds accumulated damage to triggering hit");
        require(executed.executed(), "cut-line hit executes");

        requireClose(DeathEyeProgress.renderRatio(5.0, 20.0f), 0.25, "render ratio uses max health");
        requireClose(DeathEyeProgress.renderRatio(-1.0, 20.0f), 0.0, "render ratio clamps negative progress");
        requireClose(DeathEyeProgress.renderRatio(30.0, 20.0f), 1.0, "render ratio clamps overfull progress");
        requireClose(DeathEyeProgress.renderRatio(5.0, 0.0f), 0.0, "render ratio handles invalid max health");

        System.out.println("PASS: Death Eye accumulation, execution conversion, reset and render ratio");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void requireClose(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 1.0e-5) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
