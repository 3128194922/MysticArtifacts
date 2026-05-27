package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.TrackingArrowEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TrackingArrowItem extends Item {
    public TrackingArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            if (player.getHealth() <= 2.0F) {
                player.kill();
            } else {
                player.hurt(player.damageSources().magic(), 2.0F);
            }
            TrackingArrowEntity entity = new TrackingArrowEntity(level, player);
            level.addFreshEntity(entity);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
