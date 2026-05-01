package com.uniye.mysticartifacts.client.network;

import com.uniye.mysticartifacts.client.particle.TextIndicatorParticle;
import com.uniye.mysticartifacts.network.SpawnTextIndicatorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class ClientPacketHandler {
    public static void handleSpawnTextIndicator(SpawnTextIndicatorPacket msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Minecraft.getInstance().particleEngine.add(
                    new TextIndicatorParticle(
                            level,
                            msg.getX(),
                            msg.getY(),
                            msg.getZ(),
                            msg.getText(),
                            msg.getColor(),
                            msg.isOutline(),
                            msg.getOutlineColor(),
                            msg.getSize()
                    )
            );
        }
    }
}
