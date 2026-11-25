package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.TntArrowEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TntArrowItem extends ArrowItem {
    public TntArrowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new TntArrowEntity(ModEntities.TNT_ARROW.get(), level, shooter);
    }
}
