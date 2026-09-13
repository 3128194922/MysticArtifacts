package com.uniye.mysticartifacts.client.trail;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailSightTracker {
    private static final Map<UUID, TrailBuffer> TRACKS = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();
    private static final Map<UUID, TrailBuffer> READ_ONLY_TRACKS = Collections.unmodifiableMap(TRACKS);

    private static ClientLevel trackedLevel;
    private static int sampleCounter;

    private TrailSightTracker() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            return;
        }

        tick(minecraft.level, minecraft.player);
    }

    public static void tick(ClientLevel level, LocalPlayer player) {
        if (trackedLevel != level) {
            clear();
            trackedLevel = level;
        }

        if (!TrailSightItem.isWearing(player)) {
            clear();
            return;
        }

        long currentTick = level.getGameTime();
        prune(currentTick);

        int interval = Math.max(1, TrailSightClientConfig.SAMPLE_INTERVAL.get());
        sampleCounter++;
        if (sampleCounter < interval) return;
        sampleCounter = 0;

        double range = TrailSightClientConfig.RANGE.get();
        double rangeSqr = range * range;
        int maxEntities = Math.max(1, TrailSightClientConfig.MAX_ENTITIES.get());
        List<Entity> candidates = selectNearestEntities(level, player, rangeSqr, maxEntities);

        Set<UUID> selected = new HashSet<>(candidates.size());
        for (Entity entity : candidates) {
            UUID id = entity.getUUID();
            selected.add(id);
            Vec3 position = entity.position();
            Vec3 previous = LAST_POSITIONS.put(id, position);
            TrailBuffer trail = TRACKS.computeIfAbsent(id,
                    ignored -> new TrailBuffer(maxSamples(), TrailSightClientConfig.retentionTicks()));

            if (previous == null) {
                trail.prune(currentTick);
                continue;
            }

            Vec3 delta = position.subtract(previous);
            if (!shouldRecordMovement(delta)) {
                trail.prune(currentTick);
                continue;
            }

            Vec3 direction = horizontalDirection(entity, delta);
            trail.add(new TrailSample(position.x, position.y, position.z,
                    direction.x, direction.z, currentTick));
        }

        TRACKS.keySet().removeIf(id -> !selected.contains(id));
        LAST_POSITIONS.keySet().removeIf(id -> !selected.contains(id));
    }

    public static void prune(long currentTick) {
        TRACKS.values().forEach(trail -> trail.prune(currentTick));
        TRACKS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void clear() {
        TRACKS.clear();
        LAST_POSITIONS.clear();
        sampleCounter = 0;
    }

    public static Map<UUID, TrailBuffer> getTracks() {
        return READ_ONLY_TRACKS;
    }

    private static List<Entity> selectNearestEntities(ClientLevel level, LocalPlayer player,
                                                      double rangeSqr, int maxEntities) {
        List<Entity> candidates = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved() || entity.distanceToSqr(player) > rangeSqr) continue;
            candidates.add(entity);
        }

        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (candidates.size() <= maxEntities) return candidates;
        return new ArrayList<>(candidates.subList(0, maxEntities));
    }

    private static boolean shouldRecordMovement(Vec3 delta) {
        double horizontalDistanceSqr = delta.x * delta.x + delta.z * delta.z;
        if (horizontalDistanceSqr <= 0.0D) return false;
        return horizontalDistanceSqr > 0.0D;
    }

    private static int maxSamples() {
        int interval = Math.max(1, TrailSightClientConfig.SAMPLE_INTERVAL.get());
        int retention = Math.max(1, TrailSightClientConfig.retentionTicks());
        return Math.max(2, (retention + interval - 1) / interval + 2);
    }

    private static Vec3 horizontalDirection(Entity entity, Vec3 delta) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.x * motion.x + motion.z * motion.z > 0.000001D) {
            return new Vec3(motion.x, 0.0D, motion.z).normalize();
        }
        if (delta.x * delta.x + delta.z * delta.z > 0.000001D) {
            return new Vec3(delta.x, 0.0D, delta.z).normalize();
        }

        float yaw = entity.getYRot() * ((float) Math.PI / 180.0F);
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }
}
