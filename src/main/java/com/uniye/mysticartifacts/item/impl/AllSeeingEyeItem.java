package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.client.event.AllSeeingEyeClientHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class AllSeeingEyeItem extends Item {

    public AllSeeingEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AllSeeingEyeClientHandler.onUse());
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        return InteractionResultHolder.pass(stack);
    }
}
