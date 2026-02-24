package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.AirburstArrowEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AirburstArrowItem extends ArrowItem {
    public AirburstArrowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new AirburstArrowEntity(ModEntities.AIRBURST_ARROW.get(), level, shooter);
    }

}
