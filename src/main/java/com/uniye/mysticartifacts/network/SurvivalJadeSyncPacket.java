package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步佩戴者当前的残影量，用于血条 HUD 渲染。
 */
public class SurvivalJadeSyncPacket {
    private final float phantom;

    public SurvivalJadeSyncPacket(float phantom) {
        this.phantom = phantom;
    }

    public static void encode(SurvivalJadeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.phantom);
    }

    public static SurvivalJadeSyncPacket decode(FriendlyByteBuf buf) {
        return new SurvivalJadeSyncPacket(buf.readFloat());
    }

    public static void handle(SurvivalJadeSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSurvivalJadeSync(msg.phantom));
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, float phantom) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SurvivalJadeSyncPacket(phantom));
    }

    public float getPhantom() {
        return phantom;
    }
}
