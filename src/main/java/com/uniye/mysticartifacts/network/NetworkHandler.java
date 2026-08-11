package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MysticArtifacts.MODID, "simple_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, TeleportToKunaiPacket.class,
                TeleportToKunaiPacket::encode,
                TeleportToKunaiPacket::decode,
                TeleportToKunaiPacket::handle
        );

        INSTANCE.registerMessage(id++, SpawnTextIndicatorPacket.class,
                SpawnTextIndicatorPacket::encode,
                SpawnTextIndicatorPacket::decode,
                SpawnTextIndicatorPacket::handle
        );

        INSTANCE.registerMessage(id++, PokerCardThrowPacket.class,
                PokerCardThrowPacket::encode,
                PokerCardThrowPacket::decode,
                PokerCardThrowPacket::handle
        );

        INSTANCE.registerMessage(id++, TwoDragonsThrowPacket.class,
                TwoDragonsThrowPacket::encode,
                TwoDragonsThrowPacket::decode,
                TwoDragonsThrowPacket::handle
        );

        INSTANCE.registerMessage(id++, SwordSwarmAttackPacket.class,
                SwordSwarmAttackPacket::encode,
                SwordSwarmAttackPacket::decode,
                SwordSwarmAttackPacket::handle
        );

        INSTANCE.registerMessage(id++, RequestPlayerListPacket.class,
                RequestPlayerListPacket::encode,
                RequestPlayerListPacket::decode,
                RequestPlayerListPacket::handle
        );

        INSTANCE.registerMessage(id++, PlayerListPacket.class,
                PlayerListPacket::encode,
                PlayerListPacket::decode,
                PlayerListPacket::handle
        );

        INSTANCE.registerMessage(id++, SelectSpectatePacket.class,
                SelectSpectatePacket::encode,
                SelectSpectatePacket::decode,
                SelectSpectatePacket::handle
        );

        INSTANCE.registerMessage(id++, SurvivalJadeSyncPacket.class,
                SurvivalJadeSyncPacket::encode,
                SurvivalJadeSyncPacket::decode,
                SurvivalJadeSyncPacket::handle
        );
    }
}
