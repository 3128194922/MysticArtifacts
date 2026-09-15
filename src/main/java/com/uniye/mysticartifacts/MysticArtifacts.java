package com.uniye.mysticartifacts;

import com.mojang.logging.LogUtils;
import com.uniye.mysticartifacts.compat.QuarkCompat;
import com.uniye.mysticartifacts.config.ModConfigs;
import com.uniye.mysticartifacts.init.ModRegistries;
import com.uniye.mysticartifacts.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MysticArtifacts.MODID)
public class MysticArtifacts
{
    public static final String MODID = "mysticartifacts";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MysticArtifacts()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        NetworkHandler.register();

        ModRegistries.register(modEventBus);
        ModConfigs.register();
        QuarkCompat.register();
    }
}
