package com.uniye.mysticartifacts.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class TwoDragonsState {
    private static final String ROOT_KEY = "mysticartifacts_two_dragons";
    public static final String KEY_HAS_FIRE = "has_fire";
    public static final String KEY_HAS_ICE = "has_ice";
    public static final String KEY_ACTIVE_FIRE = "active_fire";
    public static final String KEY_ACTIVE_ICE = "active_ice";
    public static final String KEY_LAST_THROWN = "last_thrown";

    private TwoDragonsState() {
    }

    private static CompoundTag getRoot(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag root = new CompoundTag();
            root.putBoolean(KEY_HAS_FIRE, true);
            root.putBoolean(KEY_HAS_ICE, true);
            root.putBoolean(KEY_ACTIVE_FIRE, false);
            root.putBoolean(KEY_ACTIVE_ICE, false);
            root.putString(KEY_LAST_THROWN, "");
            data.put(ROOT_KEY, root);
        }
        return data.getCompound(ROOT_KEY);
    }

    public static boolean hasFire(Player player) {
        return getRoot(player).getBoolean(KEY_HAS_FIRE);
    }

    public static boolean hasIce(Player player) {
        return getRoot(player).getBoolean(KEY_HAS_ICE);
    }

    public static boolean isActiveFire(Player player) {
        return getRoot(player).getBoolean(KEY_ACTIVE_FIRE);
    }

    public static boolean isActiveIce(Player player) {
        return getRoot(player).getBoolean(KEY_ACTIVE_ICE);
    }

    public static String getLastThrown(Player player) {
        return getRoot(player).getString(KEY_LAST_THROWN);
    }

    public static void setHasFire(Player player, boolean value) {
        getRoot(player).putBoolean(KEY_HAS_FIRE, value);
    }

    public static void setHasIce(Player player, boolean value) {
        getRoot(player).putBoolean(KEY_HAS_ICE, value);
    }

    public static void setActiveFire(Player player, boolean value) {
        getRoot(player).putBoolean(KEY_ACTIVE_FIRE, value);
    }

    public static void setActiveIce(Player player, boolean value) {
        getRoot(player).putBoolean(KEY_ACTIVE_ICE, value);
    }

    public static void setLastThrown(Player player, String value) {
        getRoot(player).putString(KEY_LAST_THROWN, value);
    }

    public static void resetOnDeath(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag root = data.getCompound(ROOT_KEY);
            root.putBoolean(KEY_ACTIVE_FIRE, false);
            root.putBoolean(KEY_ACTIVE_ICE, false);
        }
    }
}
