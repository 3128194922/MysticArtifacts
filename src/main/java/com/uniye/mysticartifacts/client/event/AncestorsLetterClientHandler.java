package com.uniye.mysticartifacts.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.AncestorsLetterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 先祖的信状态图标 HUD：
 * 佩戴时在经验条正上方显示 18x18 状态图标。Y 偏移量由 Config.AncestorLetterIconYOffset 控制，
 * 默认 36（经验条上方两个图标高度），可在配置文件中自由调整。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AncestorsLetterClientHandler {

    private static final ResourceLocation NORMAL_ICON =
            new ResourceLocation(MysticArtifacts.MODID, "textures/gui/ancestor_letter/normal.png");
    private static final ResourceLocation VIRTUE_ICON =
            new ResourceLocation(MysticArtifacts.MODID, "textures/gui/ancestor_letter/virtue.png");
    private static final ResourceLocation TORMENT_ICON =
            new ResourceLocation(MysticArtifacts.MODID, "textures/gui/ancestor_letter/torment.png");

    private static final int ICON_SIZE = 18;

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.options.hideGui) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!AncestorsLetterItem.isWearing(player)) return;

        ResourceLocation icon = switch (AncestorsLetterItem.clientState) {
            case AncestorsLetterItem.STATE_VIRTUE -> VIRTUE_ICON;
            case AncestorsLetterItem.STATE_TORMENT -> TORMENT_ICON;
            default -> NORMAL_ICON;
        };

        GuiGraphics gg = event.getGuiGraphics();
        int x = gg.guiWidth() / 2 - ICON_SIZE / 2;   // 屏幕水平中心
        int y = gg.guiHeight() - 29 - Config.AncestorLetterIconYOffset;     // 经验条正上方

        RenderSystem.enableBlend();
        gg.blit(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }
}
