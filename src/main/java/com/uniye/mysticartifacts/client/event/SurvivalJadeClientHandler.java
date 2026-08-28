package com.uniye.mysticartifacts.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.uniye.mysticartifacts.item.impl.SurvivalJadeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 求生玉残影 HUD：渲染在 Thirst-Mod 口渴条相同位置（屏幕右侧，食物栏上方一行）。
 *
 * 位置与错位机制：
 * - 锚点 left = width/2 + 91，图标从左向右排列（与原版血量方向一致，区域与食物栏/口渴条相同）；
 * - 通过 Forge GUI overlay 的 rightHeight 堆叠协议实现自动错位：任何在同一位置渲染
 *   并递增 rightHeight 的 mod（如 Thirst-Mod 口渴条）都会自动把本条上移一行，反之亦然，
 *   与注册顺序无关，因此不会重叠；
 * - 未佩戴 / 残影为 0 时不渲染也不递增 rightHeight，不产生空行；
 * - 骑乘实体时跳过（该区域显示坐骑血条）。
 *
 * 残影显示（类似 absorption 的独立临时生命值，以灰色心显示）：
 * - 残影 <= 20：单行显示（<= 10 心），支持半心；
 * - 残影 > 20：以原版血量超过 20 的方式向上换行堆叠（每行 10 心）；
 * - 安装 overflowing-bars 且 health.allowLayers 开启时：
 *   底行只显示当前层余数（(残影-1)%20+1，与 OB 血量层逻辑一致），整行以 OB 橙色心显示，
 *   并在条右侧以 OB forceFontRenderer 风格的四向描边数字显示总残影计数。
 */
public class SurvivalJadeClientHandler {

    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final ResourceLocation OB_ICONS = new ResourceLocation("overflowingbars", "textures/gui/icons.png");
    private static final ResourceLocation OB_TINY_NUMBERS = new ResourceLocation("overflowingbars", "textures/font/tiny_numbers.png");

    // 原版 icons.png 心形 sprite（非 hardcore 行 v=0）
    private static final int CONTAINER_U = 16; // 空心轮廓
    private static final int HEART_FULL_U = 52; // 满心（用于灰色染色）
    private static final int HEART_HALF_U = 61; // 半心（左半）
    private static final int HEART_V = 0;
    private static final int HEART_SIZE = 9;
    private static final int HALF_HEART_WIDTH = 5;

    // OB 橙色心 sprite（overflowingbars:textures/gui/icons.png，HeartType.ORANGE）
    private static final int OB_FULL_U = 0;
    private static final int OB_HALF_U = 9;
    private static final int OB_V = 27;
    private static final int OB_V_HARDCORE = 36;

    // 灰色染色
    private static final float TINT_GRAY = 0.55f;
    private static final float TINT_ALPHA = 0.85f;

    // 由网络包写入的残影量
    private static float phantom = 0f;

    // OB 检测缓存：null=未检测，true/false=检测结果
    private static Boolean obActiveCache = null;

    public static void setPhantom(float value) {
        phantom = value;
    }

    public static final IGuiOverlay PHANTOM_OVERLAY = (gui, guiGraphics, partialTicks, screenWidth, screenHeight) -> {
        if (phantom <= 0f) return;
        Minecraft mc = gui.getMinecraft();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.options.hideGui || !gui.shouldDrawSurvivalElements()) return;
        if (player.getVehicle() instanceof LivingEntity) return; // 骑乘时该区域显示坐骑血条
        if (!SurvivalJadeItem.isWearing(player)) return;

