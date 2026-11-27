package com.uniye.mysticartifacts.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 伪爆炸：不调用原版 {@code Level#explode}，因此不破坏方块、不点燃火焰、
 * 也不会触发其它 mod 的爆炸事件。伤害由 AABB 实体检索 + 距离衰减自行结算，
 * 视觉上只补爆炸粒子与音效。
 */
public final class PseudoExplosion {
    /** 与原版爆炸伤害公式保持一致的系数：中心 7r+1，边缘 1。 */
    private static final double DAMAGE_SCALE = 7.0D;
    /** 爆炸击退的最大水平速度（格/tick），与原版 (1 - 距离/半径) 衰减一致。 */
    private static final double KNOCKBACK_SCALE = 1.0D;

    private PseudoExplosion() {
    }

    /**
     * 原版爆炸伤害公式，只是把方块遮挡的暴露度固定为 1：
     * {@code (k * k + k) / 2 * 7 * radius + 1}，其中 {@code k = 1 - distance / radius}。
     */
    public static float damageAt(double distance, float radius) {
        if (radius <= 0.0F) return 0.0F;
        double falloff = 1.0D - Math.min(1.0D, Math.max(0.0D, distance / radius));
        return (float) ((falloff * falloff + falloff) * 0.5D * DAMAGE_SCALE * radius + 1.0D);
    }

    /**
     * 在指定位置结算一次伪爆炸。
     *
     * @param level        服务端世界
     * @param source       爆炸来源实体，不会被自己炸到；可为 null
     * @param damageSource 范围伤害使用的伤害源
     * @param x            爆炸中心
     * @param y            爆炸中心
     * @param z            爆炸中心
     * @param radius       伤害半径（球体，AABB 只是检索范围）
     */
    public static void detonate(Level level, @Nullable Entity source, DamageSource damageSource,
                                double x, double y, double z, float radius) {
        if (level.isClientSide() || radius <= 0.0F) return;

        Vec3 center = new Vec3(x, y, z);
        AABB searchArea = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<Entity> targets = new ArrayList<>();

        for (Entity candidate : level.getEntities(source, searchArea, e -> canAffect(source, e, damageSource))) {
            Entity target = resolvePart(candidate);
            if (target == null || target == source || targets.contains(target)) continue;
            targets.add(target);
        }

        for (Entity target : targets) {
            AABB box = target.getBoundingBox();
            double distance = Math.sqrt(box.distanceToSqr(center));
            if (distance > radius) continue;

            float damage = damageAt(distance, radius);
            if (damage <= 0.0F) continue;
            if (!target.hurt(damageSource, damage)) continue;

            applyKnockback(target, box.getCenter().subtract(center), distance, radius);
        }

        spawnEffects(level, x, y, z, radius);
    }

    private static boolean canAffect(@Nullable Entity source, Entity entity, DamageSource damageSource) {
        if (entity.isRemoved() || entity.isSpectator()) return false;
        if (entity == source) return false;
        if (entity.isInvulnerableTo(damageSource)) return false;
        return !(source instanceof Player attacker)
                || !(entity instanceof Player victim)
                || attacker.canHarmPlayer(victim);
    }

    @Nullable
    private static Entity resolvePart(@Nullable Entity entity) {
        if (entity instanceof PartEntity<?> partEntity) {
            return partEntity.getParent();
        }
        return entity;
    }

    private static void applyKnockback(Entity target, Vec3 offset, double distance, float radius) {
        double falloff = 1.0D - Math.min(1.0D, distance / radius);
        if (falloff <= 0.0D) return;

        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        double length = horizontal.length();
        if (length < 1.0E-4D) return;

        double strength = KNOCKBACK_SCALE * falloff;
        horizontal = horizontal.scale(1.0D / length);
        target.push(horizontal.x * strength, 0.35D * falloff, horizontal.z * strength);
        if (target instanceof Player player) {
            player.hurtMarked = true;
        }
    }

    private static void spawnEffects(Level level, double x, double y, double z, float radius) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4,
                radius * 0.3D, radius * 0.3D, radius * 0.3D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12,
                radius * 0.35D, radius * 0.35D, radius * 0.35D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 8,
                radius * 0.3D, radius * 0.3D, radius * 0.3D, 0.03D);

        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
    }
}
