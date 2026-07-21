package com.uniye.mysticartifacts.item.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CodexItem extends Item {

    public CodexItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("StoredEnchant");

        if (tag.contains("id")) {
            ResourceLocation enchantId = ResourceLocation.tryParse(tag.getString("id"));
            Enchantment enchant = enchantId != null ? BuiltInRegistries.ENCHANTMENT.get(enchantId) : null;

            if (enchant != null) {
                Component enchantName = Component.translatable(enchant.getDescriptionId());
                return Component.literal("已誊写")
                        .append(enchantName)
                        .append("的伪抄");
            }
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTag().getCompound("StoredEnchant");
        Style grayStyle = Style.EMPTY.withColor(ChatFormatting.GRAY);

        if (tag.contains("id") && tag.contains("lvl")) {
            String rawId = tag.getString("id");
            ResourceLocation enchantId = ResourceLocation.tryParse(rawId);
            Enchantment enchant = enchantId != null ? BuiltInRegistries.ENCHANTMENT.get(enchantId) : null;
            int level = tag.getInt("lvl");

            if (enchant != null) {
                String enchantName = Component.translatable(enchant.getDescriptionId()).getString();
                tooltip.add(Component.literal("+I " + enchantName + " (Max. " + toRoman(level) + ")").setStyle(grayStyle));
            }
        } else {
            tooltip.add(Component.literal("伪典中翻涌着被遗忘的力量。").setStyle(grayStyle));
            tooltip.add(Component.literal("惟有将古卷抄录其中才能将其唤醒。").setStyle(grayStyle));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().contains("StoredEnchant");
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return stack.getOrCreateTag().contains("StoredEnchant") ? Rarity.EPIC : Rarity.RARE;
    }

    private static String toRoman(int number) {
        String[] romans = {
                "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
        };
        return (number > 0 && number <= romans.length) ? romans[number - 1] : Integer.toString(number);
    }
}
