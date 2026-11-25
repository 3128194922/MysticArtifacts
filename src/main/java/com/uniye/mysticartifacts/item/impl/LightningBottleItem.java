package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.config.LightningBottleConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Optional;

public class LightningBottleItem extends Item implements ICurioItem {
    private static final String THUNDER_POWER_TAG = "LightningBottleThunderPower";
    private static final int BAR_WIDTH_PIXELS = 13;
    private static final int THUNDER_BAR_COLOR = 0x48CFFF;

    public LightningBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.LIGHTNING_BOTTLE.get()).isEmpty())
                .orElse(false);
    }

    public static Optional<ItemStack> findEquippedBottle(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findCurios(ModItems.LIGHTNING_BOTTLE.get()).stream()
                        .map(slotResult -> slotResult.stack())
                        .findFirst())
                .orElse(Optional.empty());
    }

    public static int getThunderPower(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(THUNDER_POWER_TAG));
    }

    /** Adds charge and returns true when this addition fills the configured capacity. */
    public static boolean addThunderPower(ItemStack stack, int amount, int maximum) {
        int capacity = Math.max(1, maximum);
        int updated = Math.min(capacity, getThunderPower(stack) + Math.max(0, amount));
        if (updated < capacity) {
            stack.getOrCreateTag().putInt(THUNDER_POWER_TAG, updated);
            return false;
        }
        stack.getOrCreateTag().putInt(THUNDER_POWER_TAG, 0);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getThunderPower(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int maximum = Math.max(1, LightningBottleConfig.MAX_THUNDER_POWER.get());
        return Math.min(BAR_WIDTH_PIXELS,
                Math.round(getThunderPower(stack) * BAR_WIDTH_PIXELS / (float) maximum));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return THUNDER_BAR_COLOR;
    }
}
