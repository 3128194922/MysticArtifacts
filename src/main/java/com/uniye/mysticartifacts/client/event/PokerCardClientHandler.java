package com.uniye.mysticartifacts.client.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.PokerCardItem;
import com.uniye.mysticartifacts.network.NetworkHandler;
import com.uniye.mysticartifacts.network.PokerCardThrowPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT)
public class PokerCardClientHandler {
    private static int throwCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (throwCooldown > 0) {
            throwCooldown--;
        }

        if (mc.options.keyAttack.isDown()) {
            if (mc.player.getMainHandItem().getItem() instanceof PokerCardItem) {
                if (throwCooldown <= 0) {
                    NetworkHandler.INSTANCE.sendToServer(new PokerCardThrowPacket());
                    throwCooldown = 4; // 4 ticks cooldown
                    
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
        }
    }
}
