package com.uniye.mysticartifacts.util;

/**
 * 直死之魔眼的斩杀值计算，不依赖 Minecraft API，便于单元测试。
 */
public final class DeathEyeProgress {

    private DeathEyeProgress() {
    }

    public static StrikeResult apply(double accumulatedDamage, float damage, boolean cutLineHit) {
        double safeAccumulated = Math.max(0.0, accumulatedDamage);
        float safeDamage = Math.max(0.0f, damage);
        if (cutLineHit) {
            return new StrikeResult(0.0, safeDamage + (float) safeAccumulated, true);
        }
        return new StrikeResult(safeAccumulated + safeDamage, safeDamage, false);
    }

    public static float renderRatio(double accumulatedDamage, float maxHealth) {
        if (maxHealth <= 0.0f) return 0.0f;
        double ratio = Math.max(0.0, accumulatedDamage) / maxHealth;
        return (float) Math.max(0.0, Math.min(1.0, ratio));
    }

    public record StrikeResult(double accumulatedDamage, float damage, boolean executed) {
    }
}
