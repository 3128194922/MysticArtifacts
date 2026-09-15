package com.uniye.mysticartifacts.client.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.client.screen.AllSeeingEyeScreen;
import com.uniye.mysticartifacts.client.camera.SpectateCameraEntity;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.network.PlayerListPacket;
import com.uniye.mysticartifacts.network.RequestPlayerListPacket;
import com.uniye.mysticartifacts.network.SelectSpectatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AllSeeingEyeClientHandler {

    private static java.util.UUID watchedTarget;
    private static SpectateCameraEntity cameraEntity;

    public static boolean isWatching() {
        return watchedTarget != null;
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

    public static void handleSpectateState(java.util.UUID target) {
        if (target == null) {
            stopWatching();
        } else {
            watchedTarget = target;
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || watchedTarget == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopWatching();
            return;
        }

        if (cameraEntity == null || cameraEntity.level() != mc.level) {
            cameraEntity = new SpectateCameraEntity(mc.level);
        }

        Player target = mc.level.getPlayerByUUID(watchedTarget);
        if (target != null) {
            cameraEntity.follow(target);
        }
        if (mc.getCameraEntity() != cameraEntity) {
            mc.setCameraEntity(cameraEntity);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isWatching()) return;
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isWatching()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (isWatching() && mc.player != null && event.getEntity() == mc.player) {
            event.setCanceled(true);
        }
    }

    private static void stopWatching() {
        Minecraft mc = Minecraft.getInstance();
        watchedTarget = null;
        if (cameraEntity != null && mc.getCameraEntity() == cameraEntity && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        cameraEntity = null;
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
