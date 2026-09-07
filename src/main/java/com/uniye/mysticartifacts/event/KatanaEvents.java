package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.item.impl.KatanaState;
import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 武士刀战斗事件：
 * <ul>
 * <li>鬼刀模式左键（含命中实体）改为发射剑气；</li>
 * <li>右键格挡：完美窗口内弹反弹射体（重定向反打）并充能，窗口外仅免伤部分伤害；</li>
 * <li>鬼刀模式受伤自动完美弹反（有限次数）。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class KatanaEvents {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MuramasaItem)
                || !KatanaState.isOpen(stack, player.level())) {
            return;
        }

        // 鬼刀模式：左键改为发射剑气，取消近战
        event.setCanceled(true);
        if (!player.level().isClientSide) {
            MuramasaItem.fireGhostSlash(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHitResult)) {
            return;
        }
        if (!(entityHitResult.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        ItemStack usedStack = entity.getUseItem();
        boolean manualParry = entity.isUsingItem() && usedStack.getItem() instanceof MuramasaItem;
        boolean autoParry = MuramasaItem.canAutoParry(entity);
        if (!manualParry && !autoParry) {
            return;
        }

        if (!isPerfectParry(entity, usedStack, manualParry, autoParry)) {
            // 非完美格挡：弹射体正常命中，伤害在 LivingHurtEvent 中减免
            return;
        }

        // 完美弹反（手动 / 鬼刀自动）：弹射体重定向反打
        event.setCanceled(true);
        Projectile projectile = event.getProjectile();
        Entity attacker = projectile.getOwner();
        projectile.setOwner(entity);
        Vec3 lookVec = entity.getLookAngle();
        projectile.shoot(lookVec.x, lookVec.y, lookVec.z, 1.5F, 0.0F);

        playBlockSound(entity);
        if (!entity.level().isClientSide) {
            consumeParry(entity, usedStack, manualParry, autoParry);
            MinecraftForge.EVENT_BUS.post(new KatanaBlockEvent(
                    entity,
                    attacker,
                    projectile,
                    null,
                    0.0F,
                    true
            ));
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        ItemStack usedStack = entity.getUseItem();
        boolean manualParry = entity.isUsingItem() && usedStack.getItem() instanceof MuramasaItem;
        boolean autoParry = MuramasaItem.canAutoParry(entity);
        if (!manualParry && !autoParry) {
            return;
        }

        if (!isPerfectParry(entity, usedStack, manualParry, autoParry)) {
            // 非完美格挡：伤害继续，由 LivingHurtEvent 免伤部分伤害
            playBlockSound(entity);
            if (!entity.level().isClientSide) {
                MinecraftForge.EVENT_BUS.post(new KatanaBlockEvent(
                        entity,
                        event.getSource().getEntity(),
                        event.getSource().getDirectEntity() instanceof Projectile projectile ? projectile : null,
                        event.getSource(),
                        event.getAmount(),
                        false
                ));
            }
            return;
        }

        // 完美弹反（手动 / 鬼刀自动）：完全免伤；弹射体已在 ProjectileImpact 中重定向
        event.setCanceled(true);
        if (event.getSource().getDirectEntity() instanceof Projectile) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!entity.level().isClientSide
                && sourceEntity instanceof LivingEntity attacker
                && attacker != entity
                && attacker.isAlive()) {
            float reflectDamage = event.getAmount();
            if (reflectDamage > 0.0F && entity instanceof Player player) {
                attacker.hurt(entity.damageSources().playerAttack(player), reflectDamage);
            } else if (reflectDamage > 0.0F) {
                attacker.hurt(entity.damageSources().mobAttack(entity), reflectDamage);
            }
        }

        playBlockSound(entity);
        if (!entity.level().isClientSide) {
            consumeParry(entity, usedStack, manualParry, autoParry);
            MinecraftForge.EVENT_BUS.post(new KatanaBlockEvent(
                    entity,
                    event.getSource().getEntity(),
                    null,
                    event.getSource(),
                    event.getAmount(),
                    true
            ));
        }
    }

    /** 非完美格挡期间的伤害减免（完美窗口内已在 LivingAttackEvent 中完全免伤，不会走到这里）。 */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.isUsingItem() || !(entity.getUseItem().getItem() instanceof MuramasaItem)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        ItemStack stack = entity.getUseItem();
        int ticksUsed = stack.getUseDuration() - entity.getUseItemRemainingTicks();
        if (ticksUsed <= Config.KatanaPerfectBlockWindow) {
            return;
        }
        event.setAmount(event.getAmount() * (float) (1.0D - Config.KatanaImperfectParryReduction));
    }

    private static boolean isPerfectParry(LivingEntity entity, ItemStack usedStack,
                                          boolean manualParry, boolean autoParry) {
        if (autoParry) {
            return true;
        }
        int ticksUsed = usedStack.getUseDuration() - entity.getUseItemRemainingTicks();
        return manualParry && ticksUsed <= Config.KatanaPerfectBlockWindow;
    }

    /** 结算弹反收益：鬼刀自动弹反消耗次数，普通模式手动完美弹反充能。 */
    private static void consumeParry(LivingEntity entity, ItemStack usedStack,
                                     boolean manualParry, boolean autoParry) {
        if (autoParry) {
            KatanaState.consumeAutoParry(entity.getMainHandItem());
        } else if (!KatanaState.isOpen(entity.getMainHandItem(), entity.level())) {
            KatanaState.addEnergy(usedStack, KatanaState.CHARGE_PER_PARRY);
        }
    }

    private static void playBlockSound(LivingEntity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.KATANA_BLOCK.get(), SoundSource.PLAYERS, 1.0F,
                1.0F + (entity.level().random.nextFloat() - entity.level().random.nextFloat()) * 0.2F);
    }
}
