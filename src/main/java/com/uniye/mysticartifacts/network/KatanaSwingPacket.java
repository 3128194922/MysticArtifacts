package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端空挥开鞘左键请求；真正的生成与伤害在服务器执行。 */
public class KatanaSwingPacket {
    public KatanaSwingPacket() {
    }

    public static void encode(KatanaSwingPacket message, FriendlyByteBuf buffer) {
    }

    public static KatanaSwingPacket decode(FriendlyByteBuf buffer) {
        return new KatanaSwingPacket();
    }

    public static void handle(KatanaSwingPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MuramasaItem.triggerOpenSlash(player);
            }
        });
        context.setPacketHandled(true);
    }
}
