package com.uniye.mysticartifacts.client.render;

public final class KatanaSlashRenderProfile {
    private static final int NEAR_SEGMENTS = 40;
    private static final int FAR_SEGMENTS = 24;
    private static final double FAR_LOD_DISTANCE_SQR = 16.0D * 16.0D;
    private static final Sample[] NEAR_ENDPOINTS = buildSamples(NEAR_SEGMENTS, false);
    private static final Sample[] FAR_ENDPOINTS = buildSamples(FAR_SEGMENTS, false);
    private static final Sample[] NEAR_MIDPOINTS = buildSamples(NEAR_SEGMENTS, true);
    private static final Sample[] FAR_MIDPOINTS = buildSamples(FAR_SEGMENTS, true);

    private KatanaSlashRenderProfile() {
    }

    public static int segmentsForDistanceSqr(double distanceSqr) {
        return distanceSqr <= FAR_LOD_DISTANCE_SQR ? NEAR_SEGMENTS : FAR_SEGMENTS;
    }

    public static int nearSegments() {
        return NEAR_SEGMENTS;
    }

    public static int farSegments() {
        return FAR_SEGMENTS;
    }

    public static Sample endpoint(int segments, int index) {
        return (segments == FAR_SEGMENTS ? FAR_ENDPOINTS : NEAR_ENDPOINTS)[index];
    }

    public static Sample midpoint(int segments, int index) {
        return (segments == FAR_SEGMENTS ? FAR_MIDPOINTS : NEAR_MIDPOINTS)[index];
    }

    private static Sample[] buildSamples(int segments, boolean midpoint) {
        int count = midpoint ? segments : segments + 1;
        Sample[] samples = new Sample[count];
        for (int index = 0; index < count; index++) {
            float progress = midpoint
                    ? (index + 0.5F) / segments
                    : index / (float) segments;
            double angle = Math.toRadians(-90.0D + 180.0D * progress);
            double edge = Math.sin(Math.PI * progress);
            samples[index] = new Sample(
                    progress,
                    (float) Math.sin(angle),
                    (float) Math.cos(angle),
                    (float) Math.pow(edge, 0.68D),
                    (float) edge,
                    (float) (Math.sin(Math.PI * 11.0D * progress + 0.35D) * edge)
            );
        }
        return samples;
    }

    public record Sample(float progress, float sinAngle, float cosAngle,
                         float edgeAlpha, float thicknessFactor, float spikeFactor) {
    }
}
