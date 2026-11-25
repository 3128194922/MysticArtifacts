package com.uniye.mysticartifacts.client.sculk;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端只保留方块与幽匿声波标记，实体模型和名称牌全部隐藏。 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SculkEntityVisibilityHandler {
    private SculkEntityVisibilityHandler() {
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (isActive()) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean isActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return SculkSymbioteClientState.isActive()
                || minecraft.player != null && TrailSightItem.isWearing(minecraft.player);
    }
}
