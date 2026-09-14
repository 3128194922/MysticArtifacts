package com.uniye.mysticartifacts.client.network;

import com.uniye.mysticartifacts.client.event.SurvivalJadeClientHandler;
import com.uniye.mysticartifacts.client.event.DeathEyeClientHandler;
import com.uniye.mysticartifacts.client.event.AllSeeingEyeClientHandler;
import com.uniye.mysticartifacts.item.impl.AncestorsLetterItem;
import com.uniye.mysticartifacts.client.sculk.SculkSymbioteClientState;
import com.uniye.mysticartifacts.network.SculkSymbioteVibrationPacket;

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

    public static void handleSculkSymbioteVibrations(java.util.List<SculkSymbioteVibrationPacket.Marker> markers) {
        SculkSymbioteClientState.acceptMarkers(markers);
    }
}
