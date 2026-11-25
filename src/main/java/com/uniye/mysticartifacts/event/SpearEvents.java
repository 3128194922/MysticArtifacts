package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 玩家左键突刺入口：取消原版单目标攻击，改走长矛的多目标穿透判定，
 * 避免原版伤害与长矛伤害叠加；右键蓄力期间允许疾跑并额外提速。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class SpearEvents {
    private static final UUID SPEAR_SPEED_ID = UUID.fromString("6d1f6a66-4a65-4e8f-9a6f-2f6f8e5b8a71");
    private static final double SPEAR_SPEED_BONUS = 0.2D;

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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean usingSpear = player.isUsingItem() && player.getUseItem().getItem() instanceof SpearItem;
        AttributeModifier current = speed.getModifier(SPEAR_SPEED_ID);
        if (usingSpear && current == null) {
            speed.addTransientModifier(new AttributeModifier(SPEAR_SPEED_ID, "mysticartifacts:spear_speed",
                    SPEAR_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!usingSpear && current != null) {
            speed.removeModifier(SPEAR_SPEED_ID);
        }
    }
}
