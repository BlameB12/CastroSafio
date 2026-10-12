package com.castrosafio.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CastroMenuScreen extends Screen {
    private static final int BG = 0xF2050A0F;
    private static final int FRAME_LIGHT = 0xFFE8ECF3;
    private static final int FRAME_MID = 0xFF9BA4B4;
    private static final int FRAME_DARK = 0xFF4D5666;
    private static final int PANEL = 0xFF061019;
    private static final int PANEL_ALT = 0xFF0B1722;
    private static final int SLOT = 0xFF07121B;
    private static final int SLOT_BORDER = 0xFF163447;
    private static final int CYAN = 0xFF22E9FF;
    private static final int PINK = 0xFFFF4DD8;
    private static final int YELLOW = 0xFFFFFF52;
    private static final int GREEN = 0xFF66FF7D;
    private static final int PURPLE = 0xFF8E74FF;
    private static final int WHITE = 0xFFF4F7FF;
    private static final int MUTED = 0xFFA9B5C5;

    private Tab selected = Tab.MISSIONS;

    public CastroMenuScreen() {
        super(Component.literal("CastroSafio"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, BG);

        int margin = Math.max(8, Math.min(18, width / 100));
        int uiW = width - margin * 2;
        int uiH = height - margin * 2;
        int x = margin;
        int y = margin;
        int sideW = Math.max(150, Math.min(215, uiW / 7));

        drawOuterFrame(g, x, y, uiW, uiH);
        drawSidebar(g, x + 18, y + 22, sideW - 20, uiH - 44, mouseX, mouseY);
        drawMain(g, x + sideW + 8, y + 22, uiW - sideW - 26, uiH - 44, mouseX, mouseY);
    }

    private void drawOuterFrame(GuiGraphics g, int x, int y, int w, int h) {
        // stepped metallic frame
        g.fill(x + 10, y, x + w - 10, y + h, FRAME_DARK);
        g.fill(x, y + 10, x + w, y + h - 10, FRAME_DARK);
        g.fill(x + 12, y + 4, x + w - 12, y + h - 4, FRAME_LIGHT);
        g.fill(x + 4, y + 12, x + w - 4, y + h - 12, FRAME_LIGHT);
        g.fill(x + 16, y + 8, x + w - 16, y + h - 8, FRAME_MID);
        g.fill(x + 8, y + 16, x + w - 8, y + h - 16, FRAME_MID);
        g.fill(x + 19, y + 13, x + w - 19, y + h - 13, 0xFF111923);
        g.fill(x + 13, y + 19, x + w - 13, y + h - 19, 0xFF111923);
        g.fill(x + 22, y + 22, x + w - 22, y + h - 22, 0xFF02070B);

        drawRainbowBar(g, x + w / 2 - 64, y + 6, 128, 4);
        drawRainbowBar(g, x + w / 2 - 80, y + h - 10, 160, 4);
        drawRainbowBarVertical(g, x + 5, y + h / 2 - 80, 4, 160);
        drawRainbowBarVertical(g, x + w - 9, y + h / 2 - 80, 4, 160);

        // corner details
        for (int i = 0; i < 4; i++) {
            g.fill(x + 24 + i * 8, y + 9, x + 29 + i * 8, y + 13, 0xFF202B38);
            g.fill(x + w - 53 + i * 8, y + 9, x + w - 48 + i * 8, y + 13, 0xFF202B38);
        }
    }

    private void drawRainbowBar(GuiGraphics g, int x, int y, int w, int h) {
        int[] c = {PINK, 0xFFFF8C45, YELLOW, GREEN, CYAN, PURPLE};
        int seg = Math.max(1, w / c.length);
        for (int i = 0; i < c.length; i++) {
            int x2 = (i == c.length - 1) ? x + w : x + (i + 1) * seg;
            g.fill(x + i * seg, y, x2, y + h, c[i]);
        }
    }

    private void drawRainbowBarVertical(GuiGraphics g, int x, int y, int w, int h) {
        int[] c = {PINK, 0xFFFF8C45, YELLOW, GREEN, CYAN, PURPLE};
        int seg = Math.max(1, h / c.length);
        for (int i = 0; i < c.length; i++) {
            int y2 = (i == c.length - 1) ? y + h : y + (i + 1) * seg;
            g.fill(x, y + i * seg, x + w, y2, c[i]);
        }
    }

    private void drawSidebar(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, 0xFF071018);
        int gap = 10;
        int buttonH = (h - gap * 5) / 4;
        Tab[] tabs = Tab.values();

        for (int i = 0; i < tabs.length; i++) {
            int by = y + gap + i * (buttonH + gap);
            drawTab(g, tabs[i], x + 4, by, w - 8, buttonH, mouseX, mouseY);
        }
    }

    private void drawTab(GuiGraphics g, Tab tab, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean active = selected == tab;
        boolean hover = in(mouseX, mouseY, x, y, w, h);
        int outer = active ? CYAN : (hover ? 0xFF68A8C4 : FRAME_MID);

        g.fill(x, y, x + w, y + h, outer);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, FRAME_LIGHT);
        g.fill(x + 7, y + 7, x + w - 7, y + h - 7, FRAME_DARK);
        g.fill(x + 11, y + 11, x + w - 11, y + h - 11, active ? 0xFF13283D : PANEL_ALT);

        if (active) {
            g.fill(x + 2, y + 4, x + 6, y + h - 4, PINK);
            g.fill(x + w - 6, y + 4, x + w - 2, y + h - 4, CYAN);
            g.fill(x + 7, y + 7, x + w - 7, y + 10, PURPLE);
        }

        drawScaledItem(g, tab.icon, x + w / 2, y + Math.max(30, h / 2 - 18), 2.35f);

        int textY = y + h - 40;
        drawCenteredScaled(g, tab.line1, x + w / 2, textY, 1.15f, WHITE);
        if (!tab.line2.isEmpty()) {
            drawCenteredScaled(g, tab.line2, x + w / 2, textY + 15, 1.15f, active ? CYAN : MUTED);
        }
    }

    private void drawMain(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 6, CYAN);
        g.fill(x + 3, y + h - 6, x + w - 3, y + h - 3, 0xFF18566E);

        drawHeader(g, x + 12, y + 10, w - 24, 66);
        drawGrid(g, x + 14, y + 86, w - 28, h - 102, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 24, y, x + w - 24, y + h, 0xFF0A1724);
        g.fill(x, y + 12, x + 24, y + h - 12, 0xFF0A1724);
        g.fill(x + w - 24, y + 12, x + w, y + h - 12, 0xFF0A1724);
        g.fill(x + 4, y + 14, x + 9, y + h - 14, PINK);
        g.fill(x + w - 9, y + 14, x + w - 4, y + h - 14, CYAN);
        g.fill(x + 26, y + 4, x + w - 26, y + 7, 0xFF173D52);

        int lineY = y + h / 2;
        g.fill(x + 18, lineY - 1, x + 105, lineY + 1, PINK);
        g.fill(x + w - 105, lineY - 1, x + w - 18, lineY + 1, CYAN);
        drawCenteredScaled(g, selected.title, x + w / 2, y + 24, 1.9f, WHITE);
    }

    private void drawGrid(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cols = w >= 1100 ? 13 : (w >= 900 ? 11 : (w >= 700 ? 9 : 7));
        int rows = 5;
        int gap = 6;
        int cellW = (w - gap * (cols - 1)) / cols;
        int cellH = (h - gap * (rows - 1)) / rows;

        for (int i = 0; i < cols * rows; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cellW + gap);
            int cy = y + row * (cellH + gap);
            boolean hover = in(mouseX, mouseY, cx, cy, cellW, cellH);

            g.fill(cx, cy, cx + cellW, cy + cellH, hover ? 0xFF275268 : SLOT_BORDER);
            g.fill(cx + 3, cy + 3, cx + cellW - 3, cy + cellH - 3, hover ? 0xFF0E2230 : SLOT);

            int qColor = switch (i % 6) {
                case 0 -> CYAN;
                case 1 -> PINK;
                case 2 -> YELLOW;
                case 3 -> GREEN;
                case 4 -> PURPLE;
                default -> WHITE;
            };
            drawQuestion(g, cx + cellW / 2, cy + cellH / 2 - 9, qColor);
        }

        if (in(mouseX, mouseY, x, y, w, h)) {
            String hint = switch (selected) {
                case DIFFICULTY -> "CAMBIO DE DIFICULTAD BLOQUEADO";
                case CRAFTS -> "CRAFTEO AUN NO DESBLOQUEADO";
                case MISSIONS -> "MISION AUN NO REVELADA";
                case MOBS -> "MOB AUN NO HABILITADO";
            };
            drawCenteredScaled(g, hint, x + w / 2, y + h - 17, 0.9f, MUTED);
        }
    }

    private void drawQuestion(GuiGraphics g, int centerX, int centerY, int color) {
        // pixel-style large question mark, closer to the reference than the tiny font glyph
        int s = 5;
        int x = centerX - 3 * s;
        int y = centerY - 4 * s;
        // top
        g.fill(x + s, y, x + 5 * s, y + s, WHITE);
        g.fill(x + 5 * s, y + s, x + 6 * s, y + 3 * s, PINK);
        g.fill(x, y + s, x + s, y + 3 * s, CYAN);
        g.fill(x + 4 * s, y + 3 * s, x + 5 * s, y + 4 * s, color);
        g.fill(x + 3 * s, y + 4 * s, x + 4 * s, y + 5 * s, color);
        g.fill(x + 3 * s, y + 5 * s, x + 4 * s, y + 6 * s, color);
        // dot
        g.fill(x + 3 * s, y + 7 * s, x + 4 * s, y + 8 * s, PINK);
    }

    private void drawScaledItem(GuiGraphics g, net.minecraft.world.item.Item item, int centerX, int centerY, float scale) {
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(new ItemStack(item), -8, -8);
        g.pose().popPose();
    }

    private void drawCenteredScaled(GuiGraphics g, String text, int centerX, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(centerX, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawCenteredString(font, text, 0, 0, color);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int margin = Math.max(8, Math.min(18, width / 100));
            int uiW = width - margin * 2;
            int uiH = height - margin * 2;
            int x = margin;
            int y = margin;
            int sideW = Math.max(150, Math.min(215, uiW / 7));
            int sx = x + 18;
            int sy = y + 22;
            int sw = sideW - 20;
            int sh = uiH - 44;
            int gap = 10;
            int buttonH = (sh - gap * 5) / 4;

            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                int bx = sx + 4;
                int by = sy + gap + i * (buttonH + gap);
                int bw = sw - 8;
                if (in(mouseX, mouseY, bx, by, bw, buttonH)) {
                    selected = tabs[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private enum Tab {
        DIFFICULTY("CAMBIOS DE", "DIFICULTAD", "CAMBIOS DE DIFICULTAD", Items.BOOK),
        CRAFTS("NUEVOS", "CRAFTEOS", "NUEVOS CRAFTEOS", Items.DIAMOND_SWORD),
        MISSIONS("MISIONES", "DIARIAS", "MISIONES DIARIAS", Items.PAPER),
        MOBS("MOBS", "HABILITADOS", "MOBS HABILITADOS", Items.SPYGLASS);

        final String line1;
        final String line2;
        final String title;
        final net.minecraft.world.item.Item icon;

        Tab(String line1, String line2, String title, net.minecraft.world.item.Item icon) {
            this.line1 = line1;
            this.line2 = line2;
            this.title = title;
            this.icon = icon;
        }
    }
}