        gui.setupOverlayRenderState(true, false);
        RenderSystem.enableBlend();
        render(gui, guiGraphics, screenWidth, screenHeight, player);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    };

    private static void render(ForgeGui gui, GuiGraphics gg, int screenWidth, int screenHeight, LocalPlayer player) {
        // 与 Thirst-Mod 口渴条相同的右侧锚点
        int left = screenWidth / 2 + 91;
        int top = screenHeight - gui.rightHeight;

        boolean ob = isOverflowingBarsActive();
        boolean hardcore = player.level().getLevelData().isHardcore();

        int totalHearts = Mth.ceil(phantom / 2.0f);
        int rows;
        if (ob && phantom > 20f) {
            rows = 1; // OB 层模式：只显示底行（当前层余数）+ 总数计数
        } else {
            rows = Math.max(1, Mth.ceil(totalHearts / 10.0f));
        }

        if (ob && phantom > 20f) {
            renderObRow(gg, left, top, hardcore);
        } else {
            renderWrappedRows(gg, left, top, totalHearts, rows);
        }

        // 递增行高，让氧气泡等后续 overlay 自动上移
        gui.rightHeight += rows * 10;
    }

    /**
     * 原版式换行渲染：底行 10 心，超出向上换行（每行 10 心），从左向右排列。
     */
    private static void renderWrappedRows(GuiGraphics gg, int left, int top, int totalHearts, int rows) {
        for (int r = 0; r < rows; r++) {
            int y = top - r * 10;
            int heartsInRow = Math.min(10, totalHearts - r * 10);
            if (heartsInRow <= 0) break;

            // 底行固定画 10 个容器心；上行只为已占用槽位画容器，避免大面积空轮廓
            int containers = (r == 0) ? 10 : heartsInRow;
            for (int c = 0; c < containers; c++) {
                int x = left - 81 + c * 8;
                gg.blit(ICONS, x, y, CONTAINER_U, HEART_V, HEART_SIZE, HEART_SIZE);
            }
            for (int c = 0; c < heartsInRow; c++) {
                float fill = Math.min(2f, phantom - 2f * (r * 10 + c));
                if (fill <= 0f) break;
                int x = left - 81 + c * 8;
                blitGrayHeart(gg, x, y, fill);
            }
        }
    }

    /**
     * OB 层模式渲染：底行只显示当前层余数（(残影-1)%20+1），整行 OB 橙色心，
     * 条右侧显示总残影描边数字计数。
     */
    private static void renderObRow(GuiGraphics gg, int left, int top, boolean hardcore) {
        int hp = Mth.ceil(phantom);
        int remainder = (hp - 1) % 20 + 1;
        int v = hardcore ? OB_V_HARDCORE : OB_V;

        for (int c = 0; c < 10; c++) {
            int x = left - 81 + c * 8;
            gg.blit(ICONS, x, top, CONTAINER_U, HEART_V, HEART_SIZE, HEART_SIZE);
        }
        int orangeHearts = Mth.ceil(remainder / 2.0f);
        for (int c = 0; c < orangeHearts; c++) {
            float fill = Math.min(2f, remainder - 2f * c);
            int x = left - 81 + c * 8;
            if (fill > 1.0f) {
                gg.blit(OB_ICONS, x, top, OB_FULL_U, v, HEART_SIZE, HEART_SIZE);
            } else {
                gg.blit(OB_ICONS, x, top, OB_HALF_U, v, HALF_HEART_WIDTH, HEART_SIZE);
            }
        }

        // 总残影计数：使用 OB 的 tiny_numbers 微型数字字体（3x5，四向描边），
        // 与 OB 行计数渲染方式一致，绘制在条右侧
        drawObTinyNumber(gg, left + 4, top + 2, hp);
    }

    /**
     * 以 OB 的 tiny_numbers.png 字体渲染数字（仿 RowCountRenderer.drawBorderedSprite）：
     * 3x5 像素数字，四方向偏移 1px 的黑色描边 + 白色本体，置于条右侧。
     */
    private static void drawObTinyNumber(GuiGraphics gg, int posX, int posY, int value) {
        if (value <= 0) return;
        // 逆序拆位：digits[0] 为个位
        int[] digits = java.util.stream.IntStream
                .iterate(value, i -> i > 0, i -> i / 10)
                .map(i -> i % 10)
                .toArray();
        posX += 4 * digits.length;
        for (int i = 0; i < digits.length; i++) {
            int x = posX - 4 * i;
            int u = 5 * digits[i];
            // 四向黑色描边
            RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
            gg.blit(OB_TINY_NUMBERS, x - 1, posY, u, 0, 3, 5, 256, 256);
            gg.blit(OB_TINY_NUMBERS, x + 1, posY, u, 0, 3, 5, 256, 256);
            gg.blit(OB_TINY_NUMBERS, x, posY - 1, u, 0, 3, 5, 256, 256);
            gg.blit(OB_TINY_NUMBERS, x, posY + 1, u, 0, 3, 5, 256, 256);
            // 白色数字本体
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            gg.blit(OB_TINY_NUMBERS, x, posY, u, 0, 3, 5, 256, 256);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void blitGrayHeart(GuiGraphics gg, int x, int y, float fill) {
        RenderSystem.setShaderColor(TINT_GRAY, TINT_GRAY, TINT_GRAY, TINT_ALPHA);
        if (fill > 1.0f) {
            gg.blit(ICONS, x, y, HEART_FULL_U, HEART_V, HEART_SIZE, HEART_SIZE);
        } else {
            gg.blit(ICONS, x, y, HEART_HALF_U, HEART_V, HALF_HEART_WIDTH, HEART_SIZE);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
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
}
