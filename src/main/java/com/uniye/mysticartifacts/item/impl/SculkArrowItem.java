package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.SculkArrow;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SculkArrowItem extends ArrowItem {
    public SculkArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new SculkArrow(ModEntities.SCULK_ARROW.get(), level, shooter);
    }
}
