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

public class SpectateStatePacket {

    private final UUID target;

    public SpectateStatePacket(UUID target) {
        this.target = target;
    }

    public static void encode(SpectateStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.target != null);
        if (msg.target != null) {
            buf.writeUUID(msg.target);
        }
    }

    public static SpectateStatePacket decode(FriendlyByteBuf buf) {
        return new SpectateStatePacket(buf.readBoolean() ? buf.readUUID() : null);
    }

    public static void handle(SpectateStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientPacketHandler.handleSpectateState(msg.target)));
        ctx.get().setPacketHandled(true);
    }

    public static void sendStart(ServerPlayer watcher, UUID target) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> watcher), new SpectateStatePacket(target));
    }

    public static void sendStop(ServerPlayer watcher) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> watcher), new SpectateStatePacket(null));
    }
}
