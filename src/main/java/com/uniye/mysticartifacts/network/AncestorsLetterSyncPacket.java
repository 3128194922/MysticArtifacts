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
 * 服务端 -> 客户端：同步先祖的信当前状态（0=正常 1=美德 2=折磨），用于 HUD 图标渲染。
 */
public class AncestorsLetterSyncPacket {
    private final int state;

    public AncestorsLetterSyncPacket(int state) {
        this.state = state;
    }

    public static void encode(AncestorsLetterSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.state);
    }

    public static AncestorsLetterSyncPacket decode(FriendlyByteBuf buf) {
        return new AncestorsLetterSyncPacket(buf.readByte());
    }

    public static void handle(AncestorsLetterSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleAncestorsLetterSync(msg.state));
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, int state) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new AncestorsLetterSyncPacket(state));
    }

    public int getState() {
        return state;
    }
}
