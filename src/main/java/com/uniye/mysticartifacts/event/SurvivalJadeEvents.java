package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.SurvivalJadeItem;
import com.uniye.mysticartifacts.network.SurvivalJadeSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 求生玉（Survival Instinct）服务端逻辑：
 * - 佩戴者任何形式的实际生命值下降都会等量转化为"残影"暂存。
 * - 残影上限 = 最大生命 - 当前生命，确保 残影 + 当前血量 <= 最大生命（满血时残影为 0）。
 * - 残影缓慢衰减：每 SurvivalJadeDecayTicks tick 衰减 1 点（默认 60 tick = 3 秒/HP，可配置）。
 * - 佩戴者造成伤害时，伤害的 SurvivalJadeConversionRatio（默认 50%，可配置）转化为治疗，
 *   消耗等量残影，治疗不超过最大生命与残影余量。
 * - 取下饰品时清空残影。
 * - 仅服务端处理，残影量定期同步给玩家客户端用于 HUD。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class SurvivalJadeEvents {

    private static final int SYNC_INTERVAL = 5;               // 每 5 tick 同步一次

    private static final String KEY_PHANTOM = "SurvivalJadePhantom";
    private static final String KEY_PREV_HP = "SurvivalJadePrevHP";
    private static final String KEY_DECAY_ACCUM = "SurvivalJadeDecayAccum"; // 累计 tick 用于周期衰减

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        boolean wearing = SurvivalJadeItem.isWearing(entity);

        if (!wearing) {
            // 取下饰品：清空残影（避免卸下保留残影的取巧）
            if (data.contains(KEY_PHANTOM) || data.contains(KEY_PREV_HP)) {
                float prevPhantom = data.getFloat(KEY_PHANTOM);
                data.remove(KEY_PHANTOM);
                data.remove(KEY_PREV_HP);
                data.remove(KEY_DECAY_ACCUM);
                if (prevPhantom > 0f && entity instanceof ServerPlayer sp) {
                    SurvivalJadeSyncPacket.sendTo(sp, 0f);
                }
            }
            return;
        }

        float currentHP = entity.getHealth();
        float maxHP = entity.getMaxHealth();

        float prevHP = data.contains(KEY_PREV_HP) ? data.getFloat(KEY_PREV_HP) : currentHP;
        float phantom = data.contains(KEY_PHANTOM) ? data.getFloat(KEY_PHANTOM) : 0f;

        // 实际生命值下降 -> 残影（涵盖伤害、饥饿、中毒、凋零、虚空等所有 HP 损失来源）
        if (currentHP < prevHP) {
            phantom += (prevHP - currentHP);
        }
        // 残影上限 = maxHP - currentHP，确保 残影 + 当前血量 <= 最大生命值
        // 满血时残影为 0
        float phantomCap = Math.max(0f, maxHP - currentHP);
        if (phantom > phantomCap) phantom = phantomCap;

        // 残影缓慢衰减：每 SurvivalJadeDecayTicks tick 衰减 1 点
        if (phantom > 0f) {
            int decayTicks = Math.max(1, Config.SurvivalJadeDecayTicks);
            int accum = data.contains(KEY_DECAY_ACCUM) ? data.getInt(KEY_DECAY_ACCUM) : 0;
            accum++;
            while (accum >= decayTicks && phantom > 0f) {
                phantom = Math.max(0f, phantom - 1f);
                accum -= decayTicks;
            }
            data.putInt(KEY_DECAY_ACCUM, accum);
        } else {
            data.remove(KEY_DECAY_ACCUM);
        }

        data.putFloat(KEY_PHANTOM, phantom);
        data.putFloat(KEY_PREV_HP, currentHP);

        if (entity instanceof ServerPlayer sp && sp.tickCount % SYNC_INTERVAL == 0) {
            SurvivalJadeSyncPacket.sendTo(sp, phantom);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getEntity() == attacker) return; // 跳过自伤
        if (!SurvivalJadeItem.isWearing(attacker)) return;
        if (attacker.level().isClientSide) return;

        CompoundTag data = attacker.getPersistentData();
        if (!data.contains(KEY_PHANTOM)) return;
        float phantom = data.getFloat(KEY_PHANTOM);
        if (phantom <= 0f) return;

        float healable = attacker.getMaxHealth() - attacker.getHealth();
        if (healable <= 0f) return; // 满血时不消耗残影

        float heal = Math.min(event.getAmount() * (float) Config.SurvivalJadeConversionRatio, Math.min(phantom, healable));
        if (heal <= 0f) return;

        attacker.heal(heal);
        phantom -= heal;
        data.putFloat(KEY_PHANTOM, phantom);

        // 即时同步，让 HUD 立刻反映残影消耗
        if (attacker instanceof ServerPlayer sp) {
            SurvivalJadeSyncPacket.sendTo(sp, phantom);
        }
    }
}
