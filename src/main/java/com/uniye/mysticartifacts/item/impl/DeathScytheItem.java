package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class DeathScytheItem extends Item {
    private static final String TARGET_UUID_TAG = "DeathScytheTargetUUID";
    private static final String ENERGY_UNTIL_TAG = "DeathScytheEnergyUntil";

    public DeathScytheItem(Properties properties) {
        super(properties);
    }

    public static UUID getTargetUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TARGET_UUID_TAG) ? tag.getUUID(TARGET_UUID_TAG) : null;
    }

    public static void setTargetUUID(ItemStack stack, UUID targetUUID) {
        stack.getOrCreateTag().putUUID(TARGET_UUID_TAG, targetUUID);
    }

    public static void clearTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TARGET_UUID_TAG);
        }
    }

    public static long getEnergyUntil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(ENERGY_UNTIL_TAG);
    }

    public static void setEnergyUntil(ItemStack stack, long energyUntil) {
        stack.getOrCreateTag().putLong(ENERGY_UNTIL_TAG, energyUntil);
    }

    public static int getRemainingEnergyTicks(ItemStack stack, long gameTime) {
        long remaining = getEnergyUntil(stack) - gameTime;
        return (int) Math.max(0L, Math.min(Config.DeathScytheEnergyTicks, remaining));
    }
}
