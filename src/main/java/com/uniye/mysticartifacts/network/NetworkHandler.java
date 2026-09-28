package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1.1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MysticArtifacts.MODID, "simple_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, RevolverFirePacket.class, RevolverFirePacket::encode, RevolverFirePacket::decode,
                RevolverFirePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, RevolverFeedbackPacket.class, RevolverFeedbackPacket::encode, RevolverFeedbackPacket::decode,
                RevolverFeedbackPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, TeleportToKunaiPacket.class,
                TeleportToKunaiPacket::encode,
                TeleportToKunaiPacket::decode,
                TeleportToKunaiPacket::handle
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

        INSTANCE.registerMessage(id++, SpectateStatePacket.class,
                SpectateStatePacket::encode,
                SpectateStatePacket::decode,
                SpectateStatePacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(id++, SurvivalJadeSyncPacket.class,
                SurvivalJadeSyncPacket::encode,
                SurvivalJadeSyncPacket::decode,
                SurvivalJadeSyncPacket::handle
        );

        INSTANCE.registerMessage(id++, AncestorsLetterSyncPacket.class,
                AncestorsLetterSyncPacket::encode,
                AncestorsLetterSyncPacket::decode,
                AncestorsLetterSyncPacket::handle
        );

        INSTANCE.registerMessage(id++, KatanaSwingPacket.class,
                KatanaSwingPacket::encode,
                KatanaSwingPacket::decode,
                KatanaSwingPacket::handle
        );

        INSTANCE.registerMessage(id++, DeathEyeProgressPacket.class,
                DeathEyeProgressPacket::encode,
                DeathEyeProgressPacket::decode,
                DeathEyeProgressPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(id++, SculkSymbioteVibrationPacket.class,
                SculkSymbioteVibrationPacket::encode,
                SculkSymbioteVibrationPacket::decode,
                SculkSymbioteVibrationPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(id++, LightningBottlePacket.class,
                LightningBottlePacket::encode,
                LightningBottlePacket::decode,
                LightningBottlePacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(id++, FlagSpearTrailPacket.class,
                FlagSpearTrailPacket::encode,
                FlagSpearTrailPacket::decode,
                FlagSpearTrailPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(id++, C4ThrowPacket.class,
                C4ThrowPacket::encode,
                C4ThrowPacket::decode,
                C4ThrowPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );

        id = Math.max(id, 18); // ID 17 is reserved for the existing C4 packet change.
        INSTANCE.registerMessage(id++, DeathScytheSlashPacket.class,
                DeathScytheSlashPacket::encode,
                DeathScytheSlashPacket::decode,
                DeathScytheSlashPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        INSTANCE.registerMessage(id++, DeathScytheTargetPacket.class,
                DeathScytheTargetPacket::encode,
                DeathScytheTargetPacket::decode,
                DeathScytheTargetPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
