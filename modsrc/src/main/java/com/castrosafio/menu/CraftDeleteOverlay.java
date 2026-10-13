package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.List;

/**
 * OP-only controls to remove published crafts.
 *
 * The delete controls are shown in two places:
 *  - inside /castrosafio editor crafteos as an always-visible published list;
 *  - in G -> NUEVOS CRAFTEOS, but only for players with permission level 2+.
 *
 * The server command still validates permissions, so the client button is never
 * the authority for deleting a craft.
 */
public final class CraftDeleteOverlay {
    private static final int RED = 0xFFE74C5C;
    private static final int RED_HOVER = 0xFFFF6977;
    private static final int DARK = 0xFF18090D;
    private static final int WHITE = 0xFFF5F8FF;
    private static final int MUTED = 0xFF9CB3C7;

    private static Field modeField;
    private static Field selectedTabField;
    private static String message = "";
    private static long messageUntil;

    private CraftDeleteOverlay() {}

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!isLocalOp()) return;

        if (event.getScreen() instanceof AdminEditorScreen screen && isCraftEditor(screen)) {
            renderEditorDeleteList(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
            return;
        }

        if (event.getScreen() instanceof CastroMenuScreen screen && isMainCraftTab(screen)) {
            renderMainMenuDeleteButtons(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Post event) {
        if (event.getButton() != 0 || !isLocalOp()) return;

        if (event.getScreen() instanceof AdminEditorScreen screen && isCraftEditor(screen)) {
            CraftDefinition craft = craftAtEditorDeleteButton(screen, event.getMouseX(), event.getMouseY());
            if (craft != null) {
                deleteCraft(craft);
            }
            return;
        }

        if (event.getScreen() instanceof CastroMenuScreen screen && isMainCraftTab(screen)) {
            CraftDefinition craft = craftAtMainDeleteButton(screen, event.getMouseX(), event.getMouseY());
            if (craft != null) {
                deleteCraft(craft);
            }
        }
    }

    private static void renderEditorDeleteList(GuiGraphics g, AdminEditorScreen screen, double mouseX, double mouseY) {
        List<CraftDefinition> crafts = ClientCatalogData.activeCrafts();
        int x = screen.width - 300;
        int w = 276;
        int startY = 330;
        int bottom = screen.height - 88;

        g.drawString(Minecraft.getInstance().font, "CRAFTEOS PUBLICADOS - SOLO OP", x, startY - 18, RED_HOVER, false);

        if (crafts.isEmpty()) {
            g.drawString(Minecraft.getInstance().font, "No hay crafteos publicados.", x, startY + 4, MUTED, false);
            drawMessage(g, screen.width - 162, startY - 34);
            return;
        }

        int rowH = 26;
        int shown = 0;
        for (int i = 0; i < crafts.size(); i++) {
            int y = startY + i * rowH;
            if (y + 22 > bottom) break;
            CraftDefinition craft = crafts.get(i);
            boolean hover = inside(mouseX, mouseY, x + w - 82, y, 82, 20);

            g.fill(x, y, x + w, y + 20, 0xFF101B25);
            g.drawString(Minecraft.getInstance().font, trim(craft.name, 24), x + 6, y + 6, WHITE, false);
            drawDeleteButton(g, x + w - 82, y, 82, 20, hover);
            shown++;
        }

        if (shown < crafts.size()) {
            g.drawString(Minecraft.getInstance().font, "+ " + (crafts.size() - shown) + " mas", x, bottom - 12, MUTED, false);
        }
        drawMessage(g, screen.width - 162, startY - 34);
    }

    private static CraftDefinition craftAtEditorDeleteButton(AdminEditorScreen screen, double mouseX, double mouseY) {
        List<CraftDefinition> crafts = ClientCatalogData.activeCrafts();
        int x = screen.width - 300;
        int w = 276;
        int startY = 330;
        int bottom = screen.height - 88;
        int rowH = 26;

        for (int i = 0; i < crafts.size(); i++) {
            int y = startY + i * rowH;
            if (y + 22 > bottom) break;
            if (inside(mouseX, mouseY, x + w - 82, y, 82, 20)) return crafts.get(i);
        }
        return null;
    }

    private static void renderMainMenuDeleteButtons(GuiGraphics g, CastroMenuScreen screen, double mouseX, double mouseY) {
        List<CraftDefinition> crafts = ClientCatalogData.activeCrafts();
        if (crafts.isEmpty()) return;

        MainCraftLayout layout = mainCraftLayout(screen);
        int yy = layout.firstRowY;
        for (CraftDefinition craft : crafts) {
            if (yy + 64 > layout.contentBottom) break;
            int bx = layout.rowX + layout.rowW - 94;
            int by = yy + 18;
            boolean hover = inside(mouseX, mouseY, bx, by, 78, 20);
            drawDeleteButton(g, bx, by, 78, 20, hover);
            yy += 64;
        }

        drawMessage(g, layout.rowX + layout.rowW - 135, layout.firstRowY - 18);
    }

    private static CraftDefinition craftAtMainDeleteButton(CastroMenuScreen screen, double mouseX, double mouseY) {
        List<CraftDefinition> crafts = ClientCatalogData.activeCrafts();
        MainCraftLayout layout = mainCraftLayout(screen);
        int yy = layout.firstRowY;

        for (CraftDefinition craft : crafts) {
            if (yy + 64 > layout.contentBottom) break;
            int bx = layout.rowX + layout.rowW - 94;
            int by = yy + 18;
            if (inside(mouseX, mouseY, bx, by, 78, 20)) return craft;
            yy += 64;
        }
        return null;
    }

    private static MainCraftLayout mainCraftLayout(CastroMenuScreen screen) {
        int width = screen.width;
        int height = screen.height;
        int margin = Math.max(8, Math.min(18, width / 100));
        int uiW = width - margin * 2;
        int uiH = height - margin * 2;
        int sideW = Math.max(150, Math.min(215, uiW / 7));

        int mainX = margin + sideW + 8;
        int mainY = margin + 22;
        int mainW = uiW - sideW - 26;
        int mainH = uiH - 44;

        int rowX = mainX + 16;
        int contentY = mainY + 82;
        int rowW = mainW - 32;
        int contentH = mainH - 96;
        return new MainCraftLayout(rowX, contentY + 12, rowW, contentY + contentH);
    }

    private static void drawDeleteButton(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? RED_HOVER : RED);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, DARK);
        Font font = Minecraft.getInstance().font;
        g.drawCenteredString(font, "ELIMINAR", x + w / 2, y + 6, hover ? RED_HOVER : WHITE);
    }

    private static void deleteCraft(CraftDefinition craft) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null || craft == null) return;
        mc.player.connection.sendCommand("castrosafio crafteo limpiar " + craft.id);
        message = "Eliminado: " + craft.name;
        messageUntil = System.currentTimeMillis() + 2500L;
    }

    private static void drawMessage(GuiGraphics g, int centerX, int y) {
        if (message.isBlank()) return;
        if (System.currentTimeMillis() > messageUntil) {
            message = "";
            return;
        }
        g.drawCenteredString(Minecraft.getInstance().font, message, centerX, y, RED_HOVER);
    }

    private static boolean isLocalOp() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasPermissions(2);
    }

    private static boolean isCraftEditor(AdminEditorScreen screen) {
        try {
            if (modeField == null) {
                modeField = AdminEditorScreen.class.getDeclaredField("mode");
                modeField.setAccessible(true);
            }
            Object value = modeField.get(screen);
            return value instanceof String string && string.toLowerCase().startsWith("craft");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isMainCraftTab(CastroMenuScreen screen) {
        try {
            if (selectedTabField == null) {
                selectedTabField = CastroMenuScreen.class.getDeclaredField("selected");
                selectedTabField.setAccessible(true);
            }
            Object value = selectedTabField.get(screen);
            return value != null && "CRAFTS".equals(value.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private record MainCraftLayout(int rowX, int firstRowY, int rowW, int contentBottom) {}
}
