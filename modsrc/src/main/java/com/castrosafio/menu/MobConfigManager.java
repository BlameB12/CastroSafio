package com.castrosafio.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class MobConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("castrosafio_mobs.json");
    private static final Set<String> ENABLED = new LinkedHashSet<>();
    private static final Map<String, String> INFO = new LinkedHashMap<>();
    private static boolean loaded;

    private MobConfigManager() {}

    private static final class StoredData {
        Set<String> enabled = new LinkedHashSet<>();
        Map<String, String> info = new LinkedHashMap<>();
    }

    public static synchronized void ensureLoaded() {
        if (!loaded) load();
    }

    public static synchronized void load() {
        ENABLED.clear();
        INFO.clear();
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root != null && root.isJsonArray()) {
                    // Compatibilidad con la configuracion anterior: ["minecraft:zombie", ...]
                    Set<String> values = GSON.fromJson(root, SET_TYPE);
                    if (values != null) ENABLED.addAll(values);
                } else if (root != null && root.isJsonObject()) {
                    StoredData data = GSON.fromJson(root, StoredData.class);
                    if (data != null) {
                        if (data.enabled != null) ENABLED.addAll(data.enabled);
                        if (data.info != null) INFO.putAll(data.info);
                    }
                }
            } catch (Exception e) {
                System.err.println("[CastroSafio] No se pudo leer mobs: " + e.getMessage());
            }
        }

        ENABLED.removeIf(s -> s == null || s.isBlank());
        INFO.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isBlank());
        INFO.replaceAll((id, text) -> text == null ? "" : text.trim());
        loaded = true;
        save();
    }

    public static synchronized boolean isEnabled(String id) {
        ensureLoaded();
        return ENABLED.contains(id);
    }

    public static synchronized boolean hasRestrictions() {
        ensureLoaded();
        return !ENABLED.isEmpty();
    }

    public static synchronized Set<String> all() {
        ensureLoaded();
        return new LinkedHashSet<>(ENABLED);
    }

    public static synchronized String info(String id) {
        ensureLoaded();
        return INFO.getOrDefault(id, "");
    }

    public static synchronized void setEnabled(String id, boolean enabled) {
        ensureLoaded();
        if (enabled) ENABLED.add(id);
        else ENABLED.remove(id);
        save();
    }

    public static synchronized void setInfo(String id, String text) {
        ensureLoaded();
        if (id == null || id.isBlank()) return;
        String value = text == null ? "" : text.trim();
        if (value.isBlank()) INFO.remove(id);
        else INFO.put(id, value);
        save();
    }

    public static synchronized void clear() {
        ensureLoaded();
        ENABLED.clear();
        save();
    }

    public static synchronized void save() {
        if (!loaded) return;
        try {
            Files.createDirectories(FILE.getParent());
            StoredData data = new StoredData();
            data.enabled.addAll(ENABLED);
            data.info.putAll(INFO);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            System.err.println("[CastroSafio] No se pudo guardar mobs: " + e.getMessage());
        }
    }

    public static synchronized String toJson() {
        ensureLoaded();
        StoredData data = new StoredData();
        data.enabled.addAll(ENABLED);
        data.info.putAll(INFO);
        return GSON.toJson(data);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CatalogSyncPayload(toJson(), CraftConfigManager.toJson()));
    }

    public static void syncAll() {
        PacketDistributor.sendToAllPlayers(new CatalogSyncPayload(toJson(), CraftConfigManager.toJson()));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob)) return;
        ensureLoaded();
        if (ENABLED.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (id != null && !ENABLED.contains(id.toString())) event.setCanceled(true);
    }
}
