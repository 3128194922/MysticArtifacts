package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QuantumKeyItem extends Item {
    private static final String TAG_CREATION_TIME = "CreationTime";
    // private static final long EXPIRATION_TICKS = 1200; // Moved to Config

    public QuantumKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide) {
            getOrCreateCreationTime(stack, level);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            if (!stack.hasTag() || !stack.getTag().contains(TAG_CREATION_TIME)) {
                getOrCreateCreationTime(stack, level);
            }
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) {
            if (!stack.hasTag() || !stack.getTag().contains(TAG_CREATION_TIME)) {
                getOrCreateCreationTime(stack, entity.level());
            }
        }
        return false;
    }

    private static void getOrCreateCreationTime(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_CREATION_TIME)) {
            tag.putLong(TAG_CREATION_TIME, level.getGameTime());
        }
    }

    public static boolean isExpired(ItemStack stack, Level level) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_CREATION_TIME)) {
            return false; 
        }
        long creationTime = stack.getTag().getLong(TAG_CREATION_TIME);
        long elapsed = level.getGameTime() - creationTime;
        
        return elapsed > Config.QuantumKeyExpiration;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (stack.hasTag() && stack.getTag().contains(TAG_CREATION_TIME)) {
            long creationTime = stack.getTag().getLong(TAG_CREATION_TIME);
            long currentTime = level != null ? level.getGameTime() : 0;
            long elapsed = currentTime - creationTime;
            long remaining = Config.QuantumKeyExpiration - elapsed;

            if (remaining > 0) {
                long seconds = remaining / 20;
                tooltipComponents.add(Component.translatable("item.mysticartifacts.quantum_key.expires_in", seconds).withStyle(ChatFormatting.GRAY));
            } else {
                tooltipComponents.add(Component.translatable("item.mysticartifacts.quantum_key.expired").withStyle(ChatFormatting.RED));
            }
        } else {
            tooltipComponents.add(Component.translatable("item.mysticartifacts.quantum_key.initializing").withStyle(ChatFormatting.GRAY));
        }
    }
}
