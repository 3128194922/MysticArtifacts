package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.item.impl.TwoDragonsPlayBallItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TwoDragonsThrowPacket {
    public TwoDragonsThrowPacket() {
    }

    public static void encode(TwoDragonsThrowPacket msg, FriendlyByteBuf buf) {
    }

    public static TwoDragonsThrowPacket decode(FriendlyByteBuf buf) {
        return new TwoDragonsThrowPacket();
    }

    public static void handle(TwoDragonsThrowPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() instanceof TwoDragonsPlayBallItem) {
                    TwoDragonsPlayBallItem.throwFan(player.level(), player, mainHand);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
