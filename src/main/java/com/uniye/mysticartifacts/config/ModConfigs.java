package com.uniye.mysticartifacts.config;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.client.trail.TrailSightClientConfig;
import com.uniye.mysticartifacts.revolver.RevolverConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ModConfigs {
    private ModConfigs() {
    }

    public static void register() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER,
                RevolverConfig.SPEC, "mysticartifacts-revolver-server.toml");
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT,
                TrailSightClientConfig.SPEC,
                "mysticartifacts-trail-sight-client.toml");
    }
}
