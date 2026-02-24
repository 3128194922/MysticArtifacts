package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.entity.SwordPhantomEntity;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.SwordSwarmCharm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SwordSwarmAttackPacket {

    public SwordSwarmAttackPacket() {
    }

    public static void encode(SwordSwarmAttackPacket msg, FriendlyByteBuf buf) {
    }

    public static SwordSwarmAttackPacket decode(FriendlyByteBuf buf) {
        return new SwordSwarmAttackPacket();
    }

    public static void handle(SwordSwarmAttackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack charm = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                        .map(handler -> {
                            java.util.List<top.theillusivec4.curios.api.SlotResult> results = handler.findCurios(ModItems.SWORD_SWARM_CHARM.get());
                            return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack();
                        })
                        .orElse(ItemStack.EMPTY);
                if (!charm.isEmpty()) {
                    SwordSwarmCharm.seedQueue(charm, player.level());
                    net.minecraft.resources.ResourceLocation nextId = SwordSwarmCharm.popNextAndAppendRandom(charm, player.level());
                    if (nextId != null) {
                        ItemStack visual = new ItemStack(ForgeRegistries.ITEMS.getValue(nextId));
                        SwordPhantomEntity entity = new SwordPhantomEntity(ModEntities.SWORD_PHANTOM.get(), player.level(), player);
                        entity.setVisualItem(visual);
                        entity.setBaseDamage(Math.max(1, SwordSwarmCharm.getDevouredCount(charm)));
                        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                        player.level().addFreshEntity(entity);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
