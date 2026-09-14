package com.uniye.mysticartifacts.sculk;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** 使用原版伤害源、粒子、音效和击退参数复现监守者音爆。 */
public final class SculkSymbioteSonicBoom {
    private SculkSymbioteSonicBoom() {
    }

    public static boolean fire(ServerPlayer player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level) || target.level() != level
                || target.isRemoved() || !target.isAlive()) return false;

        Vec3 origin = player.position().add(0.0D, 1.6D, 0.0D);
        Vec3 targetPosition = target.getEyePosition();
        if (!player.closerThan(target, 15.0D, 20.0D)) return false;
        Vec3 direction = targetPosition.subtract(origin);
        double distance = direction.length();
        if (distance < 0.001D) return false;
        direction = direction.scale(1.0D / distance);

        for (int step = 1; step < (int) Math.floor(distance) + 7; step++) {
            Vec3 particlePosition = origin.add(direction.scale(step));
            level.sendParticles(ParticleTypes.SONIC_BOOM, particlePosition.x, particlePosition.y,
                    particlePosition.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 3.0F, 1.0F);
        boolean damaged = target.hurt(level.damageSources().sonicBoom(player), 10.0F);

        double resistance = Math.min(1.0D, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        double horizontal = 2.5D * (1.0D - resistance);
        double vertical = 0.5D * (1.0D - resistance);
        if (damaged) {
            target.push(direction.x * horizontal, direction.y * vertical, direction.z * horizontal);
        }
        return true;
    }
}
