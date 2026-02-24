package com.uniye.mysticartifacts;

import com.mojang.logging.LogUtils;
import com.uniye.mysticartifacts.client.render.*;
import com.uniye.mysticartifacts.init.ModCreativeModTabs;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.network.NetworkHandler;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod(MysticArtifacts.MODID)
public class MysticArtifacts
{
    public static final String MODID = "mysticartifacts";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MysticArtifacts()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        NetworkHandler.register();

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeModTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
        
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            EntityRenderers.register(ModEntities.SLIME_ARROW.get(), ctx -> new ModArrowRenderer(ctx, "slime_arrow"));
            EntityRenderers.register(ModEntities.AIRBURST_ARROW.get(), ctx -> new ModArrowRenderer(ctx, "airburst_arrow"));
            EntityRenderers.register(ModEntities.EXPLODING_ARROW.get(), ctx -> new ModArrowRenderer(ctx, "exploding_arrow"));
            EntityRenderers.register(ModEntities.FINAL_EXPLODING_ARROW.get(), ctx -> new ModArrowRenderer(ctx, "final_exploding_arrow"));
            EntityRenderers.register(ModEntities.ENDER_KUNAI.get(), ctx -> new ModArrowRenderer(ctx, "kunai"));
            EntityRenderers.register(ModEntities.POKER_CARD.get(), PokerCardRenderer::new);
            EntityRenderers.register(ModEntities.NETHER_OF_VOICE.get(), ThrownItemRenderer::new);
            
            EntityRenderers.register(ModEntities.TWO_DRAGONS_PLAY_BALL.get(), TwoDragonsPlayBallRenderer::new);
            EntityRenderers.register(ModEntities.TWO_DRAGONS_FAN.get(), TwoDragonsFanRenderer::new);
            EntityRenderers.register(ModEntities.SWORD_PHANTOM.get(), SwordPhantomRenderer::new);
            
            CuriosRendererRegistry.register(ModItems.SWORD_SWARM_CHARM.get(), SwordSwarmCharmRenderer::new);
        }
    }
}
