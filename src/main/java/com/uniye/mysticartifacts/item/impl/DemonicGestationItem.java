package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.DemonicGestationEntity;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class DemonicGestationItem extends Item implements ICurioItem {

    private static final String TAG_STORED_WEAPON = "StoredWeapon";

    public DemonicGestationItem(Properties properties) {
        super(properties);
    }

    // ========== Melee Weapon Compatibility ==========

    public static boolean isSupportedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // A melee weapon is one that can perform a melee/swing ability
        // (vanilla swords & axes + most modded melee weapons that register these abilities).
        return stack.canPerformAction(ToolActions.SWORD_DIG)
                || stack.canPerformAction(ToolActions.SWORD_SWEEP)
                || stack.canPerformAction(ToolActions.AXE_DIG);
    }

    // ========== NBT Helpers ==========

    public static ResourceLocation getStoredWeapon(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STORED_WEAPON, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_STORED_WEAPON));
            if (id != null) return id;
        }
        return null;
    }

    public static boolean hasStoredWeapon(ItemStack stack) {
        return getStoredWeapon(stack) != null;
    }

    public static void setStoredWeapon(ItemStack stack, ResourceLocation weaponId) {
        stack.getOrCreateTag().putString(TAG_STORED_WEAPON, weaponId.toString());
    }

    // ========== ICurioItem Implementation ==========

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return hasStoredWeapon(stack);
    }

    @Override
    public void onEquip(SlotContext context, ItemStack prevStack, ItemStack stack) {
        if (context.entity().level().isClientSide) return;
        if (!(context.entity() instanceof Player player)) return;
        if (!hasStoredWeapon(stack)) return;

        refreshEntityFor(stack, player);
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (context.entity().level().isClientSide) return;
        if (!(context.entity() instanceof Player player)) return;

        for (DemonicGestationEntity existing : getEntitiesFor(player, player.level())) {
            existing.discard();
        }
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;

        ItemStack other = slot.getItem();
        if (other.isEmpty()) return false;

        if (isSupportedWeapon(other)) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(other.getItem());
            if (id != null) {
                if (hasStoredWeapon(stack)) {
                    ResourceLocation oldId = getStoredWeapon(stack);
                    Item oldWeapon = ForgeRegistries.ITEMS.getValue(oldId);
                    if (oldWeapon != null) {
                        player.getInventory().placeItemBackInInventory(new ItemStack(oldWeapon));
                    }
                }
                other.shrink(1);
                setStoredWeapon(stack, id);
                player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.1F);
                refreshEntityFor(stack, player);
                return true;
            }
        }
        return false;
    }

    private void refreshEntityFor(ItemStack stack, Player player) {
        Player effective = player;
        for (DemonicGestationEntity existing : getEntitiesFor(effective, effective.level())) {
            existing.discard();
        }

        ResourceLocation weaponId = getStoredWeapon(stack);
        if (weaponId == null) return;
        DemonicGestationEntity entity = new DemonicGestationEntity(effective, weaponId);
        effective.level().addFreshEntity(entity);
    }

    private static List<DemonicGestationEntity> getEntitiesFor(Player player, Level level) {
        return level.getEntitiesOfClass(
                DemonicGestationEntity.class,
                player.getBoundingBox().inflate(64.0),
                e -> player.getUUID().equals(e.getOwnerUUID())
        );
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.DEMONIC_GESTATION.get()).isEmpty())
                .orElse(false);
    }
}