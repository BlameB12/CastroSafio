package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AdminEditorScreen extends Screen {
    private static final int PANEL = 0xF20A1119;
    private static final int PANEL_2 = 0xFF111E2A;
    private static final int BORDER = 0xFF7F8FA3;
    private static final int CYAN = 0xFF35E7FF;
    private static final int PINK = 0xFFFF4FD8;
    private static final int WHITE = 0xFFF5F8FF;
    private static final int GREEN = 0xFF5BFF86;
    private static final int RED = 0xFFFF667A;
    private static final int PAGE_SIZE = 40;

    private final String mode;
    private EditBox searchBox;
    private EditBox titleBox;
    private EditBox descriptionBox;
    private int page;
    private final List<MissionObjective> objectives = new ArrayList<>();
    private final String[] craftSlots = new String[9];
    private String craftResult = "";
    private int selectedCraftSlot = 0;

    public AdminEditorScreen(String mode) {
        super(Component.literal("CastroSafio Editor"));
        this.mode = mode == null ? "missions" : mode.toLowerCase(Locale.ROOT);
        for (int i = 0; i < craftSlots.length; i++) craftSlots[i] = "";
    }

    @Override
    protected void init() {
        int top = 18;
        searchBox = new EditBox(font, 24, top + 28, Math.max(180, width / 3), 20, Component.literal("Buscar"));
        searchBox.setHint(Component.literal("Buscar por nombre o mod..."));
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1)).bounds(width - 104, top + 28, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page++).bounds(width - 66, top + 28, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose()).bounds(width - 92, height - 30, 68, 20).build());

        if (isMissions()) {
            MissionDefinition mission = MissionClientData.active();
            if (mission == null) mission = MissionClientData.get(1);
            titleBox = new EditBox(font, width - 300, 62, 276, 20, Component.literal("Titulo"));
            descriptionBox = new EditBox(font, width - 300, 88, 276, 20, Component.literal("Descripcion"));
            if (mission != null) {
                titleBox.setValue(mission.title == null ? "" : mission.title);
                descriptionBox.setValue(mission.description == null ? "" : mission.description);
                if (mission.objectives != null) for (MissionObjective objective : mission.objectives) objectives.add(new MissionObjective(objective.type, objective.target, objective.amount));
            } else {
                titleBox.setValue("Nueva mision");
            }
            addRenderableWidget(titleBox);
            addRenderableWidget(descriptionBox);
            addRenderableWidget(Button.builder(Component.literal("Guardar y activar"), b -> saveMission()).bounds(width - 300, height - 56, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Vaciar objetivos"), b -> objectives.clear()).bounds(width - 154, height - 56, 130, 20).build());
        } else if (isMobs()) {
            addRenderableWidget(Button.builder(Component.literal("Vaciar lista"), b -> {
                sendCommand("castrosafio mob limpiar");
                for (String id : ClientCatalogData.enabledMobs()) ClientCatalogData.setMobLocal(id, false);
            }).bounds(width - 174, height - 56, 150, 20).build());
        } else if (isCrafts()) {
            CraftDefinition craft = ClientCatalogData.craft(1);
            if (craft != null) {
                for (int i = 0; i < 9 && i < craft.ingredients.size(); i++) craftSlots[i] = craft.ingredients.get(i);
                craftResult = craft.result == null ? "" : craft.result;
            }
            titleBox = new EditBox(font, width - 300, 62, 276, 20, Component.literal("Nombre del crafteo"));
            titleBox.setValue(craft == null ? "Nuevo crafteo" : craft.name);
            addRenderableWidget(titleBox);
            addRenderableWidget(Button.builder(Component.literal("Guardar crafteo"), b -> saveCraft()).bounds(width - 300, height - 56, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Limpiar"), b -> clearCraftLocal()).bounds(width - 154, height - 56, 130, 20).build());
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // This editor paints its own opaque background. Calling vanilla's background
        // here would blur/darken the editor after it has already been drawn.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xEE03070B);
        g.fill(12, 12, width - 12, height - 12, BORDER);
        g.fill(16, 16, width - 16, height - 16, PANEL);
        g.fill(20, 20, width - 20, 44, PANEL_2);
        g.drawString(font, titleForMode(), 28, 27, WHITE, false);
        g.drawString(font, "SOLO OP", width - 78, 27, PINK, false);

        int leftX = 24;
        int topY = 58;
        int leftW = Math.max(360, width - 350);
        int panelRightX = leftX + leftW + 10;
        int panelRightW = width - panelRightX - 24;

        g.fill(leftX, topY, leftX + leftW, height - 40, 0xFF071018);
        g.fill(panelRightX, topY, panelRightX + panelRightW, height - 40, 0xFF071018);

        if (isMobs()) renderMobCatalog(g, mouseX, mouseY, leftX, topY + 28, leftW, height - topY - 78);
        else renderItemCatalog(g, mouseX, mouseY, leftX, topY + 28, leftW, height - topY - 78);

        if (isMissions()) renderMissionEditor(g, mouseX, mouseY, panelRightX, topY + 58, panelRightW);
        else if (isMobs()) renderMobInfo(g, panelRightX, topY + 12, panelRightW);
        else renderCraftEditor(g, mouseX, mouseY, panelRightX, topY + 36, panelRightW);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private String titleForMode() {
        if (isMissions()) return "EDITOR DE MISIONES - selecciona objetos para crear objetivos secundarios";
        if (isMobs()) return "EDITOR DE MOBS - todos los mobs registrados por Minecraft y mods";
        return "EDITOR DE CRAFTEOS - selecciona un slot y despues el objeto";
    }

    private void renderItemCatalog(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h) {
        List<Item> values = filteredItems();
        int cols = Math.max(5, w / 72);
        int cell = Math.min(66, (w - 8) / cols);
        int rows = Math.max(1, h / cell);
        int perPage = Math.min(PAGE_SIZE, cols * rows);
        int start = Math.min(page * perPage, Math.max(0, values.size()));
        int end = Math.min(values.size(), start + perPage);
        if (start >= values.size() && page > 0) { page--; return; }

        for (int index = start; index < end; index++) {
            int local = index - start;
            int cx = x + 4 + (local % cols) * cell;
            int cy = y + 4 + (local / cols) * cell;
            Item item = values.get(index);
            boolean hover = inside(mouseX, mouseY, cx, cy, cell - 4, cell - 4);
            g.fill(cx, cy, cx + cell - 4, cy + cell - 4, hover ? 0xFF29465A : 0xFF132736);
            g.fill(cx + 2, cy + 2, cx + cell - 6, cy + cell - 6, 0xFF071018);
            g.renderItem(new ItemStack(item), cx + (cell - 20) / 2, cy + 7);
            String name = item.getDescription().getString();
            g.drawCenteredString(font, trim(name, Math.max(5, (cell - 8) / 6)), cx + (cell - 4) / 2, cy + cell - 18, WHITE);
        }
        g.drawString(font, "Pagina " + (page + 1) + " | " + values.size() + " objetos", x + 6, y + h + 5, 0xFF9CB3C7, false);
    }

    private void renderMobCatalog(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h) {
        List<EntityType<?>> values = filteredEntities();
        int cols = Math.max(3, w / 120);
        int cellW = (w - 8) / cols;
        int cellH = 54;
        int rows = Math.max(1, h / cellH);
        int perPage = Math.min(PAGE_SIZE, cols * rows);
        int start = Math.min(page * perPage, Math.max(0, values.size()));
        int end = Math.min(values.size(), start + perPage);
        if (start >= values.size() && page > 0) { page--; return; }

        for (int index = start; index < end; index++) {
            int local = index - start;
            int cx = x + 4 + (local % cols) * cellW;
            int cy = y + 4 + (local / cols) * cellH;
            EntityType<?> type = values.get(index);
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String idText = id == null ? "?" : id.toString();
            boolean enabled = ClientCatalogData.isMobEnabled(idText);
            boolean hover = inside(mouseX, mouseY, cx, cy, cellW - 5, cellH - 5);
            g.fill(cx, cy, cx + cellW - 5, cy + cellH - 5, enabled ? GREEN : (hover ? 0xFF34516A : 0xFF24313D));
            g.fill(cx + 2, cy + 2, cx + cellW - 7, cy + cellH - 7, 0xFF071018);
            g.drawString(font, trim(type.getDescription().getString(), 18), cx + 6, cy + 8, enabled ? GREEN : WHITE, false);
            g.drawString(font, trim(idText, 20), cx + 6, cy + 25, 0xFF8BA5B8, false);
            g.drawString(font, enabled ? "HABILITADO" : "Click para habilitar", cx + 6, cy + 38, enabled ? GREEN : 0xFF71879A, false);
        }
        g.drawString(font, "Pagina " + (page + 1) + " | " + values.size() + " entidades", x + 6, y + h + 5, 0xFF9CB3C7, false);
    }

    private void renderMissionEditor(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w) {
        g.drawString(font, "OBJETIVOS SECUNDARIOS", x + 10, y, CYAN, false);
        g.drawString(font, "Click izquierdo: agrega / suma", x + 10, y + 14, 0xFFA3B7C8, false);
        g.drawString(font, "Click derecho: resta / elimina", x + 10, y + 27, 0xFFA3B7C8, false);
        int yy = y + 48;
        for (int i = 0; i < objectives.size() && i < 14; i++) {
            MissionObjective objective = objectives.get(i);
            Item item = itemFromId(objective.target);
            g.fill(x + 8, yy, x + w - 8, yy + 26, 0xFF132736);
            if (item != Items.AIR) g.renderItem(new ItemStack(item), x + 13, yy + 5);
            g.drawString(font, trim(displayItem(objective.target), 24), x + 34, yy + 5, WHITE, false);
            g.drawString(font, "x" + objective.amount, x + w - 42, yy + 8, CYAN, false);
            yy += 30;
        }
        if (objectives.isEmpty()) g.drawCenteredString(font, "Todavia no agregaste nada", x + w / 2, y + 78, 0xFF71879A);
        if (objectives.size() > 14) g.drawString(font, "+ " + (objectives.size() - 14) + " mas", x + 10, yy, PINK, false);
    }

    private void renderMobInfo(GuiGraphics g, int x, int y, int w) {
        g.drawString(font, "MOBS HABILITADOS", x + 10, y, CYAN, false);
        g.drawString(font, "Seleccionados: " + ClientCatalogData.enabledMobs().size(), x + 10, y + 18, WHITE, false);
        g.drawWordWrap(font, Component.literal("Cuando la lista tiene al menos un mob, el servidor bloquea el spawn de otros Mob que no esten habilitados. Si la lista esta vacia, no restringe nada."), x + 10, y + 42, w - 20, 0xFF9CB3C7);
    }

    private void renderCraftEditor(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w) {
        g.drawString(font, "RECETA 3x3", x + 10, y, CYAN, false);
        int sx = x + Math.max(10, (w - 180) / 2);
        int sy = y + 24;
        int cell = 46;
        for (int i = 0; i < 9; i++) {
            int cx = sx + (i % 3) * cell;
            int cy = sy + (i / 3) * cell;
            int border = selectedCraftSlot == i ? CYAN : BORDER;
            g.fill(cx, cy, cx + 40, cy + 40, border);
            g.fill(cx + 2, cy + 2, cx + 38, cy + 38, 0xFF071018);
            Item item = itemFromId(craftSlots[i]);
            if (item != Items.AIR) g.renderItem(new ItemStack(item), cx + 12, cy + 11);
        }
        int rx = sx + 3 * cell + 18;
        int ry = sy + cell;
        g.drawString(font, "=>", sx + 3 * cell - 8, ry + 12, WHITE, false);
        g.fill(rx, ry, rx + 46, ry + 46, selectedCraftSlot == 9 ? PINK : BORDER);
        g.fill(rx + 2, ry + 2, rx + 44, ry + 44, 0xFF071018);
        Item result = itemFromId(craftResult);
        if (result != Items.AIR) g.renderItem(new ItemStack(result), rx + 15, ry + 14);
        g.drawWordWrap(font, Component.literal("1) Toca un slot. 2) Busca y toca un objeto. 3) Toca Guardar crafteo."), x + 10, sy + 156, w - 20, 0xFF9CB3C7);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int leftX = 24;
        int topY = 58;
        int leftW = Math.max(360, width - 350);
        int catalogY = topY + 28;
        int catalogH = height - topY - 78;

        if (isMobs()) {
            List<EntityType<?>> values = filteredEntities();
            int cols = Math.max(3, leftW / 120);
            int cellW = (leftW - 8) / cols;
            int cellH = 54;
            int rows = Math.max(1, catalogH / cellH);
            int perPage = Math.min(PAGE_SIZE, cols * rows);
            int start = Math.min(page * perPage, values.size());
            int end = Math.min(values.size(), start + perPage);
            for (int index = start; index < end; index++) {
                int local = index - start;
                int cx = leftX + 4 + (local % cols) * cellW;
                int cy = catalogY + 4 + (local / cols) * cellH;
                if (inside(mouseX, mouseY, cx, cy, cellW - 5, cellH - 5)) {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(values.get(index));
                    if (id != null) {
                        String text = id.toString();
                        boolean enable = !ClientCatalogData.isMobEnabled(text);
                        ClientCatalogData.setMobLocal(text, enable);
                        sendCommand("castrosafio mob " + (enable ? "habilitar " : "deshabilitar ") + text);
                    }
                    return true;
                }
            }
        } else {
            List<Item> values = filteredItems();
            int cols = Math.max(5, leftW / 72);
            int cell = Math.min(66, (leftW - 8) / cols);
            int rows = Math.max(1, catalogH / cell);
            int perPage = Math.min(PAGE_SIZE, cols * rows);
            int start = Math.min(page * perPage, values.size());
            int end = Math.min(values.size(), start + perPage);
            for (int index = start; index < end; index++) {
                int local = index - start;
                int cx = leftX + 4 + (local % cols) * cell;
                int cy = catalogY + 4 + (local / cols) * cell;
                if (inside(mouseX, mouseY, cx, cy, cell - 4, cell - 4)) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(values.get(index));
                    if (id != null) {
                        if (isMissions()) changeObjective(id.toString(), button == 0 ? 1 : -1);
                        else if (selectedCraftSlot == 9) craftResult = id.toString();
                        else craftSlots[selectedCraftSlot] = id.toString();
                    }
                    return true;
                }
            }
        }

        if (isCrafts()) {
            int panelRightX = leftX + leftW + 10;
            int panelRightW = width - panelRightX - 24;
            int sx = panelRightX + Math.max(10, (panelRightW - 180) / 2);
            int sy = topY + 36 + 24;
            int cell = 46;
            for (int i = 0; i < 9; i++) {
                int cx = sx + (i % 3) * cell;
                int cy = sy + (i / 3) * cell;
                if (inside(mouseX, mouseY, cx, cy, 40, 40)) {
                    if (button == 1) craftSlots[i] = ""; else selectedCraftSlot = i;
                    return true;
                }
            }
            int rx = sx + 3 * cell + 18;
            int ry = sy + cell;
            if (inside(mouseX, mouseY, rx, ry, 46, 46)) {
                if (button == 1) craftResult = ""; else selectedCraftSlot = 9;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void changeObjective(String id, int delta) {
        for (int i = 0; i < objectives.size(); i++) {
            MissionObjective objective = objectives.get(i);
            if (id.equals(objective.target)) {
                objective.amount += delta;
                if (objective.amount <= 0) objectives.remove(i);
                return;
            }
        }
        if (delta > 0) objectives.add(new MissionObjective("conseguir", id, 1));
    }

    private void saveMission() {
        sendCommand("castrosafio mision objetivos limpiar 1");
        sendCommand("castrosafio mision titulo 1 " + cleanText(titleBox == null ? "Nueva mision" : titleBox.getValue()));
        sendCommand("castrosafio mision descripcion 1 " + cleanText(descriptionBox == null ? "" : descriptionBox.getValue()));
        for (MissionObjective objective : objectives) {
            sendCommand("castrosafio mision objetivos agregar 1 " + objective.type + " " + objective.target + " " + objective.amount);
        }
        sendCommand("castrosafio mision activar 1");
    }

    private void saveCraft() {
        sendCommand("castrosafio crafteo limpiar 1");
        sendCommand("castrosafio crafteo nombre 1 " + cleanText(titleBox == null ? "Nuevo crafteo" : titleBox.getValue()));
        for (int i = 0; i < 9; i++) if (!craftSlots[i].isBlank()) sendCommand("castrosafio crafteo ingrediente 1 " + i + " " + craftSlots[i]);
        if (!craftResult.isBlank()) {
            sendCommand("castrosafio crafteo resultado 1 " + craftResult + " 1");
            sendCommand("castrosafio crafteo activar 1");
        }
    }

    private void clearCraftLocal() {
        for (int i = 0; i < 9; i++) craftSlots[i] = "";
        craftResult = "";
    }

    private void sendCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) mc.player.connection.sendCommand(command);
    }

    private List<Item> filteredItems() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String name = item.getDescription().getString().toLowerCase(Locale.ROOT);
            String idText = id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query) || idText.contains(query)) items.add(item);
        }
        items.sort(Comparator.comparing(item -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            return id == null ? "" : id.toString();
        }));
        return items;
    }

    private List<EntityType<?>> filteredEntities() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<EntityType<?>> values = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String name = type.getDescription().getString().toLowerCase(Locale.ROOT);
            String idText = id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query) || idText.contains(query)) values.add(type);
        }
        values.sort(Comparator.comparing(type -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return id == null ? "" : id.toString();
        }));
        return values;
    }

    private static Item itemFromId(String id) {
        if (id == null || id.isBlank()) return Items.AIR;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return Items.AIR;
        return BuiltInRegistries.ITEM.get(key);
    }

    private static String displayItem(String id) {
        Item item = itemFromId(id);
        return item == Items.AIR ? id : item.getDescription().getString();
    }

    private static String cleanText(String text) {
        if (text == null || text.isBlank()) return "Sin texto";
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean isMissions() { return mode.startsWith("mission"); }
    private boolean isMobs() { return mode.startsWith("mob"); }
    private boolean isCrafts() { return mode.startsWith("craft"); }
}
