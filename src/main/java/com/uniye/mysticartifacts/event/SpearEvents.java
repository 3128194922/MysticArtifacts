package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家左键突刺入口：取消原版单目标攻击，改走长矛的多目标穿透判定，
 * 避免原版伤害与长矛伤害叠加。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class SpearEvents {
    private SpearEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SpearItem spear)) return;

        Entity target = event.getTarget();
        Entity resolved = target instanceof PartEntity<?> part ? part.getParent() : target;
        if (!(resolved instanceof LivingEntity)) return;

        if (!player.level().isClientSide) {
            spear.stab(player, EquipmentSlot.MAINHAND, target);
        }
        event.setCanceled(true);
    }
}
