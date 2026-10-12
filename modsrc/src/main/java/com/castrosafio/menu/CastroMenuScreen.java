package com.castrosafio.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CastroMenuScreen extends Screen {
    private static final int BORDER = 0xFFE5E8EF;
    private static final int BORDER_DARK = 0xFF596171;
    private static final int PANEL = 0xF207111B;
    private static final int PANEL_2 = 0xFF0B1722;
    private static final int GRID = 0xFF173345;
    private static final int CYAN = 0xFF23E7FF;
    private static final int PINK = 0xFFFF45D7;
    private static final int YELLOW = 0xFFFFFF52;
    private static final int WHITE = 0xFFF5F7FF;
    private static final int MUTED = 0xFFAAB5C2;

    private Tab selected = Tab.MISSIONS;

    public CastroMenuScreen() {
        super(Component.literal("Menu de Misiones"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE800050A);

        int uiW = Math.min(width - 20, 1120);
        int uiH = Math.min(height - 20, 650);
        int x = (width - uiW) / 2;
        int y = (height - uiH) / 2;
        int sideW = Math.max(118, Math.min(170, uiW / 6));

        drawFrame(graphics, x, y, uiW, uiH);
        drawSidebar(graphics, x + 10, y + 12, sideW - 12, uiH - 24, mouseX, mouseY);
        drawMainPanel(graphics, x + sideW + 8, y + 12, uiW - sideW - 18, uiH - 24, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BORDER_DARK);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BORDER);
        g.fill(x + 8, y + 8, x + w - 8, y + h - 8, 0xFF111923);
        g.fill(x + 11, y + 11, x + w - 11, y + h - 11, 0xFF050B11);

        int center = x + w / 2;
        g.fill(center - 70, y + 3, center + 70, y + 7, 0xFF1A2330);
        int[] neon = {PINK, YELLOW, 0xFF66FF72, CYAN, 0xFF7C6DFF};
        for (int i = 0; i < neon.length; i++) {
            int sx = center - 45 + i * 18;
            g.fill(sx, y + 4, sx + 13, y + 6, neon[i]);
        }
    }

    private void drawSidebar(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, 0xFF07101A);
        int gap = 8;
        int buttonH = (h - gap * 5) / 4;

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            int by = y + gap + i * (buttonH + gap);
            drawTab(g, tabs[i], x + 7, by, w - 14, buttonH, mouseX, mouseY);
        }
    }

    private void drawTab(GuiGraphics g, Tab tab, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean active = selected == tab;
        boolean hover = in(mouseX, mouseY, x, y, w, h);
        int glow = active ? CYAN : (hover ? 0xFF4D7185 : BORDER_DARK);

        g.fill(x, y, x + w, y + h, glow);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, BORDER);
        g.fill(x + 6, y + 6, x + w - 6, y + h - 6, active ? 0xFF102338 : PANEL_2);

        if (active) {
            g.fill(x + 3, y + 3, x + 6, y + h - 3, PINK);
            g.fill(x + w - 6, y + 3, x + w - 3, y + h - 3, CYAN);
        }

        int iconX = x + w / 2 - 8;
        int iconY = y + Math.max(9, h / 2 - 24);
        g.renderItem(new ItemStack(tab.icon), iconX, iconY);

        int textY = y + h - 27;
        g.drawCenteredString(font, tab.line1, x + w / 2, textY, WHITE);
        if (!tab.line2.isEmpty()) {
            g.drawCenteredString(font, tab.line2, x + w / 2, textY + 11, active ? CYAN : MUTED);
        }
    }

    private void drawMainPanel(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x + 2, y + 2, x + w - 2, y + 4, CYAN);
        g.fill(x + 2, y + h - 4, x + w - 2, y + h - 2, 0xFF1F5A70);

        String title = selected.title;
        g.fill(x + 14, y + 12, x + w - 14, y + 50, 0xFF0A1724);
        g.fill(x + 18, y + 15, x + 22, y + 47, PINK);
        g.fill(x + w - 22, y + 15, x + w - 18, y + 47, CYAN);
        g.drawCenteredString(font, title, x + w / 2, y + 26, WHITE);

        int gridX = x + 16;
        int gridY = y + 62;
        int gridW = w - 32;
        int gridH = h - 78;
        drawGrid(g, gridX, gridY, gridW, gridH, mouseX, mouseY);
    }

    private void drawGrid(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cols = w >= 760 ? 10 : (w >= 600 ? 8 : 6);
        int rows = 5;
        int gap = 5;
        int cellW = (w - gap * (cols - 1)) / cols;
        int cellH = Math.max(48, Math.min(76, (h - gap * (rows - 1)) / rows));
        int maxRows = Math.max(1, Math.min(rows, (h + gap) / (cellH + gap)));
        int count = cols * maxRows;

        for (int i = 0; i < count; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cellW + gap);
            int cy = y + row * (cellH + gap);
            boolean hover = in(mouseX, mouseY, cx, cy, cellW, cellH);

            g.fill(cx, cy, cx + cellW, cy + cellH, hover ? 0xFF2A5365 : GRID);
            g.fill(cx + 2, cy + 2, cx + cellW - 2, cy + cellH - 2, 0xFF061019);

            int questionColor = switch (i % 3) {
                case 0 -> CYAN;
                case 1 -> PINK;
                default -> YELLOW;
            };
            g.drawCenteredString(font, "?", cx + cellW / 2, cy + cellH / 2 - 4, questionColor);

            if (hover) {
                String hint = switch (selected) {
                    case DIFFICULTY -> "Cambio bloqueado";
                    case CRAFTS -> "Crafteo bloqueado";
                    case MISSIONS -> "Mision por descubrir";
                    case MOBS -> "Mob aun no habilitado";
                };
                g.drawCenteredString(font, hint, x + w / 2, y + h - 10, MUTED);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int uiW = Math.min(width - 20, 1120);
            int uiH = Math.min(height - 20, 650);
            int x = (width - uiW) / 2;
            int y = (height - uiH) / 2;
            int sideW = Math.max(118, Math.min(170, uiW / 6));
            int sx = x + 10;
            int sy = y + 12;
            int sw = sideW - 12;
            int sh = uiH - 24;
            int gap = 8;
            int buttonH = (sh - gap * 5) / 4;

            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                int bx = sx + 7;
                int by = sy + gap + i * (buttonH + gap);
                int bw = sw - 14;
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
