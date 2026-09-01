package com.uniye.mysticartifacts.client.network;

import com.uniye.mysticartifacts.client.event.SurvivalJadeClientHandler;
import com.uniye.mysticartifacts.item.impl.AncestorsLetterItem;

public class ClientPacketHandler {
    public static void handleSurvivalJadeSync(float phantom) {
        SurvivalJadeClientHandler.setPhantom(phantom);
    }

    public static void handleAncestorsLetterSync(int state) {
        AncestorsLetterItem.clientState = state;
    }
}
