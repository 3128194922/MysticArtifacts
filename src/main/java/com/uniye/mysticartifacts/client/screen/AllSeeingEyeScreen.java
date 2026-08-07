package com.uniye.mysticartifacts.client.screen;

import com.mojang.authlib.GameProfile;
import com.uniye.mysticartifacts.network.PlayerListPacket;
import com.uniye.mysticartifacts.network.SelectSpectatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.UUID;

public class AllSeeingEyeScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int HEADER_HEIGHT = 24;

    private final List<PlayerListPacket.Entry> players;
    private int scrollOffset = 0;
    private int panelLeft;
    private int panelTop;

    public AllSeeingEyeScreen(List<PlayerListPacket.Entry> players) {
        super(Component.translatable("item.mysticartifacts.all_seeing_eye"));
        this.players = players;
    }

    @Override
    protected void init() {
        int visibleRows = Math.min(players.size(), MAX_VISIBLE_ROWS);
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = Math.max(4, (this.height - HEADER_HEIGHT - visibleRows * ROW_HEIGHT) / 2);
        int panelBottom = panelTop + HEADER_HEIGHT + visibleRows * ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("item.mysticartifacts.all_seeing_eye.exit"),
                        btn -> {
                            SelectSpectatePacket.sendToServer(new SelectSpectatePacket(null));
                            this.onClose();
                        })
                .bounds(panelLeft, panelBottom + 6, PANEL_WIDTH, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int visibleRows = Math.min(players.size(), MAX_VISIBLE_ROWS);
        int panelBottom = panelTop + HEADER_HEIGHT + visibleRows * ROW_HEIGHT;

        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelBottom, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, panelLeft + PANEL_WIDTH / 2, panelTop + 7, 0xFFFFFF);
        graphics.fill(panelLeft, panelTop + HEADER_HEIGHT, panelLeft + PANEL_WIDTH, panelTop + HEADER_HEIGHT + 1, 0x40FFFFFF);

        for (int i = 0; i < visibleRows; i++) {
            int index = scrollOffset + i;
            if (index >= players.size()) break;
            PlayerListPacket.Entry entry = players.get(index);
            int rowTop = panelTop + HEADER_HEIGHT + i * ROW_HEIGHT;
            if (isHoveringRow(mouseX, mouseY, rowTop)) {
                graphics.fill(panelLeft, rowTop, panelLeft + PANEL_WIDTH, rowTop + ROW_HEIGHT, 0x50FFFFFF);
            }
            drawHead(graphics, entry.uuid(), entry.name(), panelLeft + 6, rowTop + 4);
            graphics.drawString(this.font, entry.name(), panelLeft + 28, rowTop + 8, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int visibleRows = Math.min(players.size(), MAX_VISIBLE_ROWS);
            for (int i = 0; i < visibleRows; i++) {
                int index = scrollOffset + i;
                if (index >= players.size()) break;
                int rowTop = panelTop + HEADER_HEIGHT + i * ROW_HEIGHT;
                if (isHoveringRow(mouseX, mouseY, rowTop)) {
                    SelectSpectatePacket.sendToServer(new SelectSpectatePacket(players.get(index).uuid()));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, players.size() - MAX_VISIBLE_ROWS);
        scrollOffset = Mth.clamp(scrollOffset - (int) delta, 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isHoveringRow(double mouseX, double mouseY, int rowTop) {
        return mouseX >= panelLeft && mouseX <= panelLeft + PANEL_WIDTH
                && mouseY >= rowTop && mouseY <= rowTop + ROW_HEIGHT;
    }

    private void drawHead(GuiGraphics graphics, UUID uuid, String name, int x, int y) {
        ResourceLocation skin = Minecraft.getInstance().getSkinManager()
                .getInsecureSkinLocation(new GameProfile(uuid, name));
        if (skin == null) {
            skin = new ResourceLocation("minecraft", "textures/entity/steve.png");
        }
        graphics.blit(skin, x, y, 16, 16, 8.0F, 8.0F, 8, 8, 64, 64);
    }
}
