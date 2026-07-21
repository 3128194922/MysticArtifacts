package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.item.impl.CodexItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.Map;

public class CodexAnvilHandler {

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!left.isEmpty() && !right.isEmpty() && right.getItem() instanceof CodexItem) {
            CompoundTag tag = right.getOrCreateTag().getCompound("StoredEnchant");

            if (tag.contains("id") && tag.contains("lvl")) {
                ResourceLocation enchantId = ResourceLocation.tryParse(tag.getString("id"));
                int targetLevel = tag.getInt("lvl");

                if (enchantId != null) {
                    Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(enchantId);
                    int currentLevel = EnchantmentHelper.getEnchantments(left).getOrDefault(enchantment, 0);

                    if (enchantment != null && currentLevel == targetLevel - 1) {
                        ItemStack output = left.copy();
                        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(output);
                        enchants.put(enchantment, targetLevel);
                        EnchantmentHelper.setEnchantments(enchants, output);

                        event.setOutput(output);
                        event.setCost(50);
                        return;
                    }
                }
            }
        }

        if (!left.isEmpty() && !right.isEmpty()
                && left.getItem() instanceof CodexItem
                && !left.getOrCreateTag().contains("StoredEnchant")) {

            ResourceLocation rightId = BuiltInRegistries.ITEM.getKey(right.getItem());
            if (rightId != null && rightId.toString().equals("quark:ancient_tome") && ModList.get().isLoaded("quark")) {

                CompoundTag tag = right.getTag();
                if (tag != null && tag.contains("StoredEnchantments")) {
                    var enchantList = EnchantmentHelper.deserializeEnchantments(tag.getList("StoredEnchantments", 10));

                    if (enchantList.size() == 1) {
                        Map.Entry<Enchantment, Integer> entry = enchantList.entrySet().iterator().next();
                        Enchantment enchant = entry.getKey();

                        ItemStack output = left.copy();
                        CompoundTag stored = new CompoundTag();
                        stored.putString("id", BuiltInRegistries.ENCHANTMENT.getKey(enchant).toString());
                        stored.putInt("lvl", enchant.getMaxLevel() + 2);
                        output.getOrCreateTag().put("StoredEnchant", stored);

                        event.setOutput(output);
                        event.setCost(30);
                    }
                }
            }
        }
    }
}
