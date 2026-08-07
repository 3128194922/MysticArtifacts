package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModDamageTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class GrieferSpearItem extends SpearItem {
    private static final float PIERCE_EXPLOSION_RADIUS = 2.0F;

    public GrieferSpearItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    protected void pierceAttack(Level level, Player player, ItemStack stack) {
        for (EntityHitResult entityHitResult : getHitEntitiesAlong(player, HITBOX_MARGIN, e -> canHitEntity(player, e) && isWithinRange(player, e))) {
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof net.minecraftforge.entity.PartEntity<?> partEntity) {
                entity = partEntity.getParent();
            }

            if (entity instanceof LivingEntity) {
                EnchantmentHelper.doPostHurtEffects((LivingEntity) entity, player);
            }
            EnchantmentHelper.doPostDamageEffects(player, entity);
            player.setLastHurtMob(entity);

            DamageSource source = ModDamageTypes.getSource(level, ModDamageTypes.SPEAR, player, player);

            level.explode(
                    player,
                    source,
                    null,
                    entity.getX(), entity.getY(), entity.getZ(),
                    PIERCE_EXPLOSION_RADIUS,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            player.stopUsingItem();
            player.getCooldowns().addCooldown(this, CONTACT_COOLDOWN_TICKS);
            break;
        }
    }
}
