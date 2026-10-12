package com.castrosafio.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CastroMenuScreen extends Screen {
    private static final int BG = 0xF2050A0F;
    private static final int FRAME_LIGHT = 0xFFE8ECF3;
    private static final int FRAME_MID = 0xFF9BA4B4;
    private static final int FRAME_DARK = 0xFF4D5666;
    private static final int PANEL = 0xFF061019;
    private static final int PANEL_ALT = 0xFF0B1722;
    private static final int CYAN = 0xFF22E9FF;
    private static final int PINK = 0xFFFF4DD8;
    private static final int YELLOW = 0xFFFFFF52;
    private static final int GREEN = 0xFF66FF7D;
    private static final int PURPLE = 0xFF8E74FF;
    private static final int WHITE = 0xFFF4F7FF;
    private static final int MUTED = 0xFFA9B5C5;

    private final Map<ResourceLocation, LivingEntity> mobPreviews = new HashMap<>();
    private final Set<ResourceLocation> invalidMobPreviews = new HashSet<>();
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
        int sideW = Math.max(150, Math.min(215, uiW / 7));
        drawOuterFrame(g, margin, margin, uiW, uiH);
        drawSidebar(g, margin + 18, margin + 22, sideW - 20, uiH - 44, mouseX, mouseY);
        drawMain(g, margin + sideW + 8, margin + 22, uiW - sideW - 26, uiH - 44, mouseX, mouseY);
    }

    private void drawOuterFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 10, y, x + w - 10, y + h, FRAME_DARK);
        g.fill(x, y + 10, x + w, y + h - 10, FRAME_DARK);
        g.fill(x + 12, y + 4, x + w - 12, y + h - 4, FRAME_LIGHT);
        g.fill(x + 4, y + 12, x + w - 4, y + h - 12, FRAME_LIGHT);
        g.fill(x + 16, y + 8, x + w - 16, y + h - 8, FRAME_MID);
        g.fill(x + 8, y + 16, x + w - 8, y + h - 16, FRAME_MID);
        g.fill(x + 22, y + 22, x + w - 22, y + h - 22, 0xFF02070B);
        drawRainbowBar(g, x + w / 2 - 64, y + 6, 128, 4);
        drawRainbowBar(g, x + w / 2 - 80, y + h - 10, 160, 4);
    }

    private void drawRainbowBar(GuiGraphics g, int x, int y, int w, int h) {
        int[] c = {PINK, 0xFFFF8C45, YELLOW, GREEN, CYAN, PURPLE};
        int seg = Math.max(1, w / c.length);
        for (int i = 0; i < c.length; i++) {
            g.fill(x + i * seg, y, i == c.length - 1 ? x + w : x + (i + 1) * seg, y + h, c[i]);
        }
    }

    private void drawSidebar(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, 0xFF071018);
        int gap = 10;
        int buttonH = (h - gap * 5) / 4;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            int by = y + gap + i * (buttonH + gap);
            boolean active = selected == tabs[i];
            boolean hover = inside(mouseX, mouseY, x + 4, by, w - 8, buttonH);
            g.fill(x + 4, by, x + w - 4, by + buttonH, active ? CYAN : (hover ? 0xFF68A8C4 : FRAME_MID));
            g.fill(x + 10, by + 6, x + w - 10, by + buttonH - 6, active ? 0xFF13283D : PANEL_ALT);
            int ty = by + buttonH / 2 - 9;
            g.drawCenteredString(font, tabs[i].line1, x + w / 2, ty, WHITE);
            g.drawCenteredString(font, tabs[i].line2, x + w / 2, ty + 13, active ? CYAN : MUTED);
        }
    }

    private void drawMain(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 6, CYAN);
        drawHeader(g, x + 12, y + 10, w - 24, 62);
        int contentY = y + 82;
        int contentH = h - 96;
        if (selected == Tab.MISSIONS) drawMission(g, x + 16, contentY, w - 32, contentH);
        else if (selected == Tab.CRAFTS) drawCrafts(g, x + 16, contentY, w - 32, contentH);
        else if (selected == Tab.MOBS) drawMobs(g, x + 16, contentY, w - 32, contentH, mouseX, mouseY);
        else drawLockedGrid(g, x + 16, contentY, w - 32, contentH);
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 24, y, x + w - 24, y + h, 0xFF0A1724);
        g.fill(x, y + 12, x + 24, y + h - 12, 0xFF0A1724);
        g.fill(x + w - 24, y + 12, x + w, y + h - 12, 0xFF0A1724);
        g.fill(x + 4, y + 14, x + 9, y + h - 14, PINK);
        g.fill(x + w - 9, y + 14, x + w - 4, y + h - 14, CYAN);
        g.drawCenteredString(font, selected.title, x + w / 2, y + 26, WHITE);
    }

    private void drawMission(GuiGraphics g, int x, int y, int w, int h) {
        MissionDefinition mission = MissionClientData.active();
        if (mission == null) {
            drawEmpty(g, x, y, w, h, "NO HAY UNA MISION ACTIVA", "Un OP puede crearla con /castrosafio editor misiones");
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFF071018);
        g.fill(x + 8, y + 8, x + w - 8, y + 58, 0xFF102131);
        g.drawCenteredString(font, mission.title, x + w / 2, y + 18, CYAN);
        g.drawCenteredString(font, mission.description == null ? "" : mission.description, x + w / 2, y + 36, MUTED);
        int yy = y + 74;
        if (mission.objectives == null || mission.objectives.isEmpty()) {
            g.drawCenteredString(font, "Esta mision aun no tiene objetivos secundarios", x + w / 2, yy + 20, MUTED);
            return;
        }
        int rowH = 42;
        for (int i = 0; i < mission.objectives.size() && yy + rowH < y + h - 8; i++) {
            MissionObjective objective = mission.objectives.get(i);
            g.fill(x + 14, yy, x + w - 14, yy + rowH - 5, 0xFF102131);
            Item item = itemFromId(objective.target);
            if (item != Items.AIR) g.renderItem(new ItemStack(item), x + 24, yy + 8);
            g.drawString(font, displayItem(objective.target), x + 50, yy + 8, WHITE, false);
            g.drawString(font, "Conseguir: 0 / " + objective.amount, x + 50, yy + 23, CYAN, false);
            yy += rowH;
        }
    }

    private void drawMobs(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        Set<String> mobs = ClientCatalogData.enabledMobs();
        if (mobs.isEmpty()) {
            drawEmpty(g, x, y, w, h, "NO HAY MOBS SELECCIONADOS", "Un OP puede elegirlos con /castrosafio editor mobs");
            return;
        }

        g.fill(x, y, x + w, y + h, 0xFF071018);
        int gap = 10;
        int cols = Math.max(1, w / 390);
        int cardW = (w - 20 - gap * (cols - 1)) / cols;
        int cardH = 126;
        int startX = x + 10;
        int startY = y + 12;
        int index = 0;

        for (String id : mobs) {
            int col = index % cols;
            int row = index / cols;
            int cx = startX + col * (cardW + gap);
            int cy = startY + row * (cardH + gap);
            if (cy + cardH > y + h - 8) break;

            g.fill(cx, cy, cx + cardW, cy + cardH, GREEN);
            g.fill(cx + 3, cy + 3, cx + cardW - 3, cy + cardH - 3, 0xFF0A1621);
            g.fill(cx + 7, cy + 7, cx + 103, cy + cardH - 7, 0xFF071018);

            EntityType<?> type = entityTypeFromId(id);
            LivingEntity preview = type == null ? null : previewEntity(type);
            if (preview != null) {
                float biggest = Math.max(0.65F, Math.max(preview.getBbWidth(), preview.getBbHeight()));
                int scale = Math.max(7, Math.min(38, (int) (48.0F / biggest)));
                try {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g,
                            cx + 10, cy + 10,
                            cx + 100, cy + cardH - 10,
                            scale,
                            0.0625F,
                            (float) mouseX,
                            (float) mouseY,
                            preview
                    );
                } catch (Throwable ignored) {
                    g.drawCenteredString(font, "3D", cx + 55, cy + cardH / 2, MUTED);
                }
            } else {
                g.drawCenteredString(font, "?", cx + 55, cy + cardH / 2, MUTED);
            }

            String name = type == null ? id : type.getDescription().getString();
            int textX = cx + 114;
            int textW = Math.max(80, cardW - 126);
            g.drawString(font, trim(name, Math.max(10, textW / 6)), textX, cy + 16, WHITE, false);
            g.drawString(font, trim(id, Math.max(10, textW / 6)), textX, cy + 33, MUTED, false);
            g.drawString(font, "HABILITADO", textX, cy + 52, GREEN, false);

            String info = ClientCatalogData.mobInfo(id);
            if (info == null || info.isBlank()) info = "Sin informacion adicional.";
            g.drawWordWrap(font, Component.literal(info), textX, cy + 72, textW, info.equals("Sin informacion adicional.") ? MUTED : WHITE);
            index++;
        }

        if (index < mobs.size()) {
            g.drawString(font, "+ " + (mobs.size() - index) + " mobs mas", x + 12, y + h - 18, PINK, false);
        }
    }

    private void drawCrafts(GuiGraphics g, int x, int y, int w, int h) {
        List<CraftDefinition> crafts = ClientCatalogData.activeCrafts();
        if (crafts.isEmpty()) {
            drawEmpty(g, x, y, w, h, "NO HAY CRAFTEOS NUEVOS", "Un OP puede crearlos con /castrosafio editor crafteos");
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFF071018);
        int yy = y + 12;
        for (CraftDefinition craft : crafts) {
            if (yy + 64 > y + h) break;
            g.fill(x + 12, yy, x + w - 12, yy + 56, 0xFF102131);
            Item result = itemFromId(craft.result);
            if (result != Items.AIR) g.renderItem(new ItemStack(result, Math.max(1, craft.resultAmount)), x + 26, yy + 19);
            g.drawString(font, craft.name, x + 54, yy + 12, WHITE, false);
            g.drawString(font, "Resultado: " + displayItem(craft.result) + " x" + craft.resultAmount, x + 54, yy + 30, CYAN, false);
            yy += 64;
        }
    }

    private void drawLockedGrid(GuiGraphics g, int x, int y, int w, int h) {
        int cols = Math.max(5, w / 90);
        int rows = Math.max(3, h / 90);
        int gap = 6;
        int cw = (w - gap * (cols - 1)) / cols;
        int ch = (h - gap * (rows - 1)) / rows;
        for (int i = 0; i < cols * rows; i++) {
            int cx = x + (i % cols) * (cw + gap);
            int cy = y + (i / cols) * (ch + gap);
            g.fill(cx, cy, cx + cw, cy + ch, 0xFF163447);
            g.fill(cx + 3, cy + 3, cx + cw - 3, cy + ch - 3, 0xFF07121B);
            g.drawCenteredString(font, "?", cx + cw / 2, cy + ch / 2 - 4, i % 2 == 0 ? CYAN : PINK);
        }
    }

    private void drawEmpty(GuiGraphics g, int x, int y, int w, int h, String title, String subtitle) {
        g.fill(x, y, x + w, y + h, 0xFF071018);
        g.drawCenteredString(font, title, x + w / 2, y + h / 2 - 14, WHITE);
        g.drawCenteredString(font, subtitle, x + w / 2, y + h / 2 + 8, MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                int by = sy + gap + i * (buttonH + gap);
                if (inside(mouseX, mouseY, sx + 4, by, sw - 8, buttonH)) {
                    selected = tabs[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private LivingEntity previewEntity(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null || minecraft == null || minecraft.level == null) return null;
        LivingEntity cached = mobPreviews.get(id);
        if (cached != null) return cached;
        if (invalidMobPreviews.contains(id)) return null;

        try {
            Entity entity = type.create(minecraft.level);
            if (entity instanceof LivingEntity living) {
                mobPreviews.put(id, living);
                return living;
            }
        } catch (Throwable ignored) {
        }
        invalidMobPreviews.add(id);
        return null;
    }

    private static EntityType<?> entityTypeFromId(String id) {
        if (id == null || id.isBlank()) return null;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) return null;
        return BuiltInRegistries.ENTITY_TYPE.get(key);
    }

    private static Item itemFromId(String id) {
        if (id == null || id.isBlank()) return Items.AIR;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return Items.AIR;
        return BuiltInRegistries.ITEM.get(key);
    }

    private static String displayItem(String id) {
        Item item = itemFromId(id);
        return item == Items.AIR ? (id == null ? "" : id) : item.getDescription().getString();
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private enum Tab {
        DIFFICULTY("CAMBIOS DE", "DIFICULTAD", "CAMBIOS DE DIFICULTAD"),
        CRAFTS("NUEVOS", "CRAFTEOS", "NUEVOS CRAFTEOS"),
        MISSIONS("MISIONES", "DIARIAS", "MISION ACTIVA"),
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
