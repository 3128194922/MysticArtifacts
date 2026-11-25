package com.uniye.mysticartifacts.lightning;

public final class LightningBottleLogic {
    private LightningBottleLogic() {
    }

    public static int effectiveMaxTargets(int configuredMaximum, boolean raining) {
        int maximum = Math.max(0, configuredMaximum);
        return raining ? maximum * 2 : maximum;
    }
}
