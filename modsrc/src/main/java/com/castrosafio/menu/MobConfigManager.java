package com.castrosafio.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MobConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("castrosafio_mobs.json");
    private static final Set<String> ENABLED = new LinkedHashSet<>();
    private static boolean loaded;

    private MobConfigManager() {}

    public static synchronized void ensureLoaded() { if (!loaded) load(); }

    public static synchronized void load() {
        ENABLED.clear();
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                Set<String> values = GSON.fromJson(reader, SET_TYPE);
                if (values != null) ENABLED.addAll(values);
            } catch (Exception e) {
                System.err.println("[CastroSafio] No se pudo leer mobs: " + e.getMessage());
            }
        }
        ENABLED.removeIf(s -> s == null || s.isBlank());
        loaded = true;
        save();
    }

    public static synchronized boolean isEnabled(String id) { ensureLoaded(); return ENABLED.contains(id); }
    public static synchronized boolean hasRestrictions() { ensureLoaded(); return !ENABLED.isEmpty(); }
    public static synchronized Set<String> all() { ensureLoaded(); return new LinkedHashSet<>(ENABLED); }

    public static synchronized void setEnabled(String id, boolean enabled) {
        ensureLoaded();
        if (enabled) ENABLED.add(id); else ENABLED.remove(id);
        save();
    }

    public static synchronized void clear() { ensureLoaded(); ENABLED.clear(); save(); }

    public static synchronized void save() {
        if (!loaded) return;
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { GSON.toJson(ENABLED, SET_TYPE, writer); }
        } catch (Exception e) {
            System.err.println("[CastroSafio] No se pudo guardar mobs: " + e.getMessage());
        }
    }

    public static synchronized String toJson() { ensureLoaded(); return GSON.toJson(ENABLED, SET_TYPE); }

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
