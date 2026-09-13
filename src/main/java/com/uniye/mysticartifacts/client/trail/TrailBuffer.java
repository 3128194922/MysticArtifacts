package com.uniye.mysticartifacts.client.trail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Bounded, time-expiring sample history for one client-side entity. */
public final class TrailBuffer {
    private final int maxSamples;
    private final long retentionTicks;
    private final Deque<TrailSample> samples = new ArrayDeque<>();

    public TrailBuffer(int maxSamples, long retentionTicks) {
        if (maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples must be positive");
        }
        if (retentionTicks < 0) {
            throw new IllegalArgumentException("retentionTicks cannot be negative");
        }
        this.maxSamples = maxSamples;
        this.retentionTicks = retentionTicks;
    }

    public void add(TrailSample sample) {
        prune(sample.tick());
        samples.addLast(sample);
        while (samples.size() > maxSamples) {
            samples.removeFirst();
        }
    }

    public void prune(long currentTick) {
        while (!samples.isEmpty() && currentTick - samples.peekFirst().tick() > retentionTicks) {
            samples.removeFirst();
        }
    }

    public List<TrailSample> samples() {
        return List.copyOf(samples);
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    public void clear() {
        samples.clear();
    }
}
