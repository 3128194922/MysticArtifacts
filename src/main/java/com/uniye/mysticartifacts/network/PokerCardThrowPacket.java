package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.item.impl.PokerCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PokerCardThrowPacket {
    public PokerCardThrowPacket() {
    }

    public static void encode(PokerCardThrowPacket msg, FriendlyByteBuf buf) {
    }

    public static PokerCardThrowPacket decode(FriendlyByteBuf buf) {
        return new PokerCardThrowPacket();
    }

    public static void handle(PokerCardThrowPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() instanceof PokerCardItem) {
                    PokerCardItem.throwCard(player.level(), player, mainHand);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
