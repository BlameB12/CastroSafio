package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;

/**
 * Adds an OP-only delete action to the craft editor without exposing it in the
 * normal player menu. The editor itself can only be opened by the server-side
 * /castrosafio editor crafteos command, which requires OP permission.
 */
public final class CraftDeleteOverlay {
    private static final int RED = 0xFFE74C5C;
    private static final int RED_HOVER = 0xFFFF6977;
    private static final int DARK = 0xFF18090D;
    private static final int WHITE = 0xFFF5F8FF;

    private static Field modeField;
    private static Field craftResultField;
    private static String message = "";
    private static long messageUntil;

    private CraftDeleteOverlay() {}

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AdminEditorScreen screen) || !isCraftEditor(screen)) return;

        String resultId = selectedResult(screen);
        CraftDefinition craft = findPublished(resultId);
        if (craft == null) {
            drawMessage(event.getGuiGraphics(), screen);
            return;
        }

        int x = screen.width - 300;
        int y = screen.height - 82;
        int w = 276;
        int h = 20;
        boolean hover = inside(event.getMouseX(), event.getMouseY(), x, y, w, h);

        GuiGraphics g = event.getGuiGraphics();
        g.fill(x, y, x + w, y + h, hover ? RED_HOVER : RED);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, DARK);
        Font font = Minecraft.getInstance().font;
        g.drawCenteredString(font, "ELIMINAR CRAFTEO PUBLICADO", x + w / 2, y + 6, hover ? RED_HOVER : WHITE);

        drawMessage(g, screen);
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Post event) {
        if (!(event.getScreen() instanceof AdminEditorScreen screen) || event.getButton() != 0 || !isCraftEditor(screen)) return;

        int x = screen.width - 300;
        int y = screen.height - 82;
        int w = 276;
        int h = 20;
        if (!inside(event.getMouseX(), event.getMouseY(), x, y, w, h)) return;

        String resultId = selectedResult(screen);
        CraftDefinition craft = findPublished(resultId);
        if (craft == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;

        mc.player.connection.sendCommand("castrosafio crafteo limpiar " + craft.id);
        message = "Eliminando: " + craft.name;
        messageUntil = System.currentTimeMillis() + 2500L;
    }

    private static void drawMessage(GuiGraphics g, AdminEditorScreen screen) {
        if (message.isBlank()) return;
        if (System.currentTimeMillis() > messageUntil) {
            message = "";
            return;
        }
        Font font = Minecraft.getInstance().font;
        g.drawCenteredString(font, message, screen.width - 162, screen.height - 98, RED_HOVER);
    }

    private static CraftDefinition findPublished(String resultId) {
        if (resultId == null || resultId.isBlank()) return null;
        for (CraftDefinition craft : ClientCatalogData.activeCrafts()) {
            if (craft != null && craft.active && resultId.equals(craft.result)) return craft;
        }
        return null;
    }

    private static boolean isCraftEditor(AdminEditorScreen screen) {
        String mode = readField(screen, true);
        return mode != null && mode.toLowerCase().startsWith("craft");
    }

    private static String selectedResult(AdminEditorScreen screen) {
        String value = readField(screen, false);
        return value == null ? "" : value;
    }

    private static String readField(AdminEditorScreen screen, boolean mode) {
        try {
            Field field = mode ? modeField : craftResultField;
            if (field == null) {
                field = AdminEditorScreen.class.getDeclaredField(mode ? "mode" : "craftResult");
                field.setAccessible(true);
                if (mode) modeField = field;
                else craftResultField = field;
            }
            Object value = field.get(screen);
            return value instanceof String string ? string : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
