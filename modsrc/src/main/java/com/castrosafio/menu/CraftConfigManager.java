package com.castrosafio.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CraftConfigManager {
    public static final int MAX_CRAFTS = 32;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<CraftDefinition>>() {}.getType();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("castrosafio_crafts.json");
    private static final List<CraftDefinition> CRAFTS = new ArrayList<>();
    private static boolean loaded;

    private CraftConfigManager() {}

    public static synchronized void ensureLoaded() { if (!loaded) load(); }

    public static synchronized void load() {
        CRAFTS.clear();
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                List<CraftDefinition> list = GSON.fromJson(reader, LIST_TYPE);
                if (list != null) CRAFTS.addAll(list);
            } catch (Exception e) {
                System.err.println("[CastroSafio] No se pudo leer crafteos: " + e.getMessage());
            }
        }
        for (int id = 1; id <= MAX_CRAFTS; id++) {
            final int craftId = id;
            if (CRAFTS.stream().noneMatch(c -> c != null && c.id == craftId)) CRAFTS.add(new CraftDefinition(id));
        }
        CRAFTS.removeIf(c -> c == null || c.id < 1 || c.id > MAX_CRAFTS);
        CRAFTS.sort(Comparator.comparingInt(c -> c.id));
        for (CraftDefinition c : CRAFTS) {
            if (c.name == null || c.name.isBlank()) c.name = "Crafteo " + c.id;
            if (c.ingredients == null) c.ingredients = new ArrayList<>();
            while (c.ingredients.size() < 9) c.ingredients.add("");
            while (c.ingredients.size() > 9) c.ingredients.remove(c.ingredients.size() - 1);
            for (int i = 0; i < c.ingredients.size(); i++) if (c.ingredients.get(i) == null) c.ingredients.set(i, "");
            if (c.result == null) c.result = "";
            if (c.resultAmount < 1) c.resultAmount = 1;
        }
        loaded = true;
        save();
    }

    public static synchronized CraftDefinition get(int id) {
        ensureLoaded();
        return CRAFTS.stream().filter(c -> c.id == id).findFirst().orElse(null);
    }

    public static synchronized List<CraftDefinition> all() {
        ensureLoaded();
        return new ArrayList<>(CRAFTS);
    }

    public static synchronized void save() {
        if (!loaded) return;
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { GSON.toJson(CRAFTS, LIST_TYPE, writer); }
        } catch (Exception e) {
            System.err.println("[CastroSafio] No se pudo guardar crafteos: " + e.getMessage());
        }
    }

    public static synchronized String toJson() {
        ensureLoaded();
        return GSON.toJson(CRAFTS, LIST_TYPE);
    }
}
