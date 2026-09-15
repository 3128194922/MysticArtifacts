package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.event.DeathEyeEvents;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class DeathEyeItem extends Item implements ICurioItem {

    public DeathEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public void onEquip(SlotContext context, ItemStack prevStack, ItemStack stack) {
        if (context.entity().level().isClientSide) return;
        if (context.entity() instanceof Player player) {
            DeathEyeEvents.syncAll(player);
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (context.entity().level().isClientSide) return;
        if (context.entity() instanceof Player player) {
            DeathEyeEvents.clearProgress(player);
        }
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.DEATH_EYE.get()).isEmpty())
                .orElse(false);
    }
}
