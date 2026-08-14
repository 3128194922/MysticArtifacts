package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 紧急预案：当玩家即将获得物品冷却时，抵消该冷却，
 * 并将同等时长的冷却转移给本饰品（冷却期间无法生效）。
 */
public class EmergencyPlanItem extends Item implements ICurioItem {

    public EmergencyPlanItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.EMERGENCY_PLAN.get()).isEmpty())
                .orElse(false);
    }

    public static boolean isOnCooldown(net.minecraft.world.entity.player.Player player) {
        return player.getCooldowns().isOnCooldown(ModItems.EMERGENCY_PLAN.get());
    }
}
