package com.uniye.mysticartifacts.item.impl;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

public class MuramasaItem extends SwordItem {
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("e0f4e6d2-8b4e-4f3b-9c7a-1a2b3c4d5e6f");
    private static final AttributeModifier STEP_HEIGHT_MODIFIER = new AttributeModifier(
            STEP_HEIGHT_UUID, "Muramasa Dash Step Height", 2.0, AttributeModifier.Operation.ADDITION);
    private static final double CLOSED_KNOCKBACK_STRENGTH = 3.0D;

    public MuramasaItem(Properties properties) {
        super(Tiers.IRON, 4, -2.4F, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            if (!KatanaState.canDash(stack, level)) {
                return InteractionResultHolder.fail(stack);
            }

            if (!level.isClientSide) {
                Vec3 look = player.getLookAngle();
                Vec3 dashVector = new Vec3(look.x, 0.0D, look.z).normalize().scale(2.0D);
                KatanaState.consumeDash(stack);
                player.push(dashVector.x, dashVector.y, dashVector.z);
                player.hurtMarked = true;
                addDashStepHeight(player);
                KatanaState.open(stack, level, KatanaState.OPEN_DURATION_TICKS);
                player.getCooldowns().addCooldown(this, 10);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.swing(hand);
            return InteractionResultHolder.success(stack);
        }

        if (KatanaState.isOpen(stack, level)) {
            player.swing(hand);
            return InteractionResultHolder.success(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft);
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, 20);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            KatanaState.clearExpired(stack, level.getGameTime());
            if (!KatanaState.isOpen(stack, level)) {
                removeDashStepHeight(player);
            }
        }
    }

    public static boolean isOpen(ItemStack stack, Level level) {
        return KatanaState.isOpen(stack, level);
    }

    public static boolean isInIaido(ItemStack stack) {
        return false;
    }

    public static boolean isEnhanced(ItemStack stack, Level level, Entity holder) {
        return KatanaState.isOpen(stack, level, holder);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof Player player && !KatanaState.isOpen(stack, player.level())
                && !player.level().isClientSide) {
            target.knockback(CLOSED_KNOCKBACK_STRENGTH,
                    player.getX() - target.getX(), player.getZ() - target.getZ());
            KatanaState.addEnergy(stack, KatanaState.HIT_ENERGY);
        }
        return result;
    }

    private static void addDashStepHeight(Player player) {
        AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight != null && !stepHeight.hasModifier(STEP_HEIGHT_MODIFIER)) {
            stepHeight.addTransientModifier(STEP_HEIGHT_MODIFIER);
        }
    }

    private static void removeDashStepHeight(Player player) {
        AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight != null && stepHeight.hasModifier(STEP_HEIGHT_MODIFIER)) {
            stepHeight.removeModifier(STEP_HEIGHT_MODIFIER);
        }
    }
}
