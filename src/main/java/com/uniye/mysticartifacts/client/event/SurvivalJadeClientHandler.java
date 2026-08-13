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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 求生玉残影 HUD：常态显示（只要佩戴且有残影就渲染）。
 *
 * 残影规则：残影 + 当前血量 <= 最大生命值（满血时残影为 0）。
 *
 * 显示方向与原版血条一致：
 * - 残影区域 = [当前生命, 当前生命+残影]，用玉色半透明实心心覆盖在原版空心心上，
 *   表示"待恢复的血量"。恢复时从当前生命右侧向右填充（右侧增加）。
 * - 残影衰减/转化时，区域右端向左收缩（右侧减少）。
 *
 * 支持半心粒度。
 *
 * 兼容 overflowing-bars：
 * - OB 开启 allowLayers 时，底部行始终显示当前 20HP 层（10 心），HP>20 用橙色溢出指示。
 * - 残影按层渲染：当前层内部分渲染在底部行；超出当前层的部分在上一行渲染或延伸到血条右端外侧。
 * - OB 底部行位置与原版一致（screenHeight - 39），无需额外偏移。
 * - 当 OB 渲染吸收心第二行时，跳过上一行渲染以避免与吸收心冲突。
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

    // OB 检测缓存：null=未检测，true/false=检测结果
    private static Boolean obActiveCache = null;

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
        // 残影上限 = maxHP - currentHP，确保 残影 + 当前血量 <= 最大生命值
        // 服务端已保证此约束，客户端额外裁剪以处理网络同步延迟
        float effectivePhantom = Math.min(phantom, Math.max(0f, maxHP - currentHP));
        if (effectivePhantom <= 0f) return;

        // 有效血量末端 = 当前生命 + 残影（不超过最大生命）
        float effectiveEnd = currentHP + effectivePhantom;

        GuiGraphics gg = event.getGuiGraphics();
        int screenWidth = gg.guiWidth();
        int screenHeight = gg.guiHeight();

        // 血条左端与基准 Y（与原版 Gui 绘制一致，OB 也使用此位置作为底部行）
        int left = screenWidth / 2 - 91;
        int baseY = screenHeight - 39;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(TINT_R, TINT_G, TINT_B, TINT_A);

        if (isOverflowingBarsActive()) {
            renderPhantomWithLayers(gg, left, baseY, currentHP, maxHP, effectivePhantom, player);
        } else {
            renderPhantomVanilla(gg, left, baseY, currentHP, effectiveEnd);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /**
     * 检测 overflowing-bars 是否已加载且开启了多层血条（health.allowLayers）。
     * 使用反射读取 OB 的配置，失败时回退到 assume true（默认值）。
     */
    private static boolean isOverflowingBarsActive() {
        if (obActiveCache != null) return obActiveCache;
        if (!ModList.get().isLoaded("overflowingbars")) {
            obActiveCache = false;
            return false;
        }
        try {
            Class<?> obClass = Class.forName("fuzs.overflowingbars.OverflowingBars");
            Field configField = obClass.getField("CONFIG");
            Object configHolder = configField.get(null);
            Method getMethod = configHolder.getClass().getMethod("get", Class.class);
            Class<?> clientConfigClass = Class.forName("fuzs.overflowingbars.config.ClientConfig");
            Object clientConfig = getMethod.invoke(configHolder, clientConfigClass);
            Field healthField = clientConfigClass.getField("health");
            Object healthConfig = healthField.get(clientConfig);
            Field allowLayersField = healthConfig.getClass().getField("allowLayers");
            obActiveCache = allowLayersField.getBoolean(healthConfig);
        } catch (Exception e) {
            // 反射失败：OB 已加载但配置无法读取，假设 allowLayers=true（默认值）
            obActiveCache = true;
        }
        return obActiveCache;
    }

    /**
     * 原版血条渲染逻辑：残影按绝对 HP 位置渲染，每行 10 心。
     * 适用于无 OB 或 OB allowLayers=false 的情况。
     */
    private static void renderPhantomVanilla(GuiGraphics gg, int left, int baseY, float currentHP, float effectiveEnd) {
        int totalHeartsToDraw = (int) Math.ceil(effectiveEnd / 2.0f);
        for (int k = 0; k < totalHeartsToDraw; k++) {
            float heartStart = 2f * k;
            float heartEnd = 2f * (k + 1);
            float fillStart = Math.max(heartStart, currentHP);
            float fillEnd = Math.min(heartEnd, effectiveEnd);
            float phantomFill = fillEnd - fillStart;
            if (phantomFill <= 0f) continue;

            int row = k / 10;
            int col = k % 10;
            int x = left + col * 8;
            int y = baseY - row * 10;

            blitPhantomHeart(gg, x, y, phantomFill);
        }
    }

    /**
     * 兼容 overflowing-bars 的多层血条渲染逻辑。
     *
     * OB 显示规则：底部行始终显示当前 20HP 层（10 心），HP>20 时用橙色指示溢出层。
     * 求生玉残影按层渲染（残影 + 当前血量 <= 最大生命值）：
     * - 从当前 HP 位置开始，在当前层底部行渲染；
     * - 若超出当前层，且上一行未被 OB 占用，则剩余部分在上一行渲染；
     * - 无法在行内显示的剩余部分（大 maxHP 场景）：以玉色心延伸显示在血条右端外侧。
     */
    private static void renderPhantomWithLayers(GuiGraphics gg, int left, int baseY,
                                                float currentHP, float maxHP, float phantom,
                                                LocalPlayer player) {
        // 计算当前层起点：currentHP 1-20 -> 层0, 21-40 -> 层1, 41-60 -> 层2...
        // 这样 currentHP=20 时属于层0（底部行全满），currentHP=21 时属于层1（底部行显示溢出）
        int layerStart = currentHP <= 20f ? 0 : ((int) Math.floor((currentHP - 1f) / 20f)) * 20;
        float layerPos = currentHP - layerStart; // 当前层内的 HP 位置（0-20）

        // 判断上一行是否被 OB 占用（吸收心第二行 或 护甲行）
        boolean obTwoRows = player.getAbsorptionAmount() > 0.0F
                && player.getMaxHealth() + player.getAbsorptionAmount() > 20.0F;
        boolean rowAboveOccupied = obTwoRows || player.getArmorValue() > 0;
        int maxRows = rowAboveOccupied ? 1 : 2;

        float remaining = phantom;
        int rowOffset = 0;

        while (remaining > 0f && rowOffset < maxRows) {
            float spaceInLayer = 20f - layerPos;
            float phantomInRow = Math.min(remaining, spaceInLayer);
            if (phantomInRow > 0f) {
                renderPhantomInRow(gg, left, baseY - rowOffset * 10, layerPos, layerPos + phantomInRow);
            }
            remaining -= phantomInRow;
            layerPos = 0f;
            rowOffset++;
        }

        // 无法在行内渲染的剩余残影（大 maxHP 场景），延伸到血条右端外侧
        if (remaining > 0f) {
            renderPhantomExcess(gg, left, baseY, remaining);
        }
    }

    /**
     * 在单行内渲染残影心（col 0-9）。
     */
    private static void renderPhantomInRow(GuiGraphics gg, int left, int y, float startHP, float endHP) {
        int startHeart = (int) Math.floor(startHP / 2f);
        int endHeart = (int) Math.ceil(endHP / 2f);
        for (int k = startHeart; k < endHeart && k < 10; k++) {
            float heartStart = 2f * k;
            float heartEnd = 2f * (k + 1);
            float fillStart = Math.max(heartStart, startHP);
            float fillEnd = Math.min(heartEnd, endHP);
            float phantomFill = fillEnd - fillStart;
            if (phantomFill <= 0f) continue;

            int x = left + k * 8;
            blitPhantomHeart(gg, x, y, phantomFill);
        }
    }

    /**
     * 渲染溢出残影（超过 maxHP 或无法在行内显示的部分），以玉色心延伸显示在血条右端外侧（col 10+）。
     */
    private static void renderPhantomExcess(GuiGraphics gg, int left, int baseY, float excessPhantom) {
        int excessHearts = (int) Math.ceil(excessPhantom / 2f);
        for (int k = 0; k < excessHearts; k++) {
            float heartStart = 2f * k;
            float heartEnd = 2f * (k + 1);
            float fillEnd = Math.min(heartEnd, excessPhantom);
            float phantomFill = fillEnd - heartStart;
            if (phantomFill <= 0f) continue;

            int x = left + (10 + k) * 8; // col 10, 11, ...（血条右端外侧）
            blitPhantomHeart(gg, x, baseY, phantomFill);
        }
    }

    private static void blitPhantomHeart(GuiGraphics gg, int x, int y, float phantomFill) {
        if (phantomFill >= 1.5f) {
            // 满心玉色
            gg.blit(ICONS, x, y, HEART_FULL_U, HEART_V, HEART_SIZE, HEART_SIZE);
        } else {
            // 半心玉色（满心左半）
            gg.blit(ICONS, x, y, HEART_FULL_U, HEART_V, HALF_HEART_WIDTH, HEART_SIZE);
        }
    }
}
