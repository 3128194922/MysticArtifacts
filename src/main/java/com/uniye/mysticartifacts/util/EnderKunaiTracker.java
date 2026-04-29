package com.uniye.mysticartifacts.util;

import com.uniye.mysticartifacts.entity.EnderKunaiEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EnderKunaiTracker {
    private static final String ROOT_KEY = "mysticartifacts_ender_kunai";
    private static final String LIST_KEY = "tracked_kunai_uuids";

    private EnderKunaiTracker() {
    }

    public static void addKunai(ServerPlayer player, UUID kunaiUuid) {
        if (kunaiUuid == null) {
            return;
        }
        List<UUID> uuids = getTrackedUuids(player);
        if (!uuids.contains(kunaiUuid)) {
            uuids.add(kunaiUuid);
            saveTrackedUuids(player, uuids);
        }
    }

    public static void removeKunai(ServerPlayer player, UUID kunaiUuid) {
        if (kunaiUuid == null) {
            return;
        }
        List<UUID> uuids = getTrackedUuids(player);
        if (uuids.remove(kunaiUuid)) {
            saveTrackedUuids(player, uuids);
        }
    }

    public static EnderKunaiEntity findNearestGroundedKunai(ServerPlayer player) {
        List<UUID> uuids = getTrackedUuids(player);
        List<UUID> validUuids = new ArrayList<>();
        EnderKunaiEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (UUID uuid : uuids) {
            Entity entity = player.serverLevel().getEntity(uuid);
            if (entity instanceof EnderKunaiEntity kunai && kunai.isAlive()) {
                validUuids.add(uuid);
                if (kunai.isInGround()) {
                    double dist = player.distanceToSqr(kunai);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = kunai;
                    }
                }
            }
        }

        if (validUuids.size() != uuids.size()) {
            saveTrackedUuids(player, validUuids);
        }

        return nearest;
    }

    private static List<UUID> getTrackedUuids(ServerPlayer player) {
        CompoundTag root = getRootTag(player);
        ListTag listTag = root.getList(LIST_KEY, Tag.TAG_STRING);
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            String raw = listTag.getString(i);
            try {
                uuids.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuids;
    }

    private static void saveTrackedUuids(ServerPlayer player, List<UUID> uuids) {
        CompoundTag data = player.getPersistentData();
        CompoundTag root = getRootTag(player);
        if (uuids.isEmpty()) {
            root.remove(LIST_KEY);
            if (root.isEmpty()) {
                data.remove(ROOT_KEY);
            } else {
                data.put(ROOT_KEY, root);
            }
            return;
        }

        ListTag listTag = new ListTag();
        for (UUID uuid : uuids) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        root.put(LIST_KEY, listTag);
        data.put(ROOT_KEY, root);
    }

    private static CompoundTag getRootTag(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            data.put(ROOT_KEY, new CompoundTag());
        }
        return data.getCompound(ROOT_KEY);
    }
}
