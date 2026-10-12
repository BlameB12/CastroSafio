package com.castrosafio.menu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientCatalogData {
    private static final Gson GSON = new Gson();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
    private static final Type CRAFT_LIST_TYPE = new TypeToken<List<CraftDefinition>>() {}.getType();
    private static final Set<String> ENABLED_MOBS = new LinkedHashSet<>();
    private static final Map<Integer, CraftDefinition> CRAFTS = new HashMap<>();

    private ClientCatalogData() {}

    public static void update(String mobsJson, String craftsJson) {
        ENABLED_MOBS.clear();
        Set<String> mobs = GSON.fromJson(mobsJson, SET_TYPE);
        if (mobs != null) ENABLED_MOBS.addAll(mobs);
        CRAFTS.clear();
        List<CraftDefinition> crafts = GSON.fromJson(craftsJson, CRAFT_LIST_TYPE);
        if (crafts != null) for (CraftDefinition craft : crafts) if (craft != null) CRAFTS.put(craft.id, craft);
    }

    public static Set<String> enabledMobs() { return new LinkedHashSet<>(ENABLED_MOBS); }
    public static boolean isMobEnabled(String id) { return ENABLED_MOBS.contains(id); }
    public static void setMobLocal(String id, boolean enabled) { if (enabled) ENABLED_MOBS.add(id); else ENABLED_MOBS.remove(id); }
    public static CraftDefinition craft(int id) { return CRAFTS.get(id); }
    public static List<CraftDefinition> activeCrafts() {
        List<CraftDefinition> list = new ArrayList<>();
        for (CraftDefinition c : CRAFTS.values()) if (c.active && c.result != null && !c.result.isBlank()) list.add(c);
        list.sort((a, b) -> Integer.compare(a.id, b.id));
        return list;
    }

    public static void openEditor(String mode) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new AdminEditorScreen(mode)));
    }
}
