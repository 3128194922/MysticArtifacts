package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 充能同步：服务端在充能后把「玩家物品栏槽位 + 充能层数」发给该玩家，
 * 让客户端的充能条与后续判定立即可见，不必等服务端周期性同步槽位。
 * 槽位约定与 Inventory.items 下标一致：0-8 主快捷栏，9-35 主背包。
 */
public final class FlagSpearChargeSyncPacket {
    private final int slot;
    private final int charge;
    private final int maxCharge;
    private final boolean valid;

    public FlagSpearChargeSyncPacket(int slot, int charge, int maxCharge) {
        this.slot = slot;
        this.charge = charge;
        this.maxCharge = maxCharge;
        this.valid = slot >= 0 && slot < 36 && charge >= 0 && maxCharge > 0;
    }

    public static void encode(FlagSpearChargeSyncPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.slot);
        buffer.writeVarInt(message.charge);
        buffer.writeVarInt(message.maxCharge);
    }

    public static FlagSpearChargeSyncPacket decode(FriendlyByteBuf buffer) {
        return new FlagSpearChargeSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(FlagSpearChargeSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (!message.valid) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleFlagSpearCharge(message.slot, message.charge, message.maxCharge)
        ));
        context.setPacketHandled(true);
    }

    /**
     * 把充能层数同步给持有该物品的玩家。
     */
    public static void send(Player player, int slot, int charge, int maxCharge) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new FlagSpearChargeSyncPacket(slot, charge, maxCharge)
        );
    }

    /**
     * 查找要充能的旗枪槽位：主手优先，其次背包内第一把旗枪。
     *
     * @return 与 Inventory.items 下标一致的槽位，未找到返回 -1
     */
    public static int findChargeSlot(Player player, Item spearItem) {
        if (player.getMainHandItem().is(spearItem)) {
            return player.getInventory().selected;
        }

        for (int i = 0; i < 36; i++) {
            if (player.getInventory().items.get(i).is(spearItem)) {
                return i;
            }
        }
        return -1;
    }
}
