package com.uniye.mysticartifacts.item.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class KatanaState {
    public static final int MAX_ENERGY = 100;
    public static final int DASH_COST = 100;
    public static final int HIT_ENERGY = 1;
    public static final int BLOCK_ENERGY = 10;
    public static final int OPEN_DURATION_TICKS = 200;
    public static final int CIRCLE_ATTACK_DURATION_TICKS = 9;

    private static final String ENERGY_TAG = "KatanaEnergy";
    private static final String OPEN_UNTIL_TAG = "KatanaOpenUntil";
    private static final String CIRCLE_ATTACK_UNTIL_TAG = "KatanaCircleAttackUntil";

    private KatanaState() {
    }

    public static int clampEnergy(int energy) {
        return Math.max(0, Math.min(MAX_ENERGY, energy));
    }

    public static boolean canDash(int energy, boolean open) {
        return !open && energy >= DASH_COST;
    }

    public static int consumeEnergy(int energy, int amount) {
        return clampEnergy(energy - Math.max(0, amount));
    }

    public static int getEnergy(ItemStack stack) {
        return stack.hasTag() ? clampEnergy(stack.getTag().getInt(ENERGY_TAG)) : 0;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt(ENERGY_TAG, clampEnergy(energy));
    }

    public static int addEnergy(ItemStack stack, int amount) {
        int newEnergy = clampEnergy(getEnergy(stack) + Math.max(0, amount));
        setEnergy(stack, newEnergy);
        return newEnergy;
    }

    public static long getOpenUntil(ItemStack stack) {
        return getLongTag(stack, OPEN_UNTIL_TAG);
    }

    public static long getCircleAttackUntil(ItemStack stack) {
        return getLongTag(stack, CIRCLE_ATTACK_UNTIL_TAG);
    }

    public static boolean isOpen(ItemStack stack, Level level) {
        long openUntil = getOpenUntil(stack);
        long currentTick = level != null ? level.getGameTime() : 0L;
        return openUntil > currentTick;
    }

    public static boolean isOpen(ItemStack stack, Level level, Entity holder) {
        long openUntil = getOpenUntil(stack);
        long currentTick = level != null ? level.getGameTime()
                : holder != null ? holder.level().getGameTime() : 0L;
        return openUntil > currentTick;
    }

    public static void open(ItemStack stack, Level level, int durationTicks) {
        if (level == null) {
            return;
        }
        stack.getOrCreateTag().putLong(OPEN_UNTIL_TAG, level.getGameTime() + Math.max(0, durationTicks));
    }

    public static void setCircleAttackUntil(ItemStack stack, long gameTime) {
        stack.getOrCreateTag().putLong(CIRCLE_ATTACK_UNTIL_TAG, gameTime);
    }

    public static void close(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(OPEN_UNTIL_TAG);
        tag.remove(CIRCLE_ATTACK_UNTIL_TAG);
    }

    public static boolean canDash(ItemStack stack, Level level) {
        return canDash(getEnergy(stack), isOpen(stack, level));
    }

    public static boolean consumeDash(ItemStack stack) {
        if (getEnergy(stack) < DASH_COST) {
            return false;
        }
        setEnergy(stack, consumeEnergy(getEnergy(stack), DASH_COST));
        return true;
    }

    public static void clearExpired(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        if (tag.getLong(OPEN_UNTIL_TAG) <= gameTime) {
            tag.remove(OPEN_UNTIL_TAG);
        }
        if (tag.getLong(CIRCLE_ATTACK_UNTIL_TAG) <= gameTime) {
            tag.remove(CIRCLE_ATTACK_UNTIL_TAG);
        }
        setEnergy(stack, getEnergy(stack));
    }

    private static long getLongTag(ItemStack stack, String key) {
        return stack.hasTag() ? stack.getTag().getLong(key) : 0L;
    }
}
