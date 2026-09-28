package com.uniye.mysticartifacts.client.trail;

import com.uniye.mysticartifacts.client.sculk.SculkSymbioteClientState;
import com.uniye.mysticartifacts.client.deathscythe.DeathScytheTargetClientState;
import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailSightClientHandler {
    private TrailSightClientHandler() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SculkSymbioteClientState.clear();
        DeathScytheTargetClientState.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            SculkSymbioteClientState.clear();
            DeathScytheTargetClientState.clear();
        }
    }
}
