package com.uniye.mysticartifacts.client.trail;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TrailSightClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue RANGE;
    public static final ForgeConfigSpec.IntValue RETENTION_SECONDS;
    public static final ForgeConfigSpec.IntValue SAMPLE_INTERVAL;
    public static final ForgeConfigSpec.IntValue MAX_ENTITIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("trailSight");
        RANGE = builder.comment("Maximum client-side tracking distance in blocks.")
                .defineInRange("trailSightRange", 48, 8, 128);
        RETENTION_SECONDS = builder.comment("How long trail samples remain visible, in seconds; default is 30 seconds.")
                .defineInRange("trailSightRetentionSeconds", 30, 1, 120);
        SAMPLE_INTERVAL = builder.comment("Client ticks between entity sampling passes.")
                .defineInRange("trailSightSampleInterval", 2, 1, 20);
        MAX_ENTITIES = builder.comment("Maximum number of entity trails kept at once.")
                .defineInRange("trailSightMaxEntities", 256, 16, 2048);
        builder.pop();
        SPEC = builder.build();
    }

    public static int retentionTicks() {
        return Math.max(1, RETENTION_SECONDS.get()) * 20;
    }

    private TrailSightClientConfig() {
    }
}
