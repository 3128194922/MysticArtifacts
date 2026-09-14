package com.uniye.mysticartifacts.sculk;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 一个佩戴者对应一个原版震动系统实例，避免全局扫描所有实体。 */
public final class SculkSymbioteSensor implements VibrationSystem, VibrationSystem.User {
    private final ServerPlayer player;
    private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final PositionSource positionSource;
    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicListener;
    private final Map<UUID, SourceState> sources = new HashMap<>();
    private final List<SculkSymbioteServerHandler.MarkerData> markers = new ArrayList<>();
    private ServerLevel registeredLevel;
    private long currentTick;

    public SculkSymbioteSensor(ServerPlayer player) {
        this.player = player;
        this.positionSource = new EntityPositionSource(player, 0.0F);
        this.dynamicListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    }

    public boolean isFor(ServerPlayer candidate) {
        return player == candidate;
    }

    public void tick(ServerLevel level) {
        if (registeredLevel != level) {
            if (registeredLevel != null) {
                dynamicListener.remove(registeredLevel);
            }
            registeredLevel = level;
            dynamicListener.add(level);
        }
        dynamicListener.move(level);
        currentTick = level.getGameTime();
        tickSourceStates();
        VibrationSystem.Ticker.tick(level, vibrationData, this);
    }

    public List<SculkSymbioteServerHandler.MarkerData> drainMarkers() {
        if (markers.isEmpty()) return List.of();
        List<SculkSymbioteServerHandler.MarkerData> result = List.copyOf(markers);
        markers.clear();
        return result;
    }

    public void remove() {
        if (registeredLevel != null) {
            dynamicListener.remove(registeredLevel);
            registeredLevel = null;
        }
        sources.clear();
        markers.clear();
        vibrationData.setCurrentVibration(null);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this;
    }

    @Override
    public int getListenerRadius() {
        return Config.SCULK_VIBRATION_RADIUS.get();
    }

    @Override
    public PositionSource getPositionSource() {
        return positionSource;
    }

    @Override
    public boolean canReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event, GameEvent.Context context) {
        return TrailSightItem.isWearing(player)
                && context.sourceEntity() != player
                && !player.isRemoved();
    }

    @Override
    public void onReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event,
                                    Entity sourceEntity, Entity projectileOwner, float distance) {
        int maxMarkers = Math.max(1, Config.SCULK_MAX_MARKERS_PER_TICK.get());
        if (markers.size() < maxMarkers) {
            markers.add(new SculkSymbioteServerHandler.MarkerData(
                    Vec3.atCenterOf(pos), VibrationSystem.getGameEventFrequency(event),
                    sourceEntity == null ? null : sourceEntity.getUUID()));
        }

        if (!(sourceEntity instanceof LivingEntity source) || source == player
                || source.isRemoved() || !source.isAlive()) {
            return;
        }

        UUID sourceId = source.getUUID();
        if (!sources.containsKey(sourceId)
                && sources.size() >= Math.max(1, Config.SCULK_MAX_TRACKED_SOURCES.get())) {
            sources.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().lastSeenTick))
                    .map(Map.Entry::getKey)
                    .ifPresent(sources::remove);
        }
        SourceState state = sources.computeIfAbsent(sourceId, ignored ->
                new SourceState(Config.SCULK_EXPOSURE_MAX.get()));
        state.lastSeenTick = currentTick;
        if (state.exposure.add(Math.max(1, Config.SCULK_EXPOSURE_PER_EVENT.get()))
                >= state.exposure.maximum() && state.cooldown <= 0) {
            if (SculkSymbioteSonicBoom.fire(player, source)) {
                state.exposure.reset();
                state.cooldown = Math.max(0, Config.SCULK_SONIC_BOOM_COOLDOWN.get());
            }
        }
    }

    @Override
    public net.minecraft.tags.TagKey<GameEvent> getListenableEvents() {
        return GameEventTags.VIBRATIONS;
    }

    private void tickSourceStates() {
        int maxSources = Math.max(1, Config.SCULK_MAX_TRACKED_SOURCES.get());
        double radius = Math.max(1, Config.SCULK_VIBRATION_RADIUS.get());
        double radiusSqr = radius * radius;
        long timeout = Math.max(40L, maxSources * 2L);
        sources.entrySet().removeIf(entry -> {
            SourceState state = entry.getValue();
            state.cooldown = Math.max(0, state.cooldown - 1);
            Entity source = player.serverLevel().getEntity(entry.getKey());
            return source == null || source.isRemoved() || !source.isAlive()
                    || source.distanceToSqr(player) > radiusSqr
                    || currentTick - state.lastSeenTick > timeout;
        });
        if (sources.size() > maxSources) {
            sources.entrySet().stream()
                    .sorted(Comparator.comparingLong(entry -> entry.getValue().lastSeenTick))
                    .limit(sources.size() - maxSources)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(sources::remove);
        }
    }

    private static final class SourceState {
        private final SculkSymbioteExposure exposure;
        private long lastSeenTick;
        private int cooldown;

        private SourceState(int maximum) {
            this.exposure = new SculkSymbioteExposure(maximum);
        }
    }
}
