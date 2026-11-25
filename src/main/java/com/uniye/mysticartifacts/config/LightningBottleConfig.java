package com.uniye.mysticartifacts.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LightningBottleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_TARGETS = BUILDER
            .comment("Maximum number of additional entities hit by the Lightning Bottle chain.")
            .defineInRange("LightningBottleMaxTargets", 5, 0, 64);

    public static final ForgeConfigSpec.IntValue MAX_THUNDER_POWER = BUILDER
            .comment("Thunder power required for the Lightning Bottle to release its lightning field.")
            .defineInRange("LightningBottleMaxThunderPower", 10, 1, 10000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private LightningBottleConfig() {
    }
}
