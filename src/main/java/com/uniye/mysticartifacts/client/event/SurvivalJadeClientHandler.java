package com.uniye.mysticartifacts.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.SurvivalJadeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 求生玉残影 HUD：常态显示（只要佩戴且有残影就渲染）。
 *
 * 显示方向与原版血条一致：
 * - 残影区域 = [当前生命, min(当前生命+残影, 最大生命)]，用玉色半透明实心心覆盖在原版空心心上，
 *   表示"待恢复的血量"。恢复时从当前生命右侧向右填充（右侧增加）。
 * - 残影衰减/转化时，区域右端向左收缩（右侧减少）。
 * - 满血时若仍有残影（残影溢出最大生命），超出部分以玉色心延伸显示在血条右端外侧。
 *
 * 支持半心粒度。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurvivalJadeClientHandler {

    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");

    // 玉色（青绿）半透明
    private static final float TINT_R = 0.30f;
    private static final float TINT_G = 0.87f;
    private static final float TINT_B = 0.62f;
    private static final float TINT_A = 0.78f;

    // 原版 icons.png 心形 sprite
    private static final int HEART_FULL_U = 52; // 满心（红）9x9，用于玉色染色覆盖
    private static final int HEART_V = 0;
    private static final int HEART_SIZE = 9;
    private static final int HALF_HEART_WIDTH = 5; // 半心=满心左半

    // 由网络包写入的残影量
    private static float phantom = 0f;

    public static void setPhantom(float value) {
        phantom = value;
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (phantom <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!SurvivalJadeItem.isWearing(player)) return;

        float currentHP = player.getHealth();
        float maxHP = player.getMaxHealth();
        // 有效血量末端 = 当前生命 + 残影（不限制最大生命，溢出部分延伸显示）
        float effectiveEnd = currentHP + phantom;
        if (effectiveEnd <= currentHP) return;

        GuiGraphics gg = event.getGuiGraphics();
        int screenWidth = gg.guiWidth();
        int screenHeight = gg.guiHeight();

        // 血条左端与基准 Y（与原版 Gui 绘制一致）
        int left = screenWidth / 2 - 91;
        int baseY = screenHeight - 39;

        // 需要绘制的心数：从 0 到 ceil(effectiveEnd/2)，覆盖残影所在区间
        int totalHeartsToDraw = (int) Math.ceil(effectiveEnd / 2.0f);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(TINT_R, TINT_G, TINT_B, TINT_A);

        for (int k = 0; k < totalHeartsToDraw; k++) {
            float heartStart = 2f * k;
            float heartEnd = 2f * (k + 1);

            // 残影在该心的填充量：[max(heartStart, currentHP), min(heartEnd, effectiveEnd)]
            float fillStart = Math.max(heartStart, currentHP);
            float fillEnd = Math.min(heartEnd, effectiveEnd);
            float phantomFill = fillEnd - fillStart;
            if (phantomFill <= 0f) continue;

            int row = k / 10;
            int col = k % 10;
            int x = left + col * 8;
            int y = baseY - row * 10;

            if (phantomFill >= 1.5f) {
                // 满心玉色
                gg.blit(ICONS, x, y, HEART_FULL_U, HEART_V, HEART_SIZE, HEART_SIZE);
            } else {
                // 半心玉色（满心左半）
                gg.blit(ICONS, x, y, HEART_FULL_U, HEART_V, HALF_HEART_WIDTH, HEART_SIZE);
            }
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
