package com.uniye.mysticartifacts.lightning;

public final class LightningBottleLogic {
    private LightningBottleLogic() {
    }

    public static int effectiveMaxTargets(int configuredMaximum, boolean raining) {
        int maximum = Math.max(0, configuredMaximum);
        return raining ? maximum * 2 : maximum;
    }

    /**
     * Cataclysm 潮湿增伤公式：每级 +20%，无潮湿时返回 1.0。
     * amplifier < 0 表示目标没有潮湿效果。
     */
    public static float wetnessMultiplier(int amplifier) {
        if (amplifier < 0) return 1.0F;
        return 1.0F + (amplifier + 1) * 0.2F;
    }

    /**
     * 主命中附加段是否在瓶内乘潮湿：
     * lightning 源会在灾变 LivingDamageEvent 中统一放大，这里保持原值避免双乘；
     * 非 lightning 源灾变不处理，需要在附加段内乘。
     */
    public static float primaryBonus(float baseBonus, boolean lightningSource, int wetnessAmplifier) {
        return lightningSource ? baseBonus : baseBonus * wetnessMultiplier(wetnessAmplifier);
    }
}
