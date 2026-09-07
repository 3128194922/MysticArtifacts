package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 武士刀（Muramasa）双模式状态：普通模式 / 鬼刀模式。
 *
 * <p>充能显示在耐久条上；充能满时潜行右键居合进入鬼刀模式。
 * 鬼刀模式下剑气与居合消耗充能，充能耗尽立即退出。</p>
 */
public final class KatanaState {
    public static final int CHARGE_PER_HIT = 1;
    public static final int CHARGE_PER_PARRY = 1;
    public static final int CHARGE_PER_USE = 1;
    /** 保留常量供 KatanaCircleSlashEntity 引用（圆斩已不再由武士刀触发）。 */
    public static final int CIRCLE_ATTACK_DURATION_TICKS = 9;

    private static final String ENERGY_TAG = "KatanaEnergy";
    /** 鬼刀结束时间戳（沿用旧 tag key，兼容已有存档的开鞘状态语义）。 */
    private static final String GHOST_UNTIL_TAG = "KatanaOpenUntil";
    private static final String AUTO_PARRY_TAG = "KatanaAutoParryLeft";

    private KatanaState() {
    }

    public static int maxCharge() {
        return Config.KatanaMaxCharge;
    }

    public static int clampEnergy(int energy) {
        return Math.max(0, Math.min(maxCharge(), energy));
    }

    public static boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= maxCharge();
    }

    /** 居合冲刺条件（纯函数）：充能满且不在鬼刀模式。 */
    public static boolean canDash(int energy, boolean open) {
        return energy >= maxCharge() && !open;
    }

    /** 从能量数值中扣除消耗（纯函数），不足时归零。 */
    public static int consumeEnergy(int energy, int cost) {
        return clampEnergy(energy - Math.max(0, cost));
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

    /** 消耗充能，不足时返回 false。 */
    public static boolean consumeCharge(ItemStack stack, int amount) {
        if (getEnergy(stack) < Math.max(0, amount)) {
            return false;
        }
        setEnergy(stack, clampEnergy(getEnergy(stack) - Math.max(0, amount)));
        return true;
    }

    // ---- 鬼刀模式状态 ----

    public static boolean isOpen(ItemStack stack, Level level) {
        long ghostUntil = getGhostUntil(stack);
        long currentTick = level != null ? level.getGameTime() : 0L;
        return ghostUntil > currentTick;
    }

    public static boolean isOpen(ItemStack stack, Level level, Entity holder) {
        long ghostUntil = getGhostUntil(stack);
        long currentTick = level != null ? level.getGameTime()
                : holder != null ? holder.level().getGameTime() : 0L;
        return ghostUntil > currentTick;
    }

    /** 无 Level 环境（属性计算等）使用：tag 存在即视为鬼刀，过期由 clearExpired 清理。 */
    public static boolean hasGhostTag(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(GHOST_UNTIL_TAG);
    }

    /** 进入鬼刀模式，同时重置本次的自动完美弹反次数。 */
    public static void openGhost(ItemStack stack, Level level) {
        if (level == null) {
            return;
        }
        stack.getOrCreateTag().putLong(GHOST_UNTIL_TAG, level.getGameTime() + Math.max(1, Config.KatanaGhostDurationTicks));
        stack.getOrCreateTag().putInt(AUTO_PARRY_TAG, Math.max(0, Config.KatanaGhostAutoParryMax));
    }

    public static void close(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(GHOST_UNTIL_TAG);
        tag.remove(AUTO_PARRY_TAG);
    }

    public static void clearExpired(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        if (tag.getLong(GHOST_UNTIL_TAG) <= gameTime) {
            tag.remove(GHOST_UNTIL_TAG);
            tag.remove(AUTO_PARRY_TAG);
        }
        setEnergy(stack, getEnergy(stack));
    }

    // ---- 鬼刀自动完美弹反 ----

    public static int getAutoParryLeft(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(AUTO_PARRY_TAG) : 0;
    }

    public static void consumeAutoParry(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        stack.getTag().putInt(AUTO_PARRY_TAG, Math.max(0, getAutoParryLeft(stack) - 1));
    }

    private static long getGhostUntil(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getLong(GHOST_UNTIL_TAG) : 0L;
    }
}
