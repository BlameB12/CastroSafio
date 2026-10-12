package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class MissionOverlayRenderer {
    private static final int SLOT = 0xFF07121B;
    private static final int GREEN = 0xFF66FF7D;
    private static final int CYAN = 0xFF22E9FF;
    private static final int WHITE = 0xFFF4F7FF;
    private static final int MUTED = 0xFFA9B5C5;

    private static boolean missionsTabActive = true;

    private MissionOverlayRenderer() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CastroMenuScreen) {
            missionsTabActive = true;
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Post event) {
        if (!(event.getScreen() instanceof CastroMenuScreen) || event.getButton() != 0) {
            return;
        }

        int width = event.getScreen().width;
        int height = event.getScreen().height;
        int margin = Math.max(8, Math.min(18, width / 100));
        int uiW = width - margin * 2;
        int uiH = height - margin * 2;
        int sideW = Math.max(150, Math.min(215, uiW / 7));
        int sx = margin + 18;
        int sy = margin + 22;
        int sw = sideW - 20;
        int sh = uiH - 44;
        int gap = 10;
        int buttonH = (sh - gap * 5) / 4;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int bx = sx + 4;
        int bw = sw - 8;

        if (mouseX < bx || mouseX >= bx + bw) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            int by = sy + gap + i * (buttonH + gap);
            if (mouseY >= by && mouseY < by + buttonH) {
                missionsTabActive = i == 2;
                return;
            }
        }
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CastroMenuScreen) || !missionsTabActive) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int width = event.getScreen().width;
        int height = event.getScreen().height;

        int margin = Math.max(8, Math.min(18, width / 100));
        int uiW = width - margin * 2;
        int uiH = height - margin * 2;
        int sideW = Math.max(150, Math.min(215, uiW / 7));

        int mainX = margin + sideW + 8;
        int mainY = margin + 22;
        int mainW = uiW - sideW - 26;
        int mainH = uiH - 44;

        int x = mainX + 14;
        int y = mainY + 86;
        int w = mainW - 28;
        int h = mainH - 102;

        int cols = w >= 1100 ? 13 : (w >= 900 ? 11 : (w >= 700 ? 9 : 7));
        int rows = 5;
        int gap = 6;
        int cellW = (w - gap * (cols - 1)) / cols;
        int cellH = (h - gap * (rows - 1)) / rows;

        MissionDefinition hovered = null;

        for (int i = 0; i < cols * rows; i++) {
            MissionDefinition mission = MissionClientData.get(i + 1);
            if (mission == null || !mission.unlocked) {
                continue;
            }

            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cellW + gap);
            int cy = y + row * (cellH + gap);
            boolean hover = inside(event.getMouseX(), event.getMouseY(), cx, cy, cellW, cellH);

            g.fill(cx, cy, cx + cellW, cy + cellH, hover ? 0xFFB2FFC8 : GREEN);
            g.fill(cx + 3, cy + 3, cx + cellW - 3, cy + cellH - 3, hover ? 0xFF10291D : SLOT);

            Font font = Minecraft.getInstance().font;
            g.drawCenteredString(font, "MISION " + mission.id, cx + cellW / 2, cy + 10, CYAN);
            g.drawCenteredString(font, shorten(mission.title, Math.max(7, cellW / 7)), cx + cellW / 2, cy + cellH / 2 - 4, WHITE);
            g.drawCenteredString(font, "ABIERTA", cx + cellW / 2, cy + cellH - 19, GREEN);

            if (hover) {
                hovered = mission;
            }
        }

        if (hovered != null) {
            Font font = Minecraft.getInstance().font;
            String title = "Mision " + hovered.id + ": " + hovered.title;
            String objective = hovered.objectiveSummary();
            String description = hovered.description == null ? "" : hovered.description;

            int panelW = Math.min(w - 20, Math.max(330, font.width(title) + 28));
            int panelH = description.isBlank() ? 40 : 52;
            int px = x + (w - panelW) / 2;
            int py = y + h - panelH - 5;

            g.fill(px, py, px + panelW, py + panelH, 0xEE02070B);
            g.fill(px, py, px + panelW, py + 2, CYAN);
            g.drawCenteredString(font, shorten(title, 60), px + panelW / 2, py + 7, WHITE);
            g.drawCenteredString(font, shorten(objective, 70), px + panelW / 2, py + 20, GREEN);
            if (!description.isBlank()) {
                g.drawCenteredString(font, shorten(description, 80), px + panelW / 2, py + 33, MUTED);
            }
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        if (max <= 3) {
            return text.substring(0, Math.max(0, max));
        }
        return text.substring(0, max - 3) + "...";
    }
}
