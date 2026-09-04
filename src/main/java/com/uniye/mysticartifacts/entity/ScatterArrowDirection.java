package com.uniye.mysticartifacts.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

final class ScatterArrowDirection {
    private ScatterArrowDirection() {
    }

    static Vec3 create(RandomSource random, int index, int pointCount) {
        float turnFraction = (1.0F + (float) Math.sqrt(5.0)) / 2.0F;
        double fullness = 0.8;
        double dst = index / (double) pointCount;
        double inclination = Math.acos(1.0 - fullness * dst);
        double azimuth = 2.0 * Math.PI * (random.nextFloat() + turnFraction * index);

        Vec3 direction = new Vec3(
                Math.sin(inclination) * Math.cos(azimuth),
                Math.cos(inclination),
                Math.sin(inclination) * Math.sin(azimuth)
        );

        if (index == 1) {
            direction = direction.add(0.0, 1.0, 0.0).scale(0.5);
        }
        return direction.scale(0.35);
    }
}
