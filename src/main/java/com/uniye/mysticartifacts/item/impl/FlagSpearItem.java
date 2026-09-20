package com.uniye.mysticartifacts.item.impl;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class FlagSpearItem extends SwordItem {
    public FlagSpearItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties.stacksTo(1));
    }
}
