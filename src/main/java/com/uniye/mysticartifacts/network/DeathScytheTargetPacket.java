package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.deathscythe.DeathScytheTargetClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 同步死神镰刀当前记录的实体 UUID，不把伤害判定交给客户端。 */
public final class DeathScytheTargetPacket {
    private final UUID targetUUID;

    public DeathScytheTargetPacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public static DeathScytheTargetPacket clear() {
        return new DeathScytheTargetPacket(null);
    }

    public static void encode(DeathScytheTargetPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.targetUUID != null);
        if (message.targetUUID != null) {
            buffer.writeUUID(message.targetUUID);
        }
    }

    public static DeathScytheTargetPacket decode(FriendlyByteBuf buffer) {
        return new DeathScytheTargetPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(DeathScytheTargetPacket message,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DeathScytheTargetClientState.setTarget(message.targetUUID)));
        context.setPacketHandled(true);
    }
}
