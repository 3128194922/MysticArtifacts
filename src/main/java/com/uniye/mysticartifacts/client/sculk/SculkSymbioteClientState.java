package com.uniye.mysticartifacts.client.sculk;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.client.trail.TrailSightClientConfig;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import com.uniye.mysticartifacts.network.SculkSymbioteVibrationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SculkSymbioteClientState {
    private static final Deque<Marker> MARKERS = new ArrayDeque<>();
    private static boolean active;
    private static boolean previousHitBoxes;

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
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        if (enabled) {
            previousHitBoxes = dispatcher.shouldRenderHitBoxes();
            dispatcher.setRenderHitBoxes(false);
            dispatcher.setRenderShadow(false);
            installWrappers(minecraft, dispatcher);
        } else {
            dispatcher.setRenderHitBoxes(previousHitBoxes);
            dispatcher.setRenderShadow(true);
            restoreWrappers(dispatcher);
            MARKERS.clear();
        }
    }

    public static boolean isActive() {
        return active;
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
        if (active) setActive(false);
        active = false;
    }

    private static void trim(long currentTick) {
        long lifetime = Math.max(1, TrailSightClientConfig.MARKER_LIFETIME.get());
        MARKERS.removeIf(marker -> currentTick - marker.tick() >= lifetime);
    }

    private static void installWrappers(Minecraft minecraft, EntityRenderDispatcher dispatcher) {
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(
                dispatcher, minecraft.getItemRenderer(), minecraft.getBlockRenderer(), null,
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        for (Map.Entry<EntityType<?>, EntityRenderer<?>> entry : dispatcher.renderers.entrySet()) {
            entry.setValue(HiddenEntityRenderer.wrap(context, entry.getValue()));
        }
        for (Map.Entry<String, EntityRenderer<? extends net.minecraft.world.entity.player.Player>> entry
                : dispatcher.getSkinMap().entrySet()) {
            entry.setValue((EntityRenderer<? extends net.minecraft.world.entity.player.Player>)
                    HiddenEntityRenderer.wrap(context, entry.getValue()));
        }
    }

    private static void restoreWrappers(EntityRenderDispatcher dispatcher) {
        dispatcher.renderers.replaceAll((type, renderer) -> unwrap(renderer));
        dispatcher.getSkinMap().replaceAll((skin, renderer) ->
                (EntityRenderer<? extends net.minecraft.world.entity.player.Player>) unwrap(renderer));
    }

    private static EntityRenderer<?> unwrap(EntityRenderer<?> renderer) {
        if (renderer instanceof HiddenEntityRenderer hidden) return hidden.delegate();
        return renderer;
    }

    public record Marker(net.minecraft.world.phys.Vec3 position, int frequency, UUID sourceId, long tick) {
    }
}
