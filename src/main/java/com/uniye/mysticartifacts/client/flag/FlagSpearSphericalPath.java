package com.uniye.mysticartifacts.client.flag;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class FlagSpearSphericalPath {
    public static final double RADIUS = FlagSpearTrailProfile.RADIUS;
    public static final double MIN_ARC_LENGTH = RADIUS * 2.0D;
    private static final int MAX_ENDPOINT_ATTEMPTS = 64;
    private static final long TRAIL_SEED_SALT = 0x9E3779B97F4A7C15L;

    private FlagSpearSphericalPath() {
    }

    public static Vec3 endpoint(long seed, int trailIndex, boolean end) {
        return endpoints(seed, trailIndex)[end ? 1 : 0];
    }

    public static Vec3 point(long seed, int trailIndex, double progress, double age) {
        Vec3 start = endpoint(seed, trailIndex, false);
        Vec3 end = endpoint(seed, trailIndex, true);
        Vec3 startUnit = start.normalize();
        Vec3 endUnit = end.normalize();
        double clampedProgress = Math.max(0.0D, Math.min(1.0D, progress));
        double dot = Math.max(-1.0D, Math.min(1.0D, startUnit.dot(endUnit)));
        double angle = Math.acos(dot);

        Vec3 surface;
        if (angle < 1.0E-5D) {
            surface = startUnit.lerp(endUnit, clampedProgress).normalize();
        } else {
            double sine = Math.sin(angle);
            double firstWeight = Math.sin((1.0D - clampedProgress) * angle) / sine;
            double secondWeight = Math.sin(clampedProgress * angle) / sine;
            surface = startUnit.scale(firstWeight).add(endUnit.scale(secondWeight)).normalize();
        }

        return surface.scale(RADIUS);
    }

    public static double arcLength(Vec3 start, Vec3 end) {
        if (start.lengthSqr() < 1.0E-8D || end.lengthSqr() < 1.0E-8D) {
            return 0.0D;
        }
        double dot = Math.max(-1.0D, Math.min(1.0D,
                start.normalize().dot(end.normalize())));
        return RADIUS * Math.acos(dot);
    }

    private static Vec3[] endpoints(long seed, int trailIndex) {
        if (trailIndex < 0 || trailIndex >= FlagSpearTrailProfile.TRAIL_COUNT) {
            throw new IndexOutOfBoundsException("trail=" + trailIndex);
        }

        RandomSource random = RandomSource.create(seed ^ (TRAIL_SEED_SALT * (trailIndex + 1L)));
        Vec3 start = randomSpherePoint(random);
        Vec3 end = start;
        for (int attempt = 0; attempt < MAX_ENDPOINT_ATTEMPTS; attempt++) {
            end = randomSpherePoint(random);
            if (arcLength(start, end) >= MIN_ARC_LENGTH) {
                return new Vec3[]{start, end};
            }
        }
        return new Vec3[]{start, start.scale(-1.0D)};
    }

    private static Vec3 randomSpherePoint(RandomSource random) {
        double y = random.nextDouble() * 2.0D - 1.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        return new Vec3(
                Math.cos(angle) * horizontal * RADIUS,
                y * RADIUS,
                Math.sin(angle) * horizontal * RADIUS
        );
    }
}
