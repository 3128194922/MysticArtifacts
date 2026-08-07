package com.uniye.mysticartifacts.client.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.client.screen.AllSeeingEyeScreen;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.network.PlayerListPacket;
import com.uniye.mysticartifacts.network.RequestPlayerListPacket;
import com.uniye.mysticartifacts.network.SelectSpectatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AllSeeingEyeClientHandler {

    public static boolean isWatching() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null
                && mc.gameMode != null
                && mc.gameMode.getPlayerMode() == GameType.SPECTATOR
                && mc.getCameraEntity() != mc.player;
    }

    public static void onUse() {
        Minecraft mc = Minecraft.getInstance();
        if (isWatching()) {
            SelectSpectatePacket.sendToServer(new SelectSpectatePacket(null));
        } else {
            if (mc.screen != null) {
                return;
            }
            RequestPlayerListPacket.sendToServer(new RequestPlayerListPacket());
        }
    }

    public static void handlePlayerList(PlayerListPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (msg.getPlayers().isEmpty()) {
            mc.player.displayClientMessage(
                    Component.translatable("item.mysticartifacts.all_seeing_eye.no_players"), true);
            return;
        }
        mc.setScreen(new AllSeeingEyeScreen(msg.getPlayers()));
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        if (event.getAction() != 1 || event.getButton() != 1) return;
        if (!isWatching()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() == ModItems.ALL_SEEING_EYE.get()) {
            event.setCanceled(true);
            SelectSpectatePacket.sendToServer(new SelectSpectatePacket(null));
        }
    }
}