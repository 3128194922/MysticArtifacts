package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.EnderKunaiEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.util.EnderKunaiTracker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class EnderKunaiItem extends Item {
    public EnderKunaiItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (pLevel.getRandom().nextFloat() * 0.4F + 0.8F));
        
        if (!pLevel.isClientSide) {
            EnderKunaiEntity kunai = new EnderKunaiEntity(ModEntities.ENDER_KUNAI.get(), pLevel, pPlayer);
            kunai.setItem(itemstack);
            kunai.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 1.5F, 1.0F);
            pLevel.addFreshEntity(kunai);
            if (pPlayer instanceof ServerPlayer serverPlayer) {
                EnderKunaiTracker.addKunai(serverPlayer, kunai.getUUID());
            }
        }

        pPlayer.awardStat(Stats.ITEM_USED.get(this));
        if (!pPlayer.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }
}
