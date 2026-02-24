package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.entity.PokerCardEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class PokerCardItem extends Item {
    public PokerCardItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        pPlayer.startUsingItem(pHand);
        
        recallCards(pLevel, pPlayer);

        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.POKER_RECALL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.consume(itemstack);
    }

    public static void throwCard(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return;

        if (!level.isClientSide) {
             PokerCardEntity card = new PokerCardEntity(ModEntities.POKER_CARD.get(), level, player);
             card.setItem(stack);
             card.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
             level.addFreshEntity(card);
             
             player.getCooldowns().addCooldown(stack.getItem(), Config.PokerCardCooldown);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POKER_THROW.get(), SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
    }
    
    private void recallCards(Level level, Player player) {
        if (level.isClientSide) return;
        AABB area = player.getBoundingBox().inflate(Config.PokerCardRetrievalDistance);
        List<PokerCardEntity> cards = level.getEntitiesOfClass(PokerCardEntity.class, area);
        for (PokerCardEntity card : cards) {
            if (card.getOwner() == player && card.isLandedState()) {
                 card.startRecall();
            }
        }
    }
}
