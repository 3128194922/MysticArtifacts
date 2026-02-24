package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import com.uniye.mysticartifacts.util.ParticleTextAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class KatanaEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
            if (entityHitResult.getEntity() instanceof LivingEntity entity) {
                if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MuramasaItem) {
                    ItemStack stack = entity.getUseItem();
                    if (MuramasaItem.getSharpness(stack) >= Config.KatanaStacksBlockCost) {
                        int ticksUsed = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                        boolean isPerfect = ticksUsed <= Config.KatanaPerfectBlockWindow;

                        if (!isPerfect) {
                            MuramasaItem.consumeSharpness(stack, Config.KatanaStacksBlockCost);
                        
                            if (MuramasaItem.getSharpness(stack) < Config.KatanaStacksBlockCost) {
                                entity.stopUsingItem();
                                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                                    player.getCooldowns().addCooldown(stack.getItem(), 100);
                                }
                            }
                        }
                        
                        event.setCanceled(true); 
                        
                        Projectile projectile = event.getProjectile();
                        projectile.setOwner(entity); 
                        
                        Vec3 lookVec = entity.getLookAngle();
                        projectile.shoot(lookVec.x, lookVec.y, lookVec.z, 1.5F, 0.0F);
                        
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.KATANA_BLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F + (entity.level().random.nextFloat() - entity.level().random.nextFloat()) * 0.2F);
                        
                        entity.getUseItem().hurtAndBreak(1, entity, (e) -> e.broadcastBreakEvent(entity.getUsedItemHand()));
                        
                        if (entity instanceof ServerPlayer sp) {
                            if (isPerfect) {
                                ParticleTextAPI.sendInFront(sp, "完美弹反！", 0xFFAA00);
                            } else {
                                ParticleTextAPI.sendInFront(sp, "弹反！", 0x00FF00);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (entity.getMainHandItem().getItem() instanceof MuramasaItem) {
             if (MuramasaItem.isInIaido(entity.getMainHandItem())) {
                 event.setCanceled(true);
                 return;
             }
        }
        
        if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MuramasaItem) {
            ItemStack stack = entity.getUseItem();
             if (MuramasaItem.getSharpness(stack) >= Config.KatanaStacksBlockCost) {
                 if (!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                      
                      int ticksUsed = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                      boolean isPerfect = ticksUsed <= Config.KatanaPerfectBlockWindow;

                      if (!isPerfect) {
                          MuramasaItem.consumeSharpness(stack, Config.KatanaStacksBlockCost);
                      
                          if (MuramasaItem.getSharpness(stack) < Config.KatanaStacksBlockCost) {
                              entity.stopUsingItem();
                              if (entity instanceof net.minecraft.world.entity.player.Player player) {
                                  player.getCooldowns().addCooldown(stack.getItem(), 100);
                              }
                          }
                      }
 
                      event.setCanceled(true);
                     
                     if (!(event.getSource().getDirectEntity() instanceof Projectile)) {
                        entity.getUseItem().hurtAndBreak(1, entity, (e) -> e.broadcastBreakEvent(entity.getUsedItemHand()));
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.KATANA_BLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F + (entity.level().random.nextFloat() - entity.level().random.nextFloat()) * 0.2F);
                        if (entity instanceof ServerPlayer sp) {
                            if (isPerfect) {
                                ParticleTextAPI.sendInFront(sp, "完美格挡！", 0xFFAA00);
                            } else {
                                ParticleTextAPI.sendInFront(sp, "格挡！", 0x00FF00);
                            }
                        }
                     }
                }
            }
        }
    }
}
