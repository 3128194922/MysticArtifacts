package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.event.AllSeeingEyeEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SelectSpectatePacket {

    private final UUID target;

    public SelectSpectatePacket(UUID target) {
        this.target = target;
    }

    public static void encode(SelectSpectatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.target != null);
        if (msg.target != null) {
            buf.writeUUID(msg.target);
        }
    }

    public static SelectSpectatePacket decode(FriendlyByteBuf buf) {
        boolean hasTarget = buf.readBoolean();
        UUID target = hasTarget ? buf.readUUID() : null;
        return new SelectSpectatePacket(target);
    }

    public static void handle(SelectSpectatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                if (msg.target == null) {
                    AllSeeingEyeEvents.stopSpectate(sender, true);
                } else {
                    AllSeeingEyeEvents.startSpectate(sender, msg.target);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToServer(SelectSpectatePacket msg) {
        NetworkHandler.INSTANCE.sendToServer(msg);
    }
}
