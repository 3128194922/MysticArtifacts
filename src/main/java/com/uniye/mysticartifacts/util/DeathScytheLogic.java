package com.uniye.mysticartifacts.util;

public final class DeathScytheLogic {
    public static final int ENERGY_TICKS = 100;
    public static final int RIGHT_CLICK_COOLDOWN_TICKS = 20;

    private DeathScytheLogic() {
    }

    public static int remainingEnergyTicks(long energyUntil, long gameTime) {
        return (int) Math.max(0L, Math.min(ENERGY_TICKS, energyUntil - gameTime));
    }

    public static int barWidth(int remainingTicks) {
        int clamped = Math.max(0, Math.min(ENERGY_TICKS, remainingTicks));
        return 13 * clamped / ENERGY_TICKS;
    }

    public static boolean hasEnergy(long energyUntil, long gameTime) {
        return energyUntil > gameTime;
    }

    public static int nextSequence(int current) {
        return current + 1;
    }
}
