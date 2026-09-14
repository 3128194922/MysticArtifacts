package com.uniye.mysticartifacts.client.trail;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TrailSightClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MARKER_LIFETIME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("sculkSymbiote");
        MARKER_LIFETIME = builder.comment("How long a vibration marker remains visible, in ticks.")
                .defineInRange("sculkSymbioteMarkerLifetime", 20, 1, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private TrailSightClientConfig() {
    }
}
