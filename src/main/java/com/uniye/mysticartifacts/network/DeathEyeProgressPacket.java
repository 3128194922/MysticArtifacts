package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/** 服务端同步指定目标的直死之魔眼斩杀值，或清空客户端缓存。 */
public class DeathEyeProgressPacket {
    private final UUID targetId;
    private final double accumulatedDamage;
    private final boolean clearAll;

    public DeathEyeProgressPacket(UUID targetId, double accumulatedDamage) {
        this.targetId = targetId;
        this.accumulatedDamage = accumulatedDamage;
        this.clearAll = false;
    }

    private DeathEyeProgressPacket(boolean clearAll) {
        this.targetId = null;
        this.accumulatedDamage = 0.0;
        this.clearAll = clearAll;
    }

    public static DeathEyeProgressPacket clearAll() {
        return new DeathEyeProgressPacket(true);
    }

    public static void encode(DeathEyeProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.clearAll);
        if (!msg.clearAll) {
            buf.writeUUID(msg.targetId);
            buf.writeDouble(msg.accumulatedDamage);
        }
    }

    public static DeathEyeProgressPacket decode(FriendlyByteBuf buf) {
        boolean clearAll = buf.readBoolean();
        if (clearAll) return clearAll();
        return new DeathEyeProgressPacket(buf.readUUID(), buf.readDouble());
    }

    public static void handle(DeathEyeProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleDeathEyeProgressSync(
                        msg.targetId, msg.accumulatedDamage, msg.clearAll)));
        ctx.get().setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, UUID targetId, double accumulatedDamage) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new DeathEyeProgressPacket(targetId, accumulatedDamage));
    }

    public static void clearClient(ServerPlayer player) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), clearAll());
    }
}
