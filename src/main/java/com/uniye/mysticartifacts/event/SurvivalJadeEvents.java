package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.SurvivalJadeItem;
import com.uniye.mysticartifacts.network.SurvivalJadeSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 求生玉（Survival Instinct）服务端逻辑：
 * - 佩戴者任何形式的实际生命值下降都会等量转化为"残影"暂存（类似 absorption 的独立临时生命值）。
 * - 残影上限：
 *   - CAP 模式（默认）：上限 = SurvivalJadeMaxPhantom（默认 20），与当前血量无关，
 *     满血时也可保有残影；
 *   - AUTO 模式：无上限，残影自由积累，HUD 显示时按每行 10 心向上换行。
 * - 残影缓慢衰减：每秒衰减 SurvivalJadeDecayPerSecond 点（默认 2 点/秒，可配置）。
 * - 佩戴者造成伤害时，伤害的 SurvivalJadeConversionRatio（默认 25%，可配置）转化为治疗，
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

        float prevHP = data.contains(KEY_PREV_HP) ? data.getFloat(KEY_PREV_HP) : currentHP;
        float phantom = data.contains(KEY_PHANTOM) ? data.getFloat(KEY_PHANTOM) : 0f;

        // 实际生命值下降 -> 残影（涵盖伤害、饥饿、中毒、凋零、虚空等所有 HP 损失来源）
        if (currentHP < prevHP) {
            phantom += (prevHP - currentHP);
        }
        // 残影上限（类似 absorption 的独立临时生命，与当前血量无关）：
        // - CAP 模式：上限 = config 值（默认 20）
        // - AUTO 模式：无上限，HUD 显示时按每行 10 心向上换行
        if (Config.SurvivalJadePhantomCapMode == Config.PhantomCapMode.CAP) {
            float phantomCap = Config.SurvivalJadeMaxPhantom;
            if (phantom > phantomCap) phantom = phantomCap;
        }

        // 残影衰减：每秒（20 tick）衰减 SurvivalJadeDecayPerSecond 点
        if (phantom > 0f) {
            float decayPerSecond = (float) Config.SurvivalJadeDecayPerSecond;
            if (decayPerSecond > 0f) {
                int accum = data.contains(KEY_DECAY_ACCUM) ? data.getInt(KEY_DECAY_ACCUM) : 0;
                accum++;
                while (accum >= 20 && phantom > 0f) {
                    phantom = Math.max(0f, phantom - decayPerSecond);
                    accum -= 20;
                }
                data.putInt(KEY_DECAY_ACCUM, accum);
            }
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

        // heal() 会触发 onLivingHeal，由那处等量扣减残影并同步
        // 这里不再手动扣减，避免对同一口治疗重复消耗
        attacker.heal(heal);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!SurvivalJadeItem.isWearing(event.getEntity())) return;
        float heal = event.getAmount();
        if (heal <= 0f) return;

        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(KEY_PHANTOM)) return;
        float phantom = data.getFloat(KEY_PHANTOM);
        if (phantom <= 0f) return;

        // 任何来源的 heal() 都会等量消除残影
        float remaining = Math.max(0f, phantom - heal);
        data.putFloat(KEY_PHANTOM, remaining);

        if (entity instanceof ServerPlayer sp) {
            SurvivalJadeSyncPacket.sendTo(sp, remaining);
        }
    }
}
