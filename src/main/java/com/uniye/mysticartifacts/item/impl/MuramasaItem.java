package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.entity.KatanaSlashEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.util.UUID;

/**
 * 武士刀（村雨）：普通模式 / 鬼刀模式。
 *
 * <p>普通模式：左键近战（命中充能+1）；右键格挡，完美弹反抛射体（充能+1），
 * 非完美弹反仅免伤部分伤害（见 Config）；潜行右键居合冲刺（需充能满），
 * 造成一次普通攻击伤害并进入鬼刀模式。</p>
 *
 * <p>鬼刀模式：攻速变为满速；左键（含空挥）发射剑气（最大飞行 5 格，消耗 1 充能）；
 * 受到伤害自动完美弹反（有限次数）；右键为半程居合（纯位移，消耗 1 充能）。
 * 充能耗尽或持续时间结束后回到普通模式。</p>
 */
public class MuramasaItem extends SwordItem {
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("e0f4e6d2-8b4e-4f3b-9c7a-1a2b3c4d5e6f");
    private static final AttributeModifier STEP_HEIGHT_MODIFIER = new AttributeModifier(
            STEP_HEIGHT_UUID, "Muramasa Dash Step Height", 2.0, AttributeModifier.Operation.ADDITION);
    private static final double CLOSED_KNOCKBACK_STRENGTH = 3.0D;
    private static final double NORMAL_DASH_STRENGTH = 2.0D;
    private static final double GHOST_DASH_STRENGTH = 1.0D;
    private static final int GHOST_SLASH_COOLDOWN_TICKS = 5;
    private static final int DASH_COOLDOWN_TICKS = 10;

    public MuramasaItem(Properties properties) {
        super(Tiers.IRON, 4, -2.4F, properties);
    }

    // ---- 状态查询 ----

    public static boolean isOpen(ItemStack stack, Level level) {
        return KatanaState.isOpen(stack, level);
    }

    public static boolean isEnhanced(ItemStack stack, Level level, Entity holder) {
        return KatanaState.isOpen(stack, level, holder);
    }

    /** 鬼刀模式下受伤自动完美弹反是否可用。 */
    public static boolean canAutoParry(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        return stack.getItem() instanceof MuramasaItem
                && KatanaState.isOpen(stack, entity.level())
                && KatanaState.getAutoParryLeft(stack) > 0;
    }

    // ---- 属性：鬼刀模式攻速归零（去掉 -2.4 修正），普通模式保持原值 ----

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                4.0 + Tiers.IRON.getAttackDamageBonus(), AttributeModifier.Operation.ADDITION));
        if (!KatanaState.hasGhostTag(stack)) {
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                    -2.4F, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    // ---- 无耐久，耐久条显示充能 ----

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return KatanaState.getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * KatanaState.getEnergy(stack) / (float) KatanaState.maxCharge());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return KatanaState.isFull(stack) ? 0xFFD700 : 0xAA00FF;
    }

    // ---- 交互 ----

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

        if (KatanaState.isOpen(stack, level)) {
            // 鬼刀：右键半程居合，纯位移，消耗 1 充能
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                if (!KatanaState.consumeCharge(stack, KatanaState.CHARGE_PER_USE)) {
                    return InteractionResultHolder.fail(stack);
                }
                performDash(serverLevel, player, stack, GHOST_DASH_STRENGTH, false);
                player.getCooldowns().addCooldown(this, DASH_COOLDOWN_TICKS);
                closeIfEmpty(player, stack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.swing(hand);
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            // 普通：潜行右键居合，需充能满；冲刺并造成一次普通攻击伤害，进入鬼刀
            if (!KatanaState.isFull(stack)) {
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                performDash(serverLevel, player, stack, NORMAL_DASH_STRENGTH, true);
                KatanaState.openGhost(stack, level);
                player.getCooldowns().addCooldown(this, DASH_COOLDOWN_TICKS);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.swing(hand);
            return InteractionResultHolder.success(stack);
        }

        // 普通：右键格挡（弹反判定见 KatanaEvents）
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

    /**
     * 鬼刀左键（含空挥）发射剑气并消耗 1 充能；服务端入口。
     * 由 {@code KatanaEvents#onAttackEntity} 与 {@code KatanaSwingPacket} 调用。
     */
    public static boolean fireGhostSlash(Player player) {
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MuramasaItem)
                || !KatanaState.isOpen(stack, player.level())
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }
        if (!KatanaState.consumeCharge(stack, KatanaState.CHARGE_PER_USE)) {
            return false;
        }
        KatanaSlashEntity.createGhostSlash(serverLevel, player, stack);
        player.getCooldowns().addCooldown(stack.getItem(), GHOST_SLASH_COOLDOWN_TICKS);
        player.swing(InteractionHand.MAIN_HAND);
        closeIfEmpty(player, stack);
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof Player player && !KatanaState.isOpen(stack, player.level())
                && !player.level().isClientSide) {
            target.knockback(CLOSED_KNOCKBACK_STRENGTH,
                    player.getX() - target.getX(), player.getZ() - target.getZ());
            KatanaState.addEnergy(stack, KatanaState.CHARGE_PER_HIT);
        }
        return result;
    }

    // ---- 内部工具 ----

    private static void performDash(ServerLevel level, Player player, ItemStack stack,
                                    double strength, boolean damageTargets) {
        Vec3 dashVector = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(strength);
        KatanaSlashEntity.createDash(level, player, stack, dashVector, damageTargets);
        player.push(dashVector.x, dashVector.y, dashVector.z);
        player.hurtMarked = true;
        addDashStepHeight(player);
    }

    /** 鬼刀充能耗尽时立即退出鬼刀模式（属性同步回普通）。 */
    private static void closeIfEmpty(Player player, ItemStack stack) {
        if (KatanaState.isOpen(stack, player.level()) && KatanaState.getEnergy(stack) <= 0) {
            KatanaState.close(stack);
            removeDashStepHeight(player);
        }
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
