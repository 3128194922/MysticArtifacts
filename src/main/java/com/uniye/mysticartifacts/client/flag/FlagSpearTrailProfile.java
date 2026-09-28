package com.uniye.mysticartifacts.client.flag;

public final class FlagSpearTrailProfile {
    public static final int TRAIL_COUNT = 5;
    public static final int SAMPLES_PER_TRAIL = 24;
    public static final double RADIUS = 5.0D;
    public static final double MOVING_TRAIL_LENGTH = 0.42D;

    private FlagSpearTrailProfile() {
    }

    public static double radiusAt(int sample) {
        if (sample < 0 || sample >= SAMPLES_PER_TRAIL) {
            throw new IndexOutOfBoundsException("sample=" + sample);
        }
        return RADIUS;
    }

    public static double phaseFor(int trail) {
        if (trail < 0 || trail >= TRAIL_COUNT) {
            throw new IndexOutOfBoundsException("trail=" + trail);
        }
        return Math.PI * 2.0D * trail / TRAIL_COUNT;
    }

    public static double verticalWave(int trail, int sample, long age) {
        double progress = sample / (double) (SAMPLES_PER_TRAIL - 1);
        return Math.sin(progress * Math.PI * 4.0D + trail * 0.93D + age * 0.42D)
                * (0.24D + trail * 0.025D);
    }

    public static double halfWidth(int trail, int sample, long age) {
        return halfWidth(trail, sample, (double) age);
    }

    public static double halfWidth(int trail, int sample, double age) {
        double progress = sample / (double) (SAMPLES_PER_TRAIL - 1);
        return 0.10D + 0.035D * Math.sin(progress * Math.PI * 2.0D + trail * 0.7D + age * 0.2D);
    }
}
