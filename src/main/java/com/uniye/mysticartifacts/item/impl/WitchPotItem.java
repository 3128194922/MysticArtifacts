package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class WitchPotItem extends Item implements ICurioItem {
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_SPLASH = 1;
    public static final int TYPE_LINGERING = 2;

    public WitchPotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();

        // 右键空槽位：释放内部存储的药水
        if (other.isEmpty()) {
            if (!hasPotionData(stack)) return false;
            return releaseStoredPotion(stack, slot, player);
        }

        Item item = other.getItem();
        int potionType = -1;
        if (item instanceof PotionItem) {
            potionType = TYPE_NORMAL;
        } else if (item instanceof SplashPotionItem) {
            potionType = TYPE_SPLASH;
        } else if (item instanceof LingeringPotionItem) {
            potionType = TYPE_LINGERING;
        } else {
            return false;
        }

        var effects = PotionUtils.getMobEffects(other);
        if (effects.isEmpty()) return false;

        if (hasPotionData(stack)) {
            removeStoredEffects(player, stack);
        }

        CompoundTag data = stack.getOrCreateTagElement("PotionData");
        data.putInt("type", potionType);
        ListTag effectsList = new ListTag();
        for (MobEffectInstance effect : effects) {
            effectsList.add(effect.save(new CompoundTag()));
        }
        data.put("effects", effectsList);

        other.shrink(1);
        player.playSound(SoundEvents.BREWING_STAND_BREW, 1.0f, 1.0f);
        return true;
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (context.entity() instanceof Player player) {
            removeStoredEffects(player, stack);
        }
    }

    private static void removeStoredEffects(Player player, ItemStack stack) {
        if (!hasPotionData(stack)) return;
        for (MobEffectInstance effect : getStoredEffects(stack)) {
            player.removeEffect(effect.getEffect());
        }
    }

    private static boolean releaseStoredPotion(ItemStack stack, Slot slot, Player player) {
        int type = getPotionType(stack);
        List<MobEffectInstance> effects = getStoredEffects(stack);
        if (effects.isEmpty() || type < 0) return false;

        Item potionItem = switch (type) {
            case TYPE_NORMAL -> Items.POTION;
            case TYPE_SPLASH -> Items.SPLASH_POTION;
            case TYPE_LINGERING -> Items.LINGERING_POTION;
            default -> null;
        };
        if (potionItem == null) return false;

        ItemStack potionStack = new ItemStack(potionItem);
        PotionUtils.setCustomEffects(potionStack, effects);

        if (!slot.mayPlace(potionStack)) return false;
        slot.set(potionStack);

        stack.removeTagKey("PotionData");

        player.playSound(SoundEvents.BREWING_STAND_BREW, 1.0f, 1.0f);
        return true;
    }

    public static boolean hasPotionData(ItemStack stack) {
        return stack.getTagElement("PotionData") != null;
    }

    public static int getPotionType(ItemStack stack) {
        CompoundTag data = stack.getTagElement("PotionData");
        if (data == null) return -1;
        return data.getInt("type");
    }

    public static List<MobEffectInstance> getStoredEffects(ItemStack stack) {
        List<MobEffectInstance> list = new ArrayList<>();
        CompoundTag data = stack.getTagElement("PotionData");
        if (data == null) return list;
        ListTag effectsList = data.getList("effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effectsList.size(); i++) {
            MobEffectInstance effect = MobEffectInstance.load(effectsList.getCompound(i));
            if (effect != null) list.add(effect);
        }
        return list;
    }

    public static boolean isWearing(net.minecraft.world.entity.LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> !handler.findCurios(ModItems.WITCH_POT.get()).isEmpty())
                .orElse(false);
    }
}
