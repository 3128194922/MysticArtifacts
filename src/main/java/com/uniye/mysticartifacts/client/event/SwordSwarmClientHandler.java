package com.uniye.mysticartifacts.client.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.SwordSwarmCharm;
import com.uniye.mysticartifacts.network.NetworkHandler;
import com.uniye.mysticartifacts.network.SwordSwarmAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT)
public class SwordSwarmClientHandler {
    
    private static final int FIRE_INTERVAL_TICKS = 2;
    private static long lastFireTime = 0;
    
    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!SwordSwarmCharm.isWearing(mc.player)) return;

        if (event.isAttack()) {
            event.setSwingHand(true);
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!SwordSwarmCharm.isWearing(mc.player)) return;
        if (mc.screen != null) return;
        if (!mc.options.keyAttack.isDown()) return;
        long now = mc.level.getGameTime();
        if (now - lastFireTime < FIRE_INTERVAL_TICKS) return;
        lastFireTime = now;
        
        NetworkHandler.INSTANCE.sendToServer(new SwordSwarmAttackPacket());
        mc.player.swing(mc.player.getUsedItemHand());
    }
}
