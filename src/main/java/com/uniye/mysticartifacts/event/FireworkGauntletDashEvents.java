package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.item.impl.FireworkGauntletItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class FireworkGauntletDashEvents {
    private static final double DASH_SPEED = 1.6D;
    private static final int IMPACT_KNOCKBACK_TICKS = 10;
    private static final double IMPACT_KNOCKBACK_SPEED = 2.0D;

    private static final String TAG_IMPACT_TICKS = "FireworkGauntletImpactTicks";
    private static final String TAG_IMPACT_DX = "FireworkGauntletImpactDx";
    private static final String TAG_IMPACT_DZ = "FireworkGauntletImpactDz";
    private static final String TAG_IMPACT_DAMAGE = "FireworkGauntletImpactDamage";
    private static final String TAG_IMPACT_SOURCE_ID = "FireworkGauntletImpactSourceId";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int remainTicks = data.getInt(FireworkGauntletItem.TAG_DASH_TICKS);
        if (remainTicks <= 0) {
            return;
        }

        Vec3 direction = new Vec3(
                data.getDouble(FireworkGauntletItem.TAG_DASH_X),
                data.getDouble(FireworkGauntletItem.TAG_DASH_Y),
                data.getDouble(FireworkGauntletItem.TAG_DASH_Z)
        ).normalize();

        int flight = Math.max(1, data.getInt(FireworkGauntletItem.TAG_DASH_FLIGHT));
        Set<Integer> hitIds = toSet(data.getIntArray(FireworkGauntletItem.TAG_DASH_HIT_IDS));

        AABB collideBox = player.getBoundingBox().inflate(0.6D, 0.3D, 0.6D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, collideBox);
        targets.remove(player);

        for (LivingEntity target : targets) {
            if (hitIds.contains(target.getId())) {
                continue;
            }

            DamageSource punch = ModDamageTypes.getSource(player.level(), ModDamageTypes.ROCKET_PUNCH, player, player);
            float damage = 4.0F * flight;
            target.hurt(punch, damage);
            startImpactKnockback(target, direction, damage, player.getId());
            hitIds.add(target.getId());
        }

        if (!hitIds.isEmpty()) {
            stopDash(player, data);
            return;
        }

        Vec3 dashVelocity = direction.scale(DASH_SPEED);
        player.setDeltaMovement(dashVelocity.x, dashVelocity.y + 0.1D, dashVelocity.z);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        remainTicks--;
        if (remainTicks <= 0 || player.horizontalCollision) {
            stopDash(player, data);
        } else {
            data.putInt(FireworkGauntletItem.TAG_DASH_TICKS, remainTicks);
            data.putIntArray(FireworkGauntletItem.TAG_DASH_HIT_IDS, hitIds.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }

        CompoundTag data = target.getPersistentData();
        int impactTicks = data.getInt(TAG_IMPACT_TICKS);
        if (impactTicks <= 0) {
            return;
        }

        if (target.horizontalCollision) {
            Entity source = data.contains(TAG_IMPACT_SOURCE_ID, 3) ? target.level().getEntity(data.getInt(TAG_IMPACT_SOURCE_ID)) : null;
            DamageSource wallSlam = getWallSlamSource(source, target);
            target.hurt(wallSlam, data.getFloat(TAG_IMPACT_DAMAGE));
            clearImpactKnockback(data);
            return;
        }

        target.setDeltaMovement(
                data.getDouble(TAG_IMPACT_DX) * IMPACT_KNOCKBACK_SPEED,
                0.0D,
                data.getDouble(TAG_IMPACT_DZ) * IMPACT_KNOCKBACK_SPEED
        );
        target.hurtMarked = true;

        impactTicks--;
        if (impactTicks <= 0) {
            clearImpactKnockback(data);
        } else {
            data.putInt(TAG_IMPACT_TICKS, impactTicks);
        }
    }

    private static Set<Integer> toSet(int[] ids) {
        Set<Integer> result = new HashSet<>();
        for (int id : ids) {
            result.add(id);
        }
        return result;
    }

    private static void startImpactKnockback(LivingEntity target, Vec3 direction, float damage, int sourceId) {
        CompoundTag data = target.getPersistentData();
        data.putInt(TAG_IMPACT_TICKS, IMPACT_KNOCKBACK_TICKS);
        data.putDouble(TAG_IMPACT_DX, direction.x);
        data.putDouble(TAG_IMPACT_DZ, direction.z);
        data.putFloat(TAG_IMPACT_DAMAGE, damage);
        data.putInt(TAG_IMPACT_SOURCE_ID, sourceId);
    }

    private static void clearImpactKnockback(CompoundTag data) {
        data.remove(TAG_IMPACT_TICKS);
        data.remove(TAG_IMPACT_DX);
        data.remove(TAG_IMPACT_DZ);
        data.remove(TAG_IMPACT_DAMAGE);
        data.remove(TAG_IMPACT_SOURCE_ID);
    }

    private static DamageSource getWallSlamSource(Entity source, LivingEntity target) {
        if (source != null) {
            return ModDamageTypes.getSource(target.level(), ModDamageTypes.WALL_SLAM, source, source);
        }
        return ModDamageTypes.getSimpleDamageSource(target.level(), ModDamageTypes.WALL_SLAM);
    }

    private static void stopDash(Player player, CompoundTag data) {
        data.remove(FireworkGauntletItem.TAG_DASH_TICKS);
        data.remove(FireworkGauntletItem.TAG_DASH_FLIGHT);
        data.remove(FireworkGauntletItem.TAG_DASH_X);
        data.remove(FireworkGauntletItem.TAG_DASH_Y);
        data.remove(FireworkGauntletItem.TAG_DASH_Z);
        data.remove(FireworkGauntletItem.TAG_DASH_HIT_IDS);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }
}
