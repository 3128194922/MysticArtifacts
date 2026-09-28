package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.client.sculk.SculkSymbioteClientState;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class TrailSightItem extends Item implements ICurioItem {
    public TrailSightItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.TRAIL_SIGHT.get()).isEmpty())
                .orElse(false);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return SculkSymbioteClientState.exposureValue() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int maximum = Math.max(1, SculkSymbioteClientState.exposureMaximum());
        return Math.max(0, Math.min(13,
                Math.round(13.0F * SculkSymbioteClientState.exposureValue() / maximum)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x35D0C5;
    }
}
