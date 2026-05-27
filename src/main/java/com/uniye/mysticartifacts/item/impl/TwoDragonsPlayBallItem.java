package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.entity.TwoDragonsFanEntity;
import com.uniye.mysticartifacts.entity.TwoDragonsPlayBallEntity;
import com.uniye.mysticartifacts.util.TwoDragonsState;
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

        boolean hasFire = TwoDragonsState.hasFire(player);
        boolean hasIce = TwoDragonsState.hasIce(player);
        boolean activeFire = TwoDragonsState.isActiveFire(player);
        boolean activeIce = TwoDragonsState.isActiveIce(player);

        boolean canActivateFire = hasFire && !activeFire;
        boolean canActivateIce = hasIce && !activeIce;

        if (!canActivateFire && !canActivateIce) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            stack.hurtAndBreak(3, player, (p) -> p.broadcastBreakEvent(hand));

            if (canActivateFire) {
                TwoDragonsState.setActiveFire(player, true);
                TwoDragonsPlayBallEntity dragon1 = new TwoDragonsPlayBallEntity(level, player, 5.0f, Config.TwoDragonsRotationTime, true);
                dragon1.setSpinOffset(0.0f);
                level.addFreshEntity(dragon1);
            }

            if (canActivateIce) {
                TwoDragonsState.setActiveIce(player, true);
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

        boolean hasFire = TwoDragonsState.hasFire(player);
        boolean hasIce = TwoDragonsState.hasIce(player);
        boolean activeFire = TwoDragonsState.isActiveFire(player);
        boolean activeIce = TwoDragonsState.isActiveIce(player);

        boolean canThrowFire = hasFire && !activeFire;
        boolean canThrowIce = hasIce && !activeIce;

        if (!canThrowFire && !canThrowIce) return;

        boolean throwFire;

        if (canThrowFire && canThrowIce) {
            String last = TwoDragonsState.getLastThrown(player);
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
            TwoDragonsState.setHasFire(player, false);
            TwoDragonsState.setLastThrown(player, "fire");
        } else {
            TwoDragonsState.setHasIce(player, false);
            TwoDragonsState.setLastThrown(player, "ice");
        }

        TwoDragonsFanEntity fan = new TwoDragonsFanEntity(level, player, throwFire);
        level.addFreshEntity(fan);

        player.getCooldowns().addCooldown(stack.getItem(), 5);
    }
}
