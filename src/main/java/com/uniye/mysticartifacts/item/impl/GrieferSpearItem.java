package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.util.PseudoExplosion;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    protected void pierceAttack(Level level, LivingEntity player, ItemStack stack) {
        if (level.isClientSide()) return;

        for (EntityHitResult entityHitResult : getHitEntitiesAlong(player, HITBOX_MARGIN, getRangeScale(player),
                e -> canHitEntity(player, e) && isWithinRange(player, e, getRangeScale(player)))) {
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

            // 直接伤害固定为 1，剩余伤害由爆炸承担
            entity.hurt(source, 1.0F);

            detonate(level, player, source, entity);

            if (player instanceof Player realPlayer) {
                realPlayer.stopUsingItem();
                realPlayer.getCooldowns().addCooldown(this, CONTACT_COOLDOWN_TICKS);
            }
            break;
        }
    }

    @Override
    protected boolean onSpearHit(Level level, LivingEntity attacker, Entity target, ItemStack stack) {
        if (attacker instanceof Player) return false;

        DamageSource source = ModDamageTypes.getSource(level, ModDamageTypes.SPEAR, attacker, attacker);
        detonate(level, attacker, source, target);
        return true;
    }

    /** 伪爆炸：只出粒子与音效，范围伤害由 AABB 自行结算，不破坏方块。 */
    private static void detonate(Level level, @Nullable Entity source, DamageSource damageSource, Entity center) {
        Vec3 at = center.getBoundingBox().getCenter();
        PseudoExplosion.detonate(level, source, damageSource, at.x, at.y, at.z, PIERCE_EXPLOSION_RADIUS);
    }
}
