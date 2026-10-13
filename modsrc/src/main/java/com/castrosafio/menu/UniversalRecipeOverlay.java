package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Dibuja la receta segun el sistema real que la creo: crafting, herreria, horno o modded. */
public final class UniversalRecipeOverlay {
    private static final int CYAN = 0xFF22E9FF;
    private static final int PINK = 0xFFFF4DD8;
    private static final int WHITE = 0xFFF4F7FF;
    private static final int MUTED = 0xFFA9B5C5;
    private static final int FRAME = 0xFF9BA4B4;
    private static final int DARK = 0xFF071018;
    private static final int PANEL = 0xFF08131D;

    private static Field selectedField;

    private UniversalRecipeOverlay() {}

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CastroMenuScreen screen) || !isCraftTab(screen)) return;
        CraftDefinition craft = hoveredCraft(screen, event.getMouseX(), event.getMouseY());
        if (craft == null) return;
        drawPopup(event.getGuiGraphics(), screen, craft, event.getMouseX(), event.getMouseY());
    }

    private static CraftDefinition hoveredCraft(CastroMenuScreen screen, double mouseX, double mouseY) {
        List<CraftDefinition> crafts = ClientCatalogData.activeCrafts();
        if (crafts.isEmpty()) return null;

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

        int craftX = mainX + 16;
        int craftY = mainY + 82;
        int craftW = mainW - 32;
        int craftH = mainH - 96;
        int cardX = craftX + 12;
        int cardW = craftW - 24;
        int yy = craftY + 12;

        for (CraftDefinition craft : crafts) {
            if (yy + 64 > craftY + craftH) break;
            if (inside(mouseX, mouseY, cardX, yy, cardW, 56)) return craft;
            yy += 64;
        }
        return null;
    }

    private static void drawPopup(GuiGraphics g, CastroMenuScreen screen, CraftDefinition craft, int mouseX, int mouseY) {
        // Tapa primero el popup viejo 3x3 de la pantalla base.
        int oldW = 218;
        int oldH = 126;
        int oldX = Math.max(8, Math.min(mouseX + 14, screen.width - oldW - 8));
        int oldY = Math.max(8, Math.min(mouseY + 14, screen.height - oldH - 8));
        g.fill(oldX - 1, oldY - 1, oldX + oldW + 1, oldY + oldH + 1, 0xF2050A0F);

        int popupW = 276;
        int popupH = 142;
        int px = Math.max(8, Math.min(mouseX + 14, screen.width - popupW - 8));
        int py = Math.max(8, Math.min(mouseY + 14, screen.height - popupH - 8));
        Font font = Minecraft.getInstance().font;

        g.fill(px, py, px + popupW, py + popupH, CYAN);
        g.fill(px + 2, py + 2, px + popupW - 2, py + popupH - 2, PANEL);
        g.drawString(font, trim(craft.name, 34), px + 10, py + 9, WHITE, false);
        g.drawString(font, friendlyType(craft.recipeType), px + 10, py + 24, CYAN, false);
        if (craft.recipeId != null && !craft.recipeId.isBlank()) {
            g.drawString(font, trim(craft.recipeId, 43), px + 10, py + 37, MUTED, false);
        }

        String type = craft.recipeType == null ? "" : craft.recipeType.toLowerCase();
        if (type.contains("crafting")) {
            drawCrafting(g, craft, px + 10, py + 53);
        } else if (type.contains("smithing")) {
            // Herreria: se muestran directamente los tres ITEMS reales de la receta.
            // Nada de textos tipo PLANTILLA / BASE / MATERIAL dentro de los slots.
            drawSmithing(g, craft, px + 14, py + 70);
        } else if (type.contains("smelting") || type.contains("blasting") || type.contains("smoking")
                || type.contains("campfire") || type.contains("stonecutting")) {
            drawLinear(g, craft, px + 24, py + 72, 1);
        } else {
            drawGeneric(g, craft, px + 10, py + 55);
        }
    }

    private static void drawCrafting(GuiGraphics g, CraftDefinition craft, int gridX, int gridY) {
        int cell = 26;
        for (int i = 0; i < 9; i++) {
            int cx = gridX + (i % 3) * cell;
            int cy = gridY + (i / 3) * cell;
            drawSlot(g, cx, cy, ingredient(craft, i));
        }
        int arrowX = gridX + cell * 3 + 10;
        int arrowY = gridY + 30;
        g.drawString(Minecraft.getInstance().font, "=>", arrowX, arrowY, WHITE, false);
        drawResult(g, craft, arrowX + 30, gridY + 22);
    }

    private static void drawSmithing(GuiGraphics g, CraftDefinition craft, int startX, int y) {
        List<String> inputs = nonBlankIngredients(craft);

        // Siempre se dibujan los tres espacios reales de una mesa de herreria.
        // Si el RecipeType del mod expone sus ingredientes, cada slot contiene el item real.
        for (int i = 0; i < 3; i++) {
            int x = startX + i * 38;
            String ingredientId = i < inputs.size() ? inputs.get(i) : "";
            drawSlot(g, x, y, ingredientId);
            if (i < 2) {
                g.drawString(Minecraft.getInstance().font, "+", x + 27, y + 8, MUTED, false);
            }
        }

        int arrowX = startX + 116;
        g.drawString(Minecraft.getInstance().font, "=>", arrowX, y + 8, WHITE, false);
        drawResult(g, craft, arrowX + 31, y - 5);
    }

    private static void drawLinear(GuiGraphics g, CraftDefinition craft, int startX, int y, int maxInputs) {
        List<String> inputs = nonBlankIngredients(craft);
        int shown = Math.min(maxInputs, inputs.size());
        int x = startX;
        for (int i = 0; i < shown; i++) {
            drawSlot(g, x, y, inputs.get(i));
            x += 34;
            if (i < shown - 1) {
                g.drawString(Minecraft.getInstance().font, "+", x - 7, y + 8, MUTED, false);
            }
        }
        if (shown == 0) {
            // Se conserva el espacio visual, pero no se inventan nombres de ingredientes.
            drawSlot(g, x, y, "");
            x += 34;
        }
        g.drawString(Minecraft.getInstance().font, "=>", x + 2, y + 8, WHITE, false);
        drawResult(g, craft, x + 29, y - 5);
    }

    private static void drawGeneric(GuiGraphics g, CraftDefinition craft, int x, int y) {
        List<String> inputs = nonBlankIngredients(craft);
        if (inputs.isEmpty()) {
            // RecipeType personalizado que no expone ingredientes: se muestra un slot vacio,
            // sin reemplazarlo por palabras que parezcan ingredientes.
            drawSlot(g, x, y + 5, "");
            g.drawString(Minecraft.getInstance().font, "=>", x + 36, y + 13, WHITE, false);
            drawResult(g, craft, x + 64, y);
            return;
        }

        int shown = Math.min(6, inputs.size());
        for (int i = 0; i < shown; i++) {
            int cx = x + (i % 3) * 26;
            int cy = y + (i / 3) * 26;
            drawSlot(g, cx, cy, inputs.get(i));
        }
        g.drawString(Minecraft.getInstance().font, "=>", x + 88, y + 20, WHITE, false);
        drawResult(g, craft, x + 116, y + 7);
        if (inputs.size() > shown) {
            g.drawString(Minecraft.getInstance().font, "+" + (inputs.size() - shown), x + 62, y + 57, MUTED, false);
        }
    }

    private static void drawSlot(GuiGraphics g, int x, int y, String id) {
        g.fill(x, y, x + 23, y + 23, FRAME);
        g.fill(x + 2, y + 2, x + 21, y + 21, DARK);
        Item item = itemFromId(id);
        if (item != Items.AIR) g.renderItem(new ItemStack(item), x + 3, y + 3);
    }

    private static void drawResult(GuiGraphics g, CraftDefinition craft, int x, int y) {
        g.fill(x, y, x + 34, y + 34, PINK);
        g.fill(x + 2, y + 2, x + 32, y + 32, DARK);
        Item item = itemFromId(craft.result);
        if (item != Items.AIR) g.renderItem(new ItemStack(item, Math.max(1, craft.resultAmount)), x + 9, y + 9);
    }

    private static List<String> nonBlankIngredients(CraftDefinition craft) {
        List<String> out = new ArrayList<>();
        if (craft.ingredients == null) return out;
        for (String ingredient : craft.ingredients) {
            if (ingredient != null && !ingredient.isBlank()) out.add(ingredient);
        }
        return out;
    }

    private static String ingredient(CraftDefinition craft, int index) {
        if (craft.ingredients == null || index < 0 || index >= craft.ingredients.size()) return "";
        String value = craft.ingredients.get(index);
        return value == null ? "" : value;
    }

    private static Item itemFromId(String id) {
        if (id == null || id.isBlank()) return Items.AIR;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return Items.AIR;
        return BuiltInRegistries.ITEM.get(key);
    }

    private static String friendlyType(String raw) {
        if (raw == null || raw.isBlank()) return "RECETA";
        String value = raw.toLowerCase();
        if (value.contains("smithing")) return "MESA DE HERRERIA";
        if (value.contains("blasting")) return "ALTO HORNO";
        if (value.contains("smoking")) return "AHUMADOR";
        if (value.contains("smelting")) return "HORNO";
        if (value.contains("campfire")) return "HOGUERA";
        if (value.contains("stonecutting")) return "CORTAPIEDRAS";
        if (value.contains("crafting")) return "MESA DE CRAFTEO";
        int colon = raw.indexOf(':');
        String namespace = colon >= 0 ? raw.substring(0, colon) : "MOD";
        String path = colon >= 0 ? raw.substring(colon + 1) : raw;
        return (namespace + " - " + path.replace('_', ' ')).toUpperCase();
    }

    private static boolean isCraftTab(CastroMenuScreen screen) {
        try {
            if (selectedField == null) {
                selectedField = CastroMenuScreen.class.getDeclaredField("selected");
                selectedField.setAccessible(true);
            }
            Object value = selectedField.get(screen);
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
}
