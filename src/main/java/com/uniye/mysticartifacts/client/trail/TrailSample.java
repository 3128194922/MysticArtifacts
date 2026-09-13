package com.uniye.mysticartifacts.client.trail;

/** Immutable client-side position sample used by the trail renderer. */
public record TrailSample(
        double x,
        double y,
        double z,
        double directionX,
        double directionZ,
        long tick
) {
}
