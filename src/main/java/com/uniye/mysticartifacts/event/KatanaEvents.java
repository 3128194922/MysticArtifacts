package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import com.uniye.mysticartifacts.item.impl.KatanaState;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class KatanaEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
            if (entityHitResult.getEntity() instanceof LivingEntity entity) {
                if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MuramasaItem) {
                    ItemStack stack = entity.getUseItem();
                    int ticksUsed = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                    boolean isPerfect = ticksUsed <= Config.KatanaPerfectBlockWindow;
                        
                    event.setCanceled(true);
                        
                    Projectile projectile = event.getProjectile();
                    Entity attacker = projectile.getOwner();
                    if (isPerfect) {
                        projectile.setOwner(entity);

                        Vec3 lookVec = entity.getLookAngle();
                        projectile.shoot(lookVec.x, lookVec.y, lookVec.z, 1.5F, 0.0F);
                    } else if (!entity.level().isClientSide) {
                        projectile.discard();
                    }
                        
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.KATANA_BLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F + (entity.level().random.nextFloat() - entity.level().random.nextFloat()) * 0.2F);
                        
                    entity.getUseItem().hurtAndBreak(1, entity, (e) -> e.broadcastBreakEvent(entity.getUsedItemHand()));
                        
                    if (!entity.level().isClientSide) {
                        KatanaState.addEnergy(stack, KatanaState.BLOCK_ENERGY);
                        MinecraftForge.EVENT_BUS.post(new KatanaBlockEvent(
                                entity,
                                attacker,
                                projectile,
                                null,
                                0.0F,
                                isPerfect
                        ));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MuramasaItem) {
            ItemStack stack = entity.getUseItem();
             if (!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                      
                  int ticksUsed = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                  boolean isPerfect = ticksUsed <= Config.KatanaPerfectBlockWindow;
 
                  event.setCanceled(true);
                     
                  if (!(event.getSource().getDirectEntity() instanceof Projectile)) {
                        Entity sourceEntity = event.getSource().getEntity();
                        if (isPerfect
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

                    entity.getUseItem().hurtAndBreak(1, entity, (e) -> e.broadcastBreakEvent(entity.getUsedItemHand()));
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.KATANA_BLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F + (entity.level().random.nextFloat() - entity.level().random.nextFloat()) * 0.2F);
                    if (!entity.level().isClientSide) {
                        KatanaState.addEnergy(entity.getUseItem(), KatanaState.BLOCK_ENERGY);
                        MinecraftForge.EVENT_BUS.post(new KatanaBlockEvent(
                                entity,
                                event.getSource().getEntity(),
                                event.getSource().getDirectEntity() instanceof Projectile projectile ? projectile : null,
                                event.getSource(),
                                event.getAmount(),
                                isPerfect
                        ));
                    }
                  }
            }
        }
    }
}
