package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class DeathScytheItem extends Item {
    private static final String TAG_TARGET_UUID = "TargetUUID";
    private static final String TAG_ENERGY_UNTIL = "EnergyUntil";

    public DeathScytheItem(Properties properties) {
        super(properties);
    }

    public static UUID getTargetUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_TARGET_UUID) ? tag.getUUID(TAG_TARGET_UUID) : null;
    }

    public static void setTargetUUID(ItemStack stack, UUID targetUUID) {
        stack.getOrCreateTag().putUUID(TAG_TARGET_UUID, targetUUID);
    }

    public static void clearTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_TARGET_UUID);
        }
    }

    public static long getEnergyUntil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(TAG_ENERGY_UNTIL);
    }

    public static void setEnergyUntil(ItemStack stack, long energyUntil) {
        stack.getOrCreateTag().putLong(TAG_ENERGY_UNTIL, energyUntil);
    }

    public static int getRemainingEnergyTicks(ItemStack stack, long gameTime) {
        long remaining = getEnergyUntil(stack) - gameTime;
        return (int) Math.max(0L, Math.min(Config.DeathScytheEnergyTicks, remaining));
    }
}
