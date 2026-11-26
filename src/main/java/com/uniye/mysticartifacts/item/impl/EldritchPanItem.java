package com.uniye.mysticartifacts.item.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 饕餮之锅：光标持锅右键食物槽喂食，已食用种类数量转化为攻击伤害。
 */
public class EldritchPanItem extends SwordItem {
    public static final String FOOD_LIST_TAG = "foodlist";
    public static final String FOOD_NUMBER_TAG = "foodnumber";
    public static final String TOOLTIP_NAME = "tooltip.mysticartifacts.eldritch_pan.name";
    public static final String TOOLTIP_FOOD_COUNT = "tooltip.mysticartifacts.eldritch_pan.food_count";
    private static final String FOOD_ALREADY_EATEN = "这个食物已经吃过了！";

    public EldritchPanItem(Properties properties) {
        // KubeJS: attackDamageBaseline 3.0 + tier attackDamageBonus 4.0 => 7；此处用铁剑 2.0 + 5 得到同样结果。
        super(Tiers.IRON, 5, -3.1F, properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pan, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack food = slot.getItem();
        if (food.isEmpty() || !food.getItem().isEdible()) {
            return false;
        }

        if (hasEaten(pan, food)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal(FOOD_ALREADY_EATEN), true);
            }
            player.playSound(SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 1.0F, 1.0F);
            return true;
        }

        CompoundTag tag = pan.getOrCreateTag();
        ListTag foodList = tag.getList(FOOD_LIST_TAG, Tag.TAG_STRING);
        foodList.add(StringTag.valueOf(getItemId(food)));
        tag.put(FOOD_LIST_TAG, foodList);
        tag.putInt(FOOD_NUMBER_TAG, foodList.size());
        // 主动 setTag，确保 Forge 的 ItemStack 属性修饰符缓存随 NBT 变化刷新。
        pan.setTag(tag);

        food.shrink(1);
        player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(0, Component.translatable(TOOLTIP_NAME));
        tooltip.add(1, Component.translatable(TOOLTIP_FOOD_COUNT)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(Integer.toString(getFoodCount(stack))).withStyle(ChatFormatting.YELLOW)));
    }

    public static int getFoodCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(FOOD_NUMBER_TAG);
    }

    private static boolean hasEaten(ItemStack pan, ItemStack food) {
        CompoundTag tag = pan.getTag();
        if (tag == null) {
            return false;
        }

        String foodId = getItemId(food);
        ListTag foodList = tag.getList(FOOD_LIST_TAG, Tag.TAG_STRING);
        for (int i = 0; i < foodList.size(); i++) {
            if (foodId.equals(foodList.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private static String getItemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString();
    }
}
