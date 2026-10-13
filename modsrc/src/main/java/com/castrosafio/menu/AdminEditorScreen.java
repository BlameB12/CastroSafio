package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AdminEditorScreen extends Screen {
    private static final int PANEL = 0xF20A1119;
    private static final int PANEL_2 = 0xFF111E2A;
    private static final int BORDER = 0xFF7F8FA3;
    private static final int CYAN = 0xFF35E7FF;
    private static final int PINK = 0xFFFF4FD8;
    private static final int WHITE = 0xFFF5F8FF;
    private static final int GREEN = 0xFF5BFF86;
    private static final int PAGE_SIZE = 40;

    private final String mode;
    private EditBox searchBox;
    private EditBox titleBox;
    private EditBox descriptionBox;
    private EditBox mobInfoBox;
    private int page;
    private final List<MissionObjective> objectives = new ArrayList<>();

    private String craftResult = "";
    private String craftStatus = "Selecciona el objeto que quieres publicar.";
    private int craftPublishWait = -1;

    private String selectedMobId = "";
    private String mobStatus = "Selecciona un mob para editar su informacion.";
    private final Map<ResourceLocation, LivingEntity> mobPreviews = new HashMap<>();
    private final Set<ResourceLocation> invalidMobPreviews = new HashSet<>();
    private List<EntityType<?>> livingCatalog;

    public AdminEditorScreen(String mode) {
        super(Component.literal("CastroSafio Editor"));
        this.mode = mode == null ? "missions" : mode.toLowerCase(Locale.ROOT);
    }

    @Override
    protected void init() {
        int top = 18;
        searchBox = new EditBox(font, 24, top + 28, Math.max(180, width / 3), 20, Component.literal("Buscar"));
        searchBox.setHint(Component.literal("Buscar por nombre o mod..."));
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                .bounds(width - 104, top + 28, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page++)
                .bounds(width - 66, top + 28, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(width - 92, height - 30, 68, 20).build());

        if (isMissions()) {
            MissionDefinition mission = MissionClientData.active();
            if (mission == null) mission = MissionClientData.get(1);

            titleBox = new EditBox(font, width - 300, 62, 276, 20, Component.literal("Titulo"));
            descriptionBox = new EditBox(font, width - 300, 88, 276, 20, Component.literal("Descripcion"));
            if (mission != null) {
                titleBox.setValue(mission.title == null ? "" : mission.title);
                descriptionBox.setValue(mission.description == null ? "" : mission.description);
                if (mission.objectives != null) {
                    for (MissionObjective objective : mission.objectives) {
                        objectives.add(new MissionObjective(objective.type, objective.target, objective.amount));
                    }
                }
            } else {
                titleBox.setValue("Nueva mision");
            }
            addRenderableWidget(titleBox);
            addRenderableWidget(descriptionBox);
            addRenderableWidget(Button.builder(Component.literal("Guardar y activar"), b -> saveMission())
                    .bounds(width - 300, height - 56, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Vaciar objetivos"), b -> objectives.clear())
                    .bounds(width - 154, height - 56, 130, 20).build());
        } else if (isMobs()) {
            if (!ClientCatalogData.enabledMobs().isEmpty()) {
                selectedMobId = ClientCatalogData.enabledMobs().iterator().next();
            }
            mobInfoBox = new EditBox(font, width - 300, 150, 276, 20, Component.literal("Info del mob"));
            mobInfoBox.setHint(Component.literal("Ej: Jefe disponible desde el dia 5"));
            mobInfoBox.setMaxLength(180);
            if (!selectedMobId.isBlank()) mobInfoBox.setValue(ClientCatalogData.mobInfo(selectedMobId));
            addRenderableWidget(mobInfoBox);

            addRenderableWidget(Button.builder(Component.literal("Guardar info"), b -> saveMobInfo())
                    .bounds(width - 300, height - 56, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Vaciar lista"), b -> {
                sendCommand("castrosafio mob limpiar");
                for (String id : ClientCatalogData.enabledMobs()) ClientCatalogData.setMobLocal(id, false);
                mobStatus = "Lista de mobs habilitados vaciada.";
            }).bounds(width - 154, height - 56, 130, 20).build());
        } else if (isCrafts()) {
            addRenderableWidget(Button.builder(Component.literal("GUARDAR"), b -> saveCraft())
                    .bounds(width - 300, height - 56, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Deseleccionar"), b -> {
                craftResult = "";
                craftPublishWait = -1;
                craftStatus = "Selecciona el objeto que quieres publicar.";
            }).bounds(width - 154, height - 56, 130, 20).build());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isCrafts() || craftPublishWait < 0 || craftResult.isBlank()) return;

        for (CraftDefinition craft : ClientCatalogData.activeCrafts()) {
            if (craftResult.equals(craft.result)) {
                craftStatus = "PUBLICADO correctamente.";
                craftPublishWait = -1;
                return;
            }
        }

        craftPublishWait++;
        if (craftPublishWait > 100) {
            craftStatus = "No se pudo publicar. Mira el chat para ver el motivo.";
            craftPublishWait = -1;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // La interfaz ya pinta un fondo opaco; evitar el blur vanilla encima del editor.
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

        if (isMobs()) {
            renderMobCatalog(g, mouseX, mouseY, leftX, topY + 28, leftW, height - topY - 78);
        } else {
            renderItemCatalog(g, mouseX, mouseY, leftX, topY + 28, leftW, height - topY - 78);
        }

        if (isMissions()) renderMissionEditor(g, panelRightX, topY + 58, panelRightW);
        else if (isMobs()) renderMobInfo(g, panelRightX, topY + 12, panelRightW, mouseX, mouseY);
        else renderCraftEditor(g, panelRightX, topY + 36, panelRightW);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private String titleForMode() {
        if (isMissions()) return "EDITOR DE MISIONES - selecciona objetos para objetivos secundarios";
        if (isMobs()) return "EDITOR DE MOBS - modelos 3D + informacion configurable";
        return "EDITOR DE CRAFTEOS - elige el resultado y pulsa GUARDAR";
    }

    private void renderItemCatalog(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h) {
        List<Item> values = filteredItems();
        int cols = Math.max(5, w / 72);
        int cell = Math.min(66, (w - 8) / cols);
        int rows = Math.max(1, h / cell);
        int perPage = Math.min(PAGE_SIZE, cols * rows);
        int start = Math.min(page * perPage, Math.max(0, values.size()));
        int end = Math.min(values.size(), start + perPage);
        if (start >= values.size() && page > 0) {
            page--;
            return;
        }

        for (int index = start; index < end; index++) {
            int local = index - start;
            int cx = x + 4 + (local % cols) * cell;
            int cy = y + 4 + (local / cols) * cell;
            Item item = values.get(index);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            boolean selected = isCrafts() && id != null && id.toString().equals(craftResult);
            boolean hover = inside(mouseX, mouseY, cx, cy, cell - 4, cell - 4);

            g.fill(cx, cy, cx + cell - 4, cy + cell - 4,
                    selected ? PINK : (hover ? CYAN : 0xFF132736));
            g.fill(cx + 2, cy + 2, cx + cell - 6, cy + cell - 6, 0xFF071018);
            g.renderItem(new ItemStack(item), cx + (cell - 20) / 2, cy + 7);
            String name = item.getDescription().getString();
            g.drawCenteredString(font, trim(name, Math.max(5, (cell - 8) / 6)),
                    cx + (cell - 4) / 2, cy + cell - 18, selected ? PINK : WHITE);
        }
        g.drawString(font, "Pagina " + (page + 1) + " | " + values.size() + " objetos",
                x + 6, y + h + 5, 0xFF9CB3C7, false);
    }

    private void renderMobCatalog(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h) {
        List<EntityType<?>> values = filteredEntities();
        int cols = Math.max(3, w / 112);
        int cellW = (w - 8) / cols;
        int cellH = 108;
        int rows = Math.max(1, h / cellH);
        int perPage = Math.min(PAGE_SIZE, cols * rows);
        int start = Math.min(page * perPage, Math.max(0, values.size()));
        int end = Math.min(values.size(), start + perPage);
        if (start >= values.size() && page > 0) {
            page--;
            return;
        }

        for (int index = start; index < end; index++) {
            int local = index - start;
            int cx = x + 4 + (local % cols) * cellW;
            int cy = y + 4 + (local / cols) * cellH;
            int cardW = cellW - 5;
            int cardH = cellH - 5;
            EntityType<?> type = values.get(index);
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String idText = id == null ? "?" : id.toString();
            boolean enabled = ClientCatalogData.isMobEnabled(idText);
            boolean selected = idText.equals(selectedMobId);
            boolean hover = inside(mouseX, mouseY, cx, cy, cardW, cardH);

            int border = selected ? PINK : (enabled ? GREEN : (hover ? CYAN : 0xFF354554));
            g.fill(cx, cy, cx + cardW, cy + cardH, border);
            g.fill(cx + 2, cy + 2, cx + cardW - 2, cy + cardH - 2, 0xFF071018);

            LivingEntity preview = previewEntity(type);
            if (preview != null) {
                float biggest = Math.max(0.65F, Math.max(preview.getBbWidth(), preview.getBbHeight()));
                int scale = Math.max(6, Math.min(28, (int) (38.0F / biggest)));
                try {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g,
                            cx + 4, cy + 4,
                            cx + cardW - 4, cy + cardH - 29,
                            scale,
                            0.0625F,
                            (float) mouseX,
                            (float) mouseY,
                            preview
                    );
                } catch (Throwable ignored) {
                    g.drawCenteredString(font, "3D", cx + cardW / 2, cy + 38, 0xFF71879A);
                }
            }

            String name = type.getDescription().getString();
            g.drawCenteredString(font, trim(name, Math.max(8, cardW / 6)),
                    cx + cardW / 2, cy + cardH - 24, selected ? PINK : (enabled ? GREEN : WHITE));
            g.drawCenteredString(font, enabled ? "HABILITADO" : "Click izq: habilitar",
                    cx + cardW / 2, cy + cardH - 12, enabled ? GREEN : 0xFF8BA5B8);
        }

        g.drawString(font, "Pagina " + (page + 1) + " | " + values.size() + " mobs 3D",
                x + 6, y + h + 5, 0xFF9CB3C7, false);
    }

    private void renderMissionEditor(GuiGraphics g, int x, int y, int w) {
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
        if (objectives.isEmpty()) {
            g.drawCenteredString(font, "Todavia no agregaste nada", x + w / 2, y + 78, 0xFF71879A);
        }
        if (objectives.size() > 14) g.drawString(font, "+ " + (objectives.size() - 14) + " mas", x + 10, yy, PINK, false);
    }

    private void renderMobInfo(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.drawString(font, "MOB SELECCIONADO", x + 10, y, CYAN, false);
        g.drawString(font, "Habilitados: " + ClientCatalogData.enabledMobs().size(), x + 10, y + 18, WHITE, false);

        if (selectedMobId.isBlank()) {
            g.drawWordWrap(font, Component.literal(
                            "Selecciona un mob. Click izquierdo lo habilita/deshabilita; click derecho solo lo selecciona para editar su informacion."),
                    x + 10, y + 44, w - 20, 0xFF9CB3C7);
            return;
        }

        EntityType<?> type = entityTypeFromId(selectedMobId);
        String name = type == null ? selectedMobId : type.getDescription().getString();
        g.drawString(font, trim(name, 32), x + 10, y + 48, PINK, false);
        g.drawString(font, trim(selectedMobId, 38), x + 10, y + 64, 0xFF8BA5B8, false);

        if (type != null) {
            LivingEntity preview = previewEntity(type);
            if (preview != null) {
                float biggest = Math.max(0.65F, Math.max(preview.getBbWidth(), preview.getBbHeight()));
                int scale = Math.max(8, Math.min(42, (int) (52.0F / biggest)));
                try {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g,
                            x + 18, y + 82,
                            x + w - 18, y + 210,
                            scale,
                            0.0625F,
                            (float) mouseX,
                            (float) mouseY,
                            preview
                    );
                } catch (Throwable ignored) {
                }
            }
        }

        g.drawString(font, "INFO QUE VERAN LOS JUGADORES", x + 10, y + 220, CYAN, false);
        String current = ClientCatalogData.mobInfo(selectedMobId);
        g.drawWordWrap(font, Component.literal(current.isBlank() ? "Sin informacion configurada." : current),
                x + 10, y + 238, w - 20, current.isBlank() ? 0xFF71879A : WHITE);
        g.drawWordWrap(font, Component.literal("Escribe arriba en el campo de texto y pulsa Guardar info."),
                x + 10, y + 292, w - 20, 0xFF9CB3C7);
        g.drawWordWrap(font, Component.literal(mobStatus), x + 10, y + 326, w - 20, GREEN);
    }

    private void renderCraftEditor(GuiGraphics g, int x, int y, int w) {
        g.drawString(font, "PUBLICAR CRAFTEO", x + 10, y, CYAN, false);
        g.drawWordWrap(font, Component.literal(
                        "1) Busca el objeto que quieres obtener. 2) Haz click sobre el. 3) Pulsa GUARDAR. CastroSafio detecta su receta real, la guarda, la activa y la envia a todos."),
                x + 10, y + 20, w - 20, 0xFF9CB3C7);

        int boxX = x + Math.max(10, (w - 82) / 2);
        int boxY = y + 92;
        g.fill(boxX, boxY, boxX + 82, boxY + 82, craftResult.isBlank() ? BORDER : PINK);
        g.fill(boxX + 3, boxY + 3, boxX + 79, boxY + 79, 0xFF071018);

        Item result = itemFromId(craftResult);
        if (result != Items.AIR) {
            g.renderItem(new ItemStack(result), boxX + 33, boxY + 26);
            g.drawCenteredString(font, trim(result.getDescription().getString(), 25),
                    x + w / 2, boxY + 58, WHITE);
        } else {
            g.drawCenteredString(font, "ELIGE UN OBJETO", x + w / 2, boxY + 35, 0xFF71879A);
        }

        g.drawCenteredString(font, craftStatus, x + w / 2, boxY + 94,
                craftResult.isBlank() ? 0xFF9CB3C7 : GREEN);
        g.drawCenteredString(font, "Crafteos publicados: " + ClientCatalogData.activeCrafts().size(),
                x + w / 2, boxY + 114, CYAN);
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
            int cols = Math.max(3, leftW / 112);
            int cellW = (leftW - 8) / cols;
            int cellH = 108;
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
                        selectedMobId = text;
                        if (mobInfoBox != null) mobInfoBox.setValue(ClientCatalogData.mobInfo(text));
                        mobStatus = "Editando " + values.get(index).getDescription().getString();

                        if (button == 0) {
                            boolean enable = !ClientCatalogData.isMobEnabled(text);
                            ClientCatalogData.setMobLocal(text, enable);
                            sendCommand("castrosafio mob " + (enable ? "habilitar " : "deshabilitar ") + text);
                        }
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
                        if (isMissions()) {
                            changeObjective(id.toString(), button == 0 ? 1 : -1);
                        } else if (isCrafts() && button == 0) {
                            craftResult = id.toString();
                            craftPublishWait = -1;
                            craftStatus = "Seleccionado: " + values.get(index).getDescription().getString();
                        }
                    }
                    return true;
                }
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

    private void saveMobInfo() {
        if (selectedMobId.isBlank()) {
            mobStatus = "Primero selecciona un mob.";
            return;
        }
        String text = mobInfoBox == null ? "" : mobInfoBox.getValue().trim();
        ClientCatalogData.setMobInfoLocal(selectedMobId, text);
        sendCommand("castrosafio_mobinfo " + selectedMobId + " " + (text.isBlank() ? "_" : cleanText(text)));
        mobStatus = text.isBlank() ? "Informacion eliminada." : "Informacion guardada y sincronizada.";
    }

    private void saveCraft() {
        if (craftResult.isBlank()) {
            craftStatus = "Primero selecciona un objeto.";
            return;
        }
        craftPublishWait = 0;
        craftStatus = "Detectando receta y publicando...";
        sendCommand("castrosafio crafteo autodetectar " + craftResult);
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
        if (livingCatalog == null) buildLivingCatalog();
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<EntityType<?>> values = new ArrayList<>();
        for (EntityType<?> type : livingCatalog) {
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

    private void buildLivingCatalog() {
        livingCatalog = new ArrayList<>();
        if (minecraft == null || minecraft.level == null) return;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (previewEntity(type) != null) livingCatalog.add(type);
        }
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
