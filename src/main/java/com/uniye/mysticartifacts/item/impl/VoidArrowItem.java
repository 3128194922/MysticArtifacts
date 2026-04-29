package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.VoidArrowEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VoidArrowItem extends ArrowItem {
    public VoidArrowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new VoidArrowEntity(ModEntities.VOID_ARROW.get(), level, shooter);
    }
}
