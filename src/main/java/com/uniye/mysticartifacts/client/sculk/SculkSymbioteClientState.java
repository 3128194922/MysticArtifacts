package com.uniye.mysticartifacts.client.sculk;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.client.trail.TrailSightClientConfig;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import com.uniye.mysticartifacts.network.SculkSymbioteVibrationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SculkSymbioteClientState {
    private static final Deque<Marker> MARKERS = new ArrayDeque<>();
    private static boolean active;
    private static int exposure;
    private static int exposureMaximum = 1;

    private SculkSymbioteClientState() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean wearing = minecraft.level != null && player != null && TrailSightItem.isWearing(player);
        setActive(wearing);
        if (minecraft.level != null) {
            trim(minecraft.level.getGameTime());
        } else {
            clear();
        }
    }

    public static void setActive(boolean enabled) {
        if (active == enabled) return;
        active = enabled;
        if (!enabled) {
            MARKERS.clear();
            exposure = 0;
            exposureMaximum = 1;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void setExposure(int value, int maximum) {
        exposureMaximum = Math.max(1, maximum);
        exposure = Math.max(0, Math.min(exposureMaximum, value));
    }

    public static int exposureValue() {
        return exposure;
    }

    public static int exposureMaximum() {
        return exposureMaximum;
    }

    public static void acceptMarkers(List<SculkSymbioteVibrationPacket.Marker> incoming) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        long tick = minecraft.level.getGameTime();
        int max = Math.max(1, TrailSightClientConfig.MARKER_LIFETIME.get()) * 4;
        for (SculkSymbioteVibrationPacket.Marker marker : incoming) {
            if (MARKERS.size() >= max) MARKERS.removeFirst();
            MARKERS.addLast(new Marker(marker.position(), marker.frequency(), marker.sourceId(), tick));
        }
    }

    public static List<Marker> snapshot(ClientLevel level) {
        trim(level.getGameTime());
        return new ArrayList<>(MARKERS);
    }

    public static void clear() {
        MARKERS.clear();
        exposure = 0;
        exposureMaximum = 1;
        if (active) setActive(false);
        active = false;
    }

    private static void trim(long currentTick) {
        long lifetime = Math.max(1, TrailSightClientConfig.MARKER_LIFETIME.get());
        MARKERS.removeIf(marker -> currentTick - marker.tick() >= lifetime);
    }

    public record Marker(net.minecraft.world.phys.Vec3 position, int frequency, UUID sourceId, long tick) {
    }
}
