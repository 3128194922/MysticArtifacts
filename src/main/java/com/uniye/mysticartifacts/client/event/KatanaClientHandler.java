package com.uniye.mysticartifacts.client.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.KatanaState;
import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import com.uniye.mysticartifacts.network.KatanaSwingPacket;
import com.uniye.mysticartifacts.network.NetworkHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 处理开鞘状态左键空挥，并将请求发送给服务器。 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KatanaClientHandler {
    private KatanaClientHandler() {
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof MuramasaItem
                && KatanaState.isOpen(player.getMainHandItem(), player.level())) {
            NetworkHandler.INSTANCE.sendToServer(new KatanaSwingPacket());
        }
    }
}
