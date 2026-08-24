package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.AncestorsLetterItem;
import com.uniye.mysticartifacts.network.AncestorsLetterSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 先祖的信（服务端状态机）：
 *
 * 正常 -> 受到致死伤害：免疫此次伤害并立即判定，50% 美德 / 50% 折磨。
 * 美德 -> 受伤 -25%；致死时 75% 拒绝死亡（回满血、回到正常）；持续一个游戏日后回到正常。
 * 折磨 -> 受伤 +15%；受伤时 25% 直接死亡（genericKill，无视图腾）；无法卸下饰品。
 *
 * 死亡、睡觉、取下饰品（美德/正常时允许取下）都会回到正常状态。
 *
 * 防递归设计：
 * - 折磨的 25% 即死不直接在伤害事件里嵌套 hurt()，而是记入 PENDING_TORMENT_DEATH，
 *   延迟到下一 tick（本次伤害流程完全结束后）执行，天然避免事件重入；
 * - internalKill 标志在执行内部伤害期间让所有本类处理器直接跳过，双保险。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class AncestorsLetterEvents {

    // 已判定为即死、等待下一 tick 执行的折磨玩家
    private static final Set<UUID> PENDING_TORMENT_DEATH = new HashSet<>();

    // 饰品自身造成的内部伤害正在进行中（防递归）
    private static boolean internalKill = false;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 执行上一 tick 判定的折磨即死（此时外部伤害流程已结束，无重入风险）
        if (PENDING_TORMENT_DEATH.remove(player.getUUID())) {
            if (player.isAlive()
                    && AncestorsLetterItem.isWearing(player)
                    && AncestorsLetterItem.getState(player) == AncestorsLetterItem.STATE_TORMENT) {
                internalKill = true;
                try {
                    // genericKill 与 /kill 同机制：无视无敌帧、无视图腾
                    player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                } finally {
                    internalKill = false;
                }
            }
        }

        if (!AncestorsLetterItem.isWearing(player)) {
            // 取下饰品：清空状态（美德效果随之消失）
            if (AncestorsLetterItem.hasState(player)) {
                AncestorsLetterItem.clearState(player);
                AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_NORMAL);
            }
            return;
        }

        // 美德到期：按触发时记录的游戏时间推算结束时间
        if (AncestorsLetterItem.getState(player) == AncestorsLetterItem.STATE_VIRTUE
                && player.level().getGameTime() >= AncestorsLetterItem.getVirtueEnd(player)) {
            AncestorsLetterItem.clearState(player);
            AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_NORMAL);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (internalKill) return;
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AncestorsLetterItem.isWearing(player)) return;

        float amount = event.getAmount();
        // /kill 类超常规伤害不参与修正
        if (amount <= 0.0F || amount >= 1.0E30F) return;

        int state = AncestorsLetterItem.getState(player);
        if (state == AncestorsLetterItem.STATE_TORMENT) {
            event.setAmount(amount * (1.0F + (float) Config.AncestorLetterTormentDamageBonus));
        } else if (state == AncestorsLetterItem.STATE_VIRTUE) {
            event.setAmount(amount * (1.0F - (float) Config.AncestorLetterVirtueDamageReduction));
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (internalKill) return;
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AncestorsLetterItem.isWearing(player)) return;

        // 与原版图腾一致：无视无敌类伤害（/kill、虚空）不可被免疫
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        int state = AncestorsLetterItem.getState(player);
        float amount = event.getAmount();

        if (state == AncestorsLetterItem.STATE_TORMENT) {
            // 受伤时 25% 直接死亡；实际处决延迟到下一 tick，防止伤害事件递归
            if (amount > 0.0F && player.getRandom().nextFloat() < (float) Config.AncestorLetterTormentDeathChance) {
                PENDING_TORMENT_DEATH.add(player.getUUID());
            }
            return;
        }

        // LivingDamageEvent 的 amount 为护甲/抗性/伤害吸收结算后、即将扣血的最终值
        boolean lethal = amount >= player.getHealth();
        if (!lethal) return;

        if (state == AncestorsLetterItem.STATE_NORMAL) {
            // 免疫此次死亡并立即判定：50% 美德 / 50% 折磨
            event.setCanceled(true);
            player.setHealth(Math.max(player.getHealth(), 1.0F));

            boolean virtue = player.getRandom().nextFloat() < (float) Config.AncestorLetterVirtueChance;
            if (virtue) {
                AncestorsLetterItem.setState(player, AncestorsLetterItem.STATE_VIRTUE);
                AncestorsLetterItem.setVirtueEnd(player,
                        player.level().getGameTime() + Config.AncestorLetterVirtueDurationTicks);
                AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_VIRTUE);
            } else {
                AncestorsLetterItem.setState(player, AncestorsLetterItem.STATE_TORMENT);
                AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_TORMENT);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else { // STATE_VIRTUE
            // 美德：死亡时 75% 拒绝死亡，回满血并回到正常状态
            if (player.getRandom().nextFloat() < (float) Config.AncestorLetterVirtueRefuseChance) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                AncestorsLetterItem.clearState(player);
                AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_NORMAL);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            // 否则正常死亡，状态在 LivingDeathEvent 中重置
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 死亡：丢弃未执行的折磨处决（避免误杀重生后的玩家），并重置为正常
        PENDING_TORMENT_DEATH.remove(player.getUUID());
        if (AncestorsLetterItem.hasState(player)) {
            AncestorsLetterItem.clearState(player);
            AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_NORMAL);
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 睡觉：回到正常状态
        if (AncestorsLetterItem.hasState(player)) {
            AncestorsLetterItem.clearState(player);
            AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.STATE_NORMAL);
        }
    }

    // 登录/重生后向客户端同步当前状态（刷新 HUD 图标）
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.getState(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AncestorsLetterSyncPacket.sendTo(player, AncestorsLetterItem.getState(player));
        }
    }
}
