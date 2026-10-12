package com.castrosafio.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CastroMenuScreen extends Screen {
    private static final int BLACK = 0xFF02070C;
    private static final int PANEL = 0xFF06111A;
    private static final int PANEL_2 = 0xFF0B1823;
    private static final int GRID = 0xFF102A3A;
    private static final int GRID_HOVER = 0xFF173F52;
    private static final int SILVER_1 = 0xFFF1F4F8;
    private static final int SILVER_2 = 0xFFBCC4CF;
    private static final int SILVER_3 = 0xFF6D7786;
    private static final int SILVER_4 = 0xFF313A47;
    private static final int WHITE = 0xFFF8FBFF;
    private static final int MUTED = 0xFFB8C2CE;
    private static final int CYAN = 0xFF23E7FF;
    private static final int PINK = 0xFFFF43D5;
    private static final int PURPLE = 0xFF9C68FF;
    private static final int YELLOW = 0xFFFFFF48;
    private static final int GREEN = 0xFF64FF7A;
    private static final int ORANGE = 0xFFFF8C42;

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
        g.fill(0, 0, width, height, 0xFF000307);

        int margin = Math.max(8, Math.min(16, width / 110));
        int x = margin;
        int y = margin;
        int w = width - margin * 2;
        int h = height - margin * 2;

        drawOuterFrame(g, x, y, w, h);

        int innerX = x + 28;
        int innerY = y + 28;
        int innerW = w - 56;
        int innerH = h - 56;
        int sideW = Math.max(180, Math.min(245, innerW / 7));

        drawSidebar(g, innerX, innerY, sideW, innerH, mouseX, mouseY);
        drawContent(g, innerX + sideW + 14, innerY, innerW - sideW - 14, innerH, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawOuterFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 12, y, x + w - 12, y + h, SILVER_4);
        g.fill(x, y + 12, x + w, y + h - 12, SILVER_4);
        g.fill(x + 14, y + 4, x + w - 14, y + h - 4, SILVER_1);
        g.fill(x + 4, y + 14, x + w - 4, y + h - 14, SILVER_1);
        g.fill(x + 18, y + 9, x + w - 18, y + h - 9, SILVER_2);
        g.fill(x + 9, y + 18, x + w - 9, y + h - 18, SILVER_2);
        g.fill(x + 22, y + 14, x + w - 22, y + h - 14, SILVER_3);
        g.fill(x + 14, y + 22, x + w - 14, y + h - 22, SILVER_3);
        g.fill(x + 27, y + 21, x + w - 27, y + h - 21, 0xFF151D28);
        g.fill(x + 21, y + 27, x + w - 21, y + h - 27, 0xFF151D28);
        g.fill(x + 29, y + 29, x + w - 29, y + h - 29, BLACK);

        drawRgbStrip(g, x + w / 2 - 70, y + 7, 140, 6, false);
        drawRgbStrip(g, x + w / 2 - 92, y + h - 13, 184, 6, false);
        drawRgbStrip(g, x + 8, y + h / 2 - 110, 6, 220, true);
        drawRgbStrip(g, x + w - 14, y + h / 2 - 110, 6, 220, true);

        int cx = x + w / 2;
        g.fill(cx - 115, y + 2, cx + 115, y + 8, SILVER_2);
        g.fill(cx - 95, y + 8, cx + 95, y + 15, SILVER_3);
        g.fill(cx - 122, y + h - 15, cx + 122, y + h - 7, SILVER_3);
        g.fill(cx - 96, y + h - 8, cx + 96, y + h - 2, SILVER_2);
        g.fill(cx - 10, y + h - 13, cx + 10, y + h - 7, CYAN);

        for (int i = 0; i < 6; i++) {
            int dx = x + 35 + i * 9;
            g.fill(dx, y + 12, dx + 5, y + 18, 0xFF252E3A);
            int rx = x + w - 86 + i * 9;
            g.fill(rx, y + 12, rx + 5, y + 18, 0xFF252E3A);
            g.fill(dx, y + h - 18, dx + 5, y + h - 12, 0xFF252E3A);
            g.fill(rx, y + h - 18, rx + 5, y + h - 12, 0xFF252E3A);
        }
    }

    private void drawRgbStrip(GuiGraphics g, int x, int y, int w, int h, boolean vertical) {
        int[] colors = {PINK, ORANGE, YELLOW, GREEN, CYAN, PURPLE};
        if (vertical) {
            int seg = Math.max(1, h / colors.length);
            for (int i = 0; i < colors.length; i++) {
                int y2 = i == colors.length - 1 ? y + h : y + (i + 1) * seg;
                g.fill(x, y + i * seg, x + w, y2, colors[i]);
            }
        } else {
            int seg = Math.max(1, w / colors.length);
            for (int i = 0; i < colors.length; i++) {
                int x2 = i == colors.length - 1 ? x + w : x + (i + 1) * seg;
                g.fill(x + i * seg, y, x2, y + h, colors[i]);
            }
        }
    }

    private void drawSidebar(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, 0xFF071019);
        g.fill(x + w - 3, y, x + w, y + h, 0xFF111F2B);

        int gap = 12;
        int buttonH = (h - gap * 5) / 4;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            int by = y + gap + i * (buttonH + gap);
            drawTab(g, tabs[i], x + 10, by, w - 22, buttonH, mouseX, mouseY);
        }
    }

    private void drawTab(GuiGraphics g, Tab tab, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean active = tab == selected;
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        int glow = active ? CYAN : (hover ? 0xFF7EA8BA : SILVER_3);

        if (active) {
            int glowT = 5;
            g.fill(x - glowT, y + 8, x, y + h - 8, PINK);
            g.fill(x + w, y + 8, x + w + glowT, y + h - 8, CYAN);
            g.fill(x + 8, y - glowT, x + w - 8, y, PURPLE);
            g.fill(x + 8, y + h, x + w - 8, y + h + glowT, CYAN);
        }

        g.fill(x, y, x + w, y + h, glow);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, SILVER_1);
        g.fill(x + 8, y + 8, x + w - 8, y + h - 8, SILVER_3);
        g.fill(x + 13, y + 13, x + w - 13, y + h - 13, active ? 0xFF10263A : PANEL_2);

        int iconCenterX = x + w / 2;
        int iconCenterY = y + Math.max(45, h / 2 - 20);
        int iconScale = Math.max(2, Math.min(4, h / 42));
        switch (tab) {
            case DIFFICULTY -> drawBookIcon(g, iconCenterX, iconCenterY, iconScale);
            case CRAFTS -> drawSwordIcon(g, iconCenterX, iconCenterY, iconScale);
            case MISSIONS -> drawScrollIcon(g, iconCenterX, iconCenterY, iconScale);
            case MOBS -> drawMagnifierIcon(g, iconCenterX, iconCenterY, iconScale);
        }

        int textY = y + h - 42;
        drawCenteredScaled(g, tab.line1, x + w / 2, textY, 1.30f, WHITE);
        drawCenteredScaled(g, tab.line2, x + w / 2, textY + 17, 1.30f, active ? CYAN : MUTED);
    }

    private void drawContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x + 2, y + 2, x + w - 2, y + 5, CYAN);
        g.fill(x + 2, y + h - 5, x + w - 2, y + h - 2, 0xFF18566B);

        int headerH = Math.max(82, Math.min(118, h / 6));
        drawHeader(g, x + 16, y + 10, w - 32, headerH);

        int scrollW = 10;
        int gridX = x + 16;
        int gridY = y + headerH + 22;
        int gridW = w - 44 - scrollW;
        int gridH = h - headerH - 38;
        drawGrid(g, gridX, gridY, gridW, gridH, mouseX, mouseY);

        int sx = x + w - 18;
        g.fill(sx, gridY, sx + 5, gridY + gridH, 0xFFBFC8D3);
        int thumbH = Math.max(70, gridH / 3);
        g.fill(sx - 1, gridY + 3, sx + 6, gridY + thumbH, WHITE);
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 42, y, x + w - 42, y + h, 0xFF091621);
        g.fill(x + 14, y + 14, x + 42, y + h - 14, 0xFF091621);
        g.fill(x + w - 42, y + 14, x + w - 14, y + h - 14, 0xFF091621);
        g.fill(x + 48, y + 5, x + w - 48, y + 8, 0xFF1C465C);
        g.fill(x + 18, y + 18, x + 23, y + h - 18, PINK);
        g.fill(x + w - 23, y + 18, x + w - 18, y + h - 18, CYAN);

        int midY = y + h / 2;
        g.fill(x + 30, midY - 2, x + 145, midY + 2, PINK);
        g.fill(x + w - 145, midY - 2, x + w - 30, midY + 2, CYAN);

        drawNeonTitle(g, selected.title, x + w / 2, y + h / 2 - 15, Math.max(2.3f, Math.min(4.0f, w / 360.0f)));
    }

    private void drawNeonTitle(GuiGraphics g, String text, int centerX, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(centerX, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        int half = font.width(text) / 2;
        g.drawString(font, text, -half - 1, 1, CYAN, false);
        g.drawString(font, text, -half + 1, 1, PINK, false);
        g.drawString(font, text, -half, 0, WHITE, false);
        g.pose().popPose();
    }

    private void drawGrid(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cols = w > 1100 ? 13 : (w > 900 ? 12 : (w > 700 ? 10 : 8));
        int rows = 6;
        int gap = 4;
        int cellW = Math.max(48, (w - gap * (cols - 1)) / cols);
        int cellH = Math.max(54, Math.min(96, (h - gap * (rows - 1)) / rows));
        int visibleRows = Math.max(1, Math.min(rows, (h + gap) / (cellH + gap)));

        int slots = cols * visibleRows;
        int shownSlots = Math.max(cols * (visibleRows - 1) + Math.min(3, cols), cols);

        for (int i = 0; i < slots; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cellW + gap);
            int cy = y + row * (cellH + gap);
            boolean hover = inside(mouseX, mouseY, cx, cy, cellW, cellH);

            g.fill(cx, cy, cx + cellW, cy + cellH, hover ? GRID_HOVER : GRID);
            g.fill(cx + 2, cy + 2, cx + cellW - 2, cy + cellH - 2, 0xFF041019);

            if (i < shownSlots) {
                int s = Math.max(3, Math.min(7, Math.min(cellW / 11, cellH / 12)));
                drawQuestion(g, cx + cellW / 2, cy + cellH / 2 - s * 2, s);
            }
        }
    }

    private void drawQuestion(GuiGraphics g, int centerX, int centerY, int s) {
        int x = centerX - 4 * s;
        int y = centerY - 5 * s;

        fill(g, x, y + s, 2 * s, 3 * s, CYAN);
        fill(g, x + s, y, 5 * s, s, CYAN);
        fill(g, x + 4 * s, y + 3 * s, 2 * s, s, CYAN);
        fill(g, x + 6 * s, y + s, s, 3 * s, PINK);
        fill(g, x + 4 * s, y + 4 * s, s, 2 * s, PINK);
        fill(g, x + 2 * s, y, 4 * s, s, WHITE);
        fill(g, x + s, y + s, s, 2 * s, WHITE);
        fill(g, x + 5 * s, y + s, s, 2 * s, WHITE);
        fill(g, x + 4 * s, y + 3 * s, s, s, WHITE);
        fill(g, x + 3 * s, y + 4 * s, s, 2 * s, YELLOW);
        fill(g, x + 3 * s, y + 7 * s, s, s, PINK);
        fill(g, x + 2 * s, y + 4 * s, s, s, GREEN);
    }

    private void drawBookIcon(GuiGraphics g, int cx, int cy, int s) {
        int x = cx - 11 * s;
        int y = cy - 8 * s;
        fill(g, x + s, y + 2*s, 9*s, 12*s, SILVER_1);
        fill(g, x + 11*s, y + 2*s, 9*s, 12*s, SILVER_1);
        fill(g, x + 9*s, y + 3*s, 2*s, 12*s, SILVER_3);
        fill(g, x + 2*s, y + s, 7*s, s, YELLOW);
        fill(g, x + 12*s, y + s, 7*s, s, YELLOW);
        fill(g, x, y + 4*s, s, 10*s, CYAN);
        fill(g, x + 20*s, y + 4*s, s, 10*s, PINK);
        for (int i = 0; i < 3; i++) {
            fill(g, x + 3*s, y + (5+i*3)*s, 5*s, s, SILVER_3);
            fill(g, x + 13*s, y + (5+i*3)*s, 5*s, s, SILVER_3);
        }
    }

    private void drawSwordIcon(GuiGraphics g, int cx, int cy, int s) {
        int x = cx - 10*s;
        int y = cy + 8*s;
        for (int i = 0; i < 12; i++) {
            fill(g, x + (i+4)*s, y - (i+4)*s, 2*s, 2*s, WHITE);
            fill(g, x + (i+3)*s, y - (i+4)*s, s, 2*s, CYAN);
            fill(g, x + (i+6)*s, y - (i+4)*s, s, 2*s, PINK);
        }
        fill(g, x + 2*s, y - 4*s, 9*s, 2*s, YELLOW);
        fill(g, x + 5*s, y - s, 2*s, 6*s, 0xFFB8792B);
        fill(g, x + 4*s, y + 4*s, 4*s, 2*s, PINK);
    }

    private void drawScrollIcon(GuiGraphics g, int cx, int cy, int s) {
        int x = cx - 9*s;
        int y = cy - 9*s;
        fill(g, x + s, y + s, 15*s, 16*s, SILVER_1);
        fill(g, x, y + 2*s, s, 14*s, CYAN);
        fill(g, x + 16*s, y + 2*s, s, 14*s, PINK);
        fill(g, x + 2*s, y, 13*s, 2*s, YELLOW);
        fill(g, x + 2*s, y + 16*s, 13*s, 2*s, SILVER_2);
        for (int i = 0; i < 4; i++) {
            fill(g, x + 4*s, y + (4+i*3)*s, 9*s, s, SILVER_3);
        }
    }

    private void drawMagnifierIcon(GuiGraphics g, int cx, int cy, int s) {
        int x = cx - 9*s;
        int y = cy - 10*s;
        fill(g, x + 3*s, y, 8*s, 2*s, YELLOW);
        fill(g, x + s, y + 2*s, 2*s, 8*s, CYAN);
        fill(g, x + 11*s, y + 2*s, 2*s, 8*s, PINK);
        fill(g, x + 3*s, y + 10*s, 8*s, 2*s, GREEN);
        fill(g, x + 3*s, y + 2*s, 8*s, 8*s, 0xFF293344);
        for (int i = 0; i < 6; i++) {
            fill(g, x + (10+i)*s, y + (10+i)*s, 2*s, 2*s, WHITE);
        }
        fill(g, x + 15*s, y + 15*s, 3*s, 4*s, PINK);
    }

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
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
            int margin = Math.max(8, Math.min(16, width / 110));
            int x = margin + 28;
            int y = margin + 28;
            int w = width - margin * 2 - 56;
            int h = height - margin * 2 - 56;
            int sideW = Math.max(180, Math.min(245, w / 7));
            int gap = 12;
            int buttonH = (h - gap * 5) / 4;

            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                int bx = x + 10;
                int by = y + gap + i * (buttonH + gap);
                int bw = sideW - 22;
                if (inside(mouseX, mouseY, bx, by, bw, buttonH)) {
                    selected = tabs[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private enum Tab {
        DIFFICULTY("CAMBIOS DE", "DIFICULTAD", "CAMBIOS DE DIFICULTAD"),
        CRAFTS("NUEVOS", "CRAFTEOS", "NUEVOS CRAFTEOS"),
        MISSIONS("MISIONES", "DIARIAS", "MISIONES DIARIAS"),
        MOBS("MOBS", "HABILITADOS", "MOBS HABILITADOS");

        final String line1;
        final String line2;
        final String title;

        Tab(String line1, String line2, String title) {
            this.line1 = line1;
            this.line2 = line2;
            this.title = title;
        }
    }
}
