package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.entity.SwordPhantomEntity;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.SwordSwarmCharm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
                    // 攻击消耗1把储存剑，储存为0时无法发射
                    if (!SwordSwarmCharm.consumeStoredSword(charm)) {
                        return;
                    }
                    SwordSwarmCharm.seedQueue(charm, player.level());
                    net.minecraft.resources.ResourceLocation nextId = SwordSwarmCharm.popNextAndAppendRandom(charm, player.level());
                    if (nextId != null) {
                        ItemStack visual = new ItemStack(ForgeRegistries.ITEMS.getValue(nextId));
                        SwordPhantomEntity entity = new SwordPhantomEntity(ModEntities.SWORD_PHANTOM.get(), player.level(), player);
                        entity.setVisualItem(visual);
                        entity.setBaseDamage(Math.max(1, SwordSwarmCharm.getDevouredCount(charm)));
                        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                        // 抛射体发射位置位于玩家左右后方交替
                        CompoundTag charmTag = charm.getOrCreateTag();
                        boolean spawnRight = charmTag.getBoolean("SwarmNextRight"); // 默认 false -> 先左侧
                        charmTag.putBoolean("SwarmNextRight", !spawnRight); // 切换下一次的发射侧
                        Vec3 look = player.getLookAngle();
                        Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
                        if (forward.lengthSqr() < 1.0E-6D) {
                            forward = new Vec3(0.0D, 0.0D, 1.0D);
                        }
                        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize(); // 玩家右手方向
                        Vec3 rear = forward.scale(-1.0D); // 玩家正后方
                        double sideOffset = 0.8D;
                        double rearOffset = 0.6D;
                        double heightOffset = -0.2D;
                        Vec3 side = right.scale(spawnRight ? sideOffset : -sideOffset);
                        Vec3 offset = side.add(rear.scale(rearOffset));
                        entity.setPos(
                                player.getX() + offset.x,
                                player.getEyeY() + heightOffset,
                                player.getZ() + offset.z
                        );
                        // 生成时立即朝向飞行方向，避免初始朝向错误后由tick纠正
                        Vec3 motion = entity.getDeltaMovement();
                        double hDist = motion.horizontalDistance();
                        if (hDist > 1.0E-4D) {
                            entity.setYRot((float) (Math.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
                            entity.setXRot((float) (Math.atan2(motion.y, hDist) * (180.0D / Math.PI)));
                        }
                        entity.yRotO = entity.getYRot();
                        entity.xRotO = entity.getXRot();
                        player.level().addFreshEntity(entity);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
