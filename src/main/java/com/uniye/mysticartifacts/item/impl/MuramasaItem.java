package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.List;
import java.util.UUID;

public class MuramasaItem extends SwordItem {
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("e0f4e6d2-8b4e-4f3b-9c7a-1a2b3c4d5e6f");
    private static final AttributeModifier STEP_HEIGHT_MODIFIER = new AttributeModifier(STEP_HEIGHT_UUID, "Muramasa Dash Step Height", 2.0, AttributeModifier.Operation.ADDITION);
    private static final float BLOCK_HEALTH_COST = 2.0F;
    private static final float IAIDO_HEALTH_COST = 6.0F;
    private static final int ENHANCED_DURATION_TICKS = 200;
    private static final String ENHANCED_EXPIRE_TICK_TAG = "EnhancedExpireTick";

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
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) {
             return InteractionResultHolder.fail(itemstack);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && applyDirectHealthCost(player, IAIDO_HEALTH_COST)) {
                return InteractionResultHolder.fail(itemstack);
            }

            Vec3 look = player.getLookAngle();
            Vec3 dashVec = new Vec3(look.x, 0, look.z).normalize().scale(2.0);

            player.push(dashVec.x, dashVec.y, dashVec.z);
            player.hurtMarked = true;

            itemstack.getOrCreateTag().putInt("IaidoTicks", 10);
            itemstack.getOrCreateTag().putLong(ENHANCED_EXPIRE_TICK_TAG, level.getGameTime() + ENHANCED_DURATION_TICKS);

            AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
            if (stepHeight != null && !stepHeight.hasModifier(STEP_HEIGHT_MODIFIER)) {
                stepHeight.addTransientModifier(STEP_HEIGHT_MODIFIER);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!level.isClientSide) {
                AABB dashBox = player.getBoundingBox().expandTowards(dashVec).inflate(1.0);
                List<Entity> targets = level.getEntities(player, dashBox, e -> e instanceof LivingEntity && e != player);

                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (float) Config.KatanaDashDamageMultiplier;

                for (Entity target : targets) {
                    if (target instanceof LivingEntity livingTarget) {
                        float enchantmentDamage = EnchantmentHelper.getDamageBonus(itemstack, livingTarget.getMobType());

                        if (livingTarget.hurt(ModDamageTypes.getSource(level, ModDamageTypes.IAIDO, player, player), baseDamage + enchantmentDamage)) {
                            if (enchantmentDamage > 0.0F && level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5, livingTarget.getZ(),
                                        10, 0.5, 0.5, 0.5, 0.1);
                            }
                            EnchantmentHelper.doPostHurtEffects(livingTarget, player);
                            EnchantmentHelper.doPostDamageEffects(player, livingTarget);

                            itemstack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                        }
                    }
                }

                if (level instanceof ServerLevel serverLevel) {
                    for (double d = 0; d <= 1; d += 0.2) {
                        double x = player.getX() + dashVec.x * d;
                        double y = player.getY() + player.getEyeHeight() * 0.5 + dashVec.y * d;
                        double z = player.getZ() + dashVec.z * d;
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
                    }
                }
            }

            player.swing(hand);
            return InteractionResultHolder.success(itemstack);
        }

        if (!level.isClientSide && applyDirectHealthCost(player, BLOCK_HEALTH_COST)) {
            return InteractionResultHolder.fail(itemstack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
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
            if (stack.getOrCreateTag().contains("IaidoTicks")) {
                int ticks = stack.getOrCreateTag().getInt("IaidoTicks");
                if (ticks > 0) {
                    stack.getOrCreateTag().putInt("IaidoTicks", ticks - 1);
                } else {
                    stack.getOrCreateTag().remove("IaidoTicks");
                    AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
                    if (stepHeight != null && stepHeight.hasModifier(STEP_HEIGHT_MODIFIER)) {
                        stepHeight.removeModifier(STEP_HEIGHT_MODIFIER);
                    }
                }
            }

            if (stack.getOrCreateTag().contains(ENHANCED_EXPIRE_TICK_TAG)) {
                long expireTick = stack.getOrCreateTag().getLong(ENHANCED_EXPIRE_TICK_TAG);
                if (expireTick <= level.getGameTime()) {
                    stack.getOrCreateTag().remove(ENHANCED_EXPIRE_TICK_TAG);
                }
            }
        }
    }
    
    public static boolean isInIaido(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt("IaidoTicks") > 0;
    }

    public static boolean isEnhanced(ItemStack stack, Level level, Entity holder) {
        if (!stack.hasTag()) {
            return false;
        }
        long expireTick = stack.getTag().getLong(ENHANCED_EXPIRE_TICK_TAG);
        return expireTick > getCurrentTick(level, holder);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return super.hurtEnemy(stack, target, attacker);
    }

    private static long getCurrentTick(Level level, Entity holder) {
        if (level != null) {
            return level.getGameTime();
        }
        if (holder != null) {
            return holder.level().getGameTime();
        }
        return 0L;
    }

    private static boolean applyDirectHealthCost(Player player, float healthCost) {
        float remain = player.getHealth() - healthCost;
        player.setHealth(Math.max(remain, 0.0F));
        if (remain <= 0.0F) {
            player.kill();
            return true;
        }
        return false;
    }
}
