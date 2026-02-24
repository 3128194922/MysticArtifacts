package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.entity.TwoDragonsFanEntity;
import com.uniye.mysticartifacts.entity.TwoDragonsPlayBallEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class TwoDragonsPlayBallItem extends Item {
    public TwoDragonsPlayBallItem(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON).durability(250));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains("has_fire")) nbt.putBoolean("has_fire", true);
        if (!nbt.contains("has_ice")) nbt.putBoolean("has_ice", true);
        if (!nbt.contains("active_fire")) nbt.putBoolean("active_fire", false);
        if (!nbt.contains("active_ice")) nbt.putBoolean("active_ice", false);

        boolean hasFire = nbt.getBoolean("has_fire");
        boolean hasIce = nbt.getBoolean("has_ice");
        boolean activeFire = nbt.getBoolean("active_fire");
        boolean activeIce = nbt.getBoolean("active_ice");

        boolean canActivateFire = hasFire && !activeFire;
        boolean canActivateIce = hasIce && !activeIce;
        
        if (!canActivateFire && !canActivateIce) {
            return InteractionResultHolder.fail(stack);
        }
        
        if (!level.isClientSide) {
            stack.hurtAndBreak(3, player, (p) -> p.broadcastBreakEvent(hand));
            
            if (canActivateFire) {
                nbt.putBoolean("active_fire", true);
                TwoDragonsPlayBallEntity dragon1 = new TwoDragonsPlayBallEntity(level, player, 5.0f, Config.TwoDragonsRotationTime, true);
                dragon1.setSpinOffset(0.0f);
                level.addFreshEntity(dragon1);
            }

            if (canActivateIce) {
                nbt.putBoolean("active_ice", true);
                TwoDragonsPlayBallEntity dragon2 = new TwoDragonsPlayBallEntity(level, player, 5.0f, Config.TwoDragonsRotationTime, false);
                dragon2.setSpinOffset((float) Math.PI); 
                level.addFreshEntity(dragon2);
            }
            
            player.getCooldowns().addCooldown(this, Config.TwoDragonsCooldown);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void throwFan(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains("active_fire")) nbt.putBoolean("active_fire", false);
        if (!nbt.contains("active_ice")) nbt.putBoolean("active_ice", false);
        
        if (!nbt.contains("has_fire")) nbt.putBoolean("has_fire", true);
        if (!nbt.contains("has_ice")) nbt.putBoolean("has_ice", true);
        
        boolean hasFire = nbt.getBoolean("has_fire");
        boolean hasIce = nbt.getBoolean("has_ice");
        boolean activeFire = nbt.getBoolean("active_fire");
        boolean activeIce = nbt.getBoolean("active_ice");
        
        boolean canThrowFire = hasFire && !activeFire;
        boolean canThrowIce = hasIce && !activeIce;
        
        if (!canThrowFire && !canThrowIce) return;
        
        boolean throwFire;
        
        if (canThrowFire && canThrowIce) {
            String last = nbt.getString("last_thrown");
            if (last.equals("fire")) {
                throwFire = false;
            } else {
                throwFire = true;
            }
        } else if (canThrowFire) {
            throwFire = true;
        } else {
            throwFire = false;
        }
        
        if (throwFire) {
            nbt.putBoolean("has_fire", false);
            nbt.putString("last_thrown", "fire");
        } else {
            nbt.putBoolean("has_ice", false);
            nbt.putString("last_thrown", "ice");
        }
        
        TwoDragonsFanEntity fan = new TwoDragonsFanEntity(level, player, throwFire);
        level.addFreshEntity(fan);
        
        player.getCooldowns().addCooldown(stack.getItem(), 5);
    }
}
