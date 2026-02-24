package com.uniye.mysticartifacts.util;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class DeathEyeCutLine {

    public static final int PERIOD_TICKS = 40;

    private DeathEyeCutLine() {
    }

    public static CutLine compute(LivingEntity target, long gameTime) {
        long segment = gameTime / PERIOD_TICKS;
        UUID id = target.getUUID();
        long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ (segment * 0x9E3779B97F4A7C15L);
        RandomSource random = RandomSource.create(seed);

        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();

        double angle = random.nextDouble() * (Math.PI * 2.0);
        double tilt = (random.nextDouble() - 0.5) * 0.6;

        Vec3 dir = new Vec3(Math.cos(angle), tilt, Math.sin(angle)).normalize();

        double y = Mth.lerp(0.15 + random.nextDouble() * 0.7, box.minY, box.maxY);
        Vec3 anchor = new Vec3(center.x, y, center.z);

        double maxSize = Math.max(target.getBbWidth(), target.getBbHeight());
        // Scale based on entity size to ensure visibility
        double halfLen = maxSize * (1.2 + random.nextDouble() * 0.5);
        Vec3 from = anchor.add(dir.scale(halfLen));
        Vec3 to = anchor.subtract(dir.scale(halfLen));

        // Always render as red
        int color = 0xFF0000;

        return new CutLine(from, to, color);
    }

    public record CutLine(Vec3 from, Vec3 to, int color) {
    }
}
