package com.uniye.mysticartifacts.util;

import com.uniye.mysticartifacts.network.SpawnTextIndicatorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleTextAPI {
    public static void send(ServerPlayer player, Vec3 pos, String text, int color) {
        SpawnTextIndicatorPacket.sendTo(player, pos, text, color, true, 0x003300, 1.0F);
    }

    public static void send(ServerPlayer player, Vec3 pos, String text, int color, boolean outline, int outlineColor, float size) {
        SpawnTextIndicatorPacket.sendTo(player, pos, text, color, outline, outlineColor, size);
    }

    public static void sendInFront(ServerPlayer player, String text, int color) {
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        SpawnTextIndicatorPacket.sendTo(player, pos, text, color, true, 0x003300, 1.0F);
    }
}
