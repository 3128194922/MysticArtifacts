package com.uniye.mysticartifacts.sculk;

/** 单个震动来源对佩戴者造成的暴露值。该类不持有实体引用，便于服务端按 UUID 管理和回收。 */
public final class SculkSymbioteExposure {
    private final int maximum;
    private int value;

    public SculkSymbioteExposure(int maximum) {
        this.maximum = Math.max(1, maximum);
    }

    public int add(int amount) {
        value = clamp((long) value + Math.max(0, amount));
        return value;
    }

    public void set(int value) {
        this.value = clamp(value);
    }

    public int value() {
        return value;
    }

    public int maximum() {
        return maximum;
    }

    public boolean isFull() {
        return value >= maximum;
    }

    public void reset() {
        value = 0;
    }

    private int clamp(long candidate) {
        return (int) Math.max(0L, Math.min(maximum, candidate));
    }
}
