package com.uniye.mysticartifacts.client.trail;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TrailSightClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue RANGE;
    public static final ForgeConfigSpec.IntValue RETENTION_TICKS;
    public static final ForgeConfigSpec.IntValue SAMPLE_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue MIN_STEP;
    public static final ForgeConfigSpec.IntValue MAX_ENTITIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("trailSight");
        RANGE = builder.comment("Maximum client-side tracking distance in blocks.")
                .defineInRange("trailSightRange", 48, 8, 128);
        RETENTION_TICKS = builder.comment("How long trail samples remain visible, in ticks.")
                .defineInRange("trailSightRetentionTicks", 100, 20, 400);
        SAMPLE_INTERVAL = builder.comment("Client ticks between entity sampling passes.")
                .defineInRange("trailSightSampleInterval", 2, 1, 20);
        MIN_STEP = builder.comment("Minimum horizontal movement required to add a sample; zero records every non-zero movement.")
                .defineInRange("trailSightMinStep", 0.0D, 0.0D, 4.0D);
        MAX_ENTITIES = builder.comment("Maximum number of entity trails kept at once.")
                .defineInRange("trailSightMaxEntities", 256, 16, 2048);
        builder.pop();
        SPEC = builder.build();
    }

    private TrailSightClientConfig() {
    }
}
