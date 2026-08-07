package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.network.PlayerListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
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
public class AllSeeingEyeEvents {

    private static final Map<UUID, SpectateInfo> SPECTATES = new HashMap<>();

    private record SpectateInfo(
            UUID target,
            ResourceKey<Level> originDim,
            double originX,
            double originY,
            double originZ,
            float originYRot,
            float originXRot,
            GameType originGameType) {
    }

    public static void sendPlayerList(ServerPlayer requester) {
        List<PlayerListPacket.Entry> players = new ArrayList<>();
        for (ServerPlayer player : requester.server.getPlayerList().getPlayers()) {
            if (player != requester) {
                players.add(new PlayerListPacket.Entry(player.getUUID(), player.getGameProfile().getName()));
            }
        }
        PlayerListPacket.sendTo(requester, players);
    }

    public static void startSpectate(ServerPlayer watcher, UUID targetUuid) {
        ServerPlayer target = watcher.server.getPlayerList().getPlayer(targetUuid);
        if (target == null || target == watcher || !target.isAlive()) {
            return;
        }
        if (SPECTATES.containsKey(targetUuid)) {
            return;
        }

        stopSpectate(watcher, true);

        SpectateInfo info = new SpectateInfo(
                targetUuid,
                watcher.level().dimension(),
                watcher.getX(), watcher.getY(), watcher.getZ(),
                watcher.getYRot(), watcher.getXRot(),
                watcher.gameMode.getGameModeForPlayer()
        );
        SPECTATES.put(watcher.getUUID(), info);

        watcher.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
        teleportToTarget(watcher, target);
        watcher.setCamera(target);
    }

    public static void stopSpectate(ServerPlayer watcher, boolean teleportBack) {
        SpectateInfo info = SPECTATES.remove(watcher.getUUID());
        if (info == null) {
            return;
        }

        if (watcher.isAlive()) {
            watcher.gameMode.changeGameModeForPlayer(info.originGameType());
            watcher.setCamera(watcher);
        }

        if (teleportBack && watcher.isAlive()) {
            ServerLevel origin = watcher.server.getLevel(info.originDim());
            if (origin != null) {
                watcher.teleportTo(
                        origin, info.originX(), info.originY(), info.originZ(),
                        info.originYRot(), info.originXRot()
                );
            }
        }
    }

    private static void teleportToTarget(ServerPlayer watcher, ServerPlayer target) {
        watcher.teleportTo(
                target.serverLevel(),
                target.getX(), target.getY() + 1.0D, target.getZ(),
                target.getYRot(), target.getXRot()
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (UUID watcherId : new ArrayList<>(SPECTATES.keySet())) {
            SpectateInfo info = SPECTATES.get(watcherId);
            if (info == null) continue;

            ServerPlayer watcher = event.getServer().getPlayerList().getPlayer(watcherId);
            if (watcher == null) {
                SPECTATES.remove(watcherId);
                continue;
            }
            if (!watcher.isAlive()) {
                stopSpectate(watcher, false);
                continue;
            }

            ServerPlayer target = event.getServer().getPlayerList().getPlayer(info.target());
            if (target == null || !target.isAlive()) {
                stopSpectate(watcher, true);
                continue;
            }

            if (watcher.level() != target.level()) {
                stopSpectate(watcher, true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                stopSpectate(player, true);
            } catch (Exception e) {
                SPECTATES.remove(player.getUUID());
            }
        }
    }
}