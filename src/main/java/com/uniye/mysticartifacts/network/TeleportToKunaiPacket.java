package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.entity.EnderKunaiEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeleportToKunaiPacket {
    private final int entityId;

    public TeleportToKunaiPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(TeleportToKunaiPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static TeleportToKunaiPacket decode(FriendlyByteBuf buf) {
        return new TeleportToKunaiPacket(buf.readInt());
    }

    public static void handle(TeleportToKunaiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity entity = player.level().getEntity(msg.entityId);
                
                double destX = 0, destY = 0, destZ = 0;
                boolean shouldTeleport = false;
                
                if (entity instanceof EnderKunaiEntity kunai) {
                    destX = kunai.getX();
                    destY = kunai.getY();
                    destZ = kunai.getZ();
                    
                    if (player.position().distanceTo(new net.minecraft.world.phys.Vec3(destX, destY, destZ)) <= Config.EnderKunaiMaxDistance) {
                        shouldTeleport = true;
                        
                        ItemStack itemStack = kunai.getPickupItem();
                        if (!player.getInventory().add(itemStack)) {
                            player.drop(itemStack, false);
                        }
                        player.take(kunai, 1);
                        kunai.discard();
                    }
                }

                if (shouldTeleport) {
                    player.teleportTo(destX, destY, destZ);

                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, destX, destY, destZ, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                        serverLevel.sendParticles(ParticleTypes.PORTAL, destX, destY, destZ, 16, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
