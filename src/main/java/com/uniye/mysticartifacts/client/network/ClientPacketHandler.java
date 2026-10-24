package com.uniye.mysticartifacts.client.network;

import com.uniye.mysticartifacts.client.event.SurvivalJadeClientHandler;
import com.uniye.mysticartifacts.client.event.DeathEyeClientHandler;
import com.uniye.mysticartifacts.client.event.AllSeeingEyeClientHandler;
import com.uniye.mysticartifacts.item.impl.AncestorsLetterItem;
import com.uniye.mysticartifacts.client.sculk.SculkSymbioteClientState;
import com.uniye.mysticartifacts.network.SculkSymbioteVibrationPacket;
import com.uniye.mysticartifacts.client.lightning.LightningBottleClientState;
import com.uniye.mysticartifacts.client.flag.FlagSpearClientState;
import com.uniye.mysticartifacts.item.impl.FlagSpearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientPacketHandler {
    public static void handleSurvivalJadeSync(float phantom) {
        SurvivalJadeClientHandler.setPhantom(phantom);
    }

    public static void handleAncestorsLetterSync(int state) {
        AncestorsLetterItem.clientState = state;
    }

    public static void handleDeathEyeProgressSync(UUID targetId, double accumulatedDamage, boolean clearAll) {
        if (clearAll) {
            DeathEyeClientHandler.clearProgress();
        } else if (targetId != null) {
            DeathEyeClientHandler.setProgress(targetId, accumulatedDamage);
        }
    }

    public static void handleSpectateState(UUID targetId) {
        AllSeeingEyeClientHandler.handleSpectateState(targetId);
    }

    public static void handleSculkSymbioteVibrations(java.util.List<SculkSymbioteVibrationPacket.Marker> markers,
                                                     int exposure, int exposureMaximum) {
        SculkSymbioteClientState.setExposure(exposure, exposureMaximum);
        SculkSymbioteClientState.acceptMarkers(markers);
    }

    public static void handleLightningBottle(java.util.List<Vec3> path, Vec3 fieldCenter) {
        LightningBottleClientState.accept(path);
        if (fieldCenter != null) {
            LightningBottleClientState.acceptField(fieldCenter);
        }
    }

    public static void handleFlagSpearTrail(Vec3 center, float yaw, long seed) {
        FlagSpearClientState.accept(center, yaw, seed);
    }

    /**
     * 把服务端权威的旗枪充能层数写回本地物品栏对应槽位，让充能条即时刷新。
     */
    public static void handleFlagSpearCharge(int slot, int charge, int maxCharge) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || slot < 0 || slot >= player.getInventory().items.size()) return;

        ItemStack stack = player.getInventory().items.get(slot);
        if (stack.isEmpty()) return;

        FlagSpearItem.setCharge(stack, charge);
    }
}
