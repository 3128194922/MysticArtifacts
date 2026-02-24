package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.entity.NetherOfVoiceEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetherOfVoiceItem extends ArrowItem {

    public NetherOfVoiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        NetherOfVoiceEntity entity = new NetherOfVoiceEntity(ModEntities.NETHER_OF_VOICE.get(), level, shooter);
        entity.setBaseDamage(Config.NetherOfVoiceDamage);
        return entity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            NetherOfVoiceEntity entity = new NetherOfVoiceEntity(level, player);
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float)Config.NetherOfVoiceSpeed, 1.0F); // Config speed
            entity.setBaseDamage(Config.NetherOfVoiceDamage);
            entity.setNoGravity(true);
            level.addFreshEntity(entity);
        }

        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
