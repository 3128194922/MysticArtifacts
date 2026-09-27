package com.uniye.mysticartifacts.util;

public final class DeathScytheLogicTest {
    private DeathScytheLogicTest() {
    }

    public static void main(String[] args) {
        requireEquals(100, DeathScytheLogic.ENERGY_TICKS, "five seconds uses 100 ticks");
        requireEquals(100, DeathScytheLogic.remainingEnergyTicks(200, 100), "fresh energy is full");
        requireEquals(50, DeathScytheLogic.remainingEnergyTicks(200, 150), "energy counts down by game time");
        requireEquals(0, DeathScytheLogic.remainingEnergyTicks(200, 250), "expired energy clamps to zero");
        requireEquals(13, DeathScytheLogic.barWidth(100), "full energy fills the bar");
        requireEquals(6, DeathScytheLogic.barWidth(50), "half energy maps to six pixels");
        requireEquals(0, DeathScytheLogic.barWidth(0), "empty energy maps to zero");
        requireFalse(DeathScytheLogic.hasEnergy(200, 200), "energy expires at its timestamp");
        requireEquals(1, DeathScytheLogic.nextSequence(0), "first slash sequence is one");

        System.out.println("PASS: Death Scythe logic rules");
    }

    private static void requireEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }
}
