package com.uniye.mysticartifacts.sculk;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import com.uniye.mysticartifacts.network.SculkSymbioteVibrationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SculkSymbioteServerHandler {
    private static final Map<UUID, SculkSymbioteSensor> SENSORS = new HashMap<>();

    private SculkSymbioteServerHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level) || !TrailSightItem.isWearing(player)) {
            remove(player.getUUID());
            return;
        }

        SculkSymbioteSensor sensor = SENSORS.get(player.getUUID());
        if (sensor == null || !sensor.isFor(player)) {
            if (sensor != null) sensor.remove();
            sensor = new SculkSymbioteSensor(player);
            SENSORS.put(player.getUUID(), sensor);
        }
        sensor.tick(level);
        List<MarkerData> markers = sensor.drainMarkers();
        SculkSymbioteVibrationPacket.sendTo(player, markers, sensor.maxExposure(), sensor.exposureMaximum());
    }

    @SubscribeEvent
    public static void onEntitySound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Entity source = event.getEntity();
        for (SculkSymbioteSensor sensor : SENSORS.values()) {
            sensor.recordEntitySound(source);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            remove(player.getUUID());
        }
    }

    private static void remove(UUID playerId) {
        SculkSymbioteSensor sensor = SENSORS.remove(playerId);
        if (sensor != null) sensor.remove();
    }

    public static final class MarkerData {
        private final Vec3 position;
        private final int frequency;
        private final UUID sourceId;

        public MarkerData(Vec3 position, int frequency, UUID sourceId) {
            this.position = position;
            this.frequency = frequency;
            this.sourceId = sourceId;
        }

        public Vec3 position() { return position; }
        public int frequency() { return frequency; }
        public UUID sourceId() { return sourceId; }
    }
}
