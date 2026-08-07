package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.event.AllSeeingEyeEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestPlayerListPacket {

    public RequestPlayerListPacket() {
    }

    public static void encode(RequestPlayerListPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestPlayerListPacket decode(FriendlyByteBuf buf) {
        return new RequestPlayerListPacket();
    }

    public static void handle(RequestPlayerListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                AllSeeingEyeEvents.sendPlayerList(sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToServer(RequestPlayerListPacket msg) {
        NetworkHandler.INSTANCE.sendToServer(msg);
    }
}
