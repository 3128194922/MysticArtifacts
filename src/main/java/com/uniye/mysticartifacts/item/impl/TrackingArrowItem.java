package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.TrackingArrowEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TrackingArrowItem extends ArrowItem {
    public TrackingArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new TrackingArrowEntity(ModEntities.TRACKING_ARROW.get(), level, shooter);
    }
}
