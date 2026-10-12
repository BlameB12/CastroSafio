package com.castrosafio.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MissionManager {
    public static final int MAX_MISSIONS = 65;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<MissionDefinition>>() {}.getType();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("castrosafio_missions.json");
    private static final List<MissionDefinition> MISSIONS = new ArrayList<>();
    private static boolean loaded;

    private MissionManager() {
    }

    public static synchronized void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static synchronized void load() {
        MISSIONS.clear();

        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                List<MissionDefinition> loadedMissions = GSON.fromJson(reader, LIST_TYPE);
                if (loadedMissions != null) {
                    MISSIONS.addAll(loadedMissions);
                }
            } catch (Exception e) {
                System.err.println("[CastroSafio] No se pudo leer " + FILE + ": " + e.getMessage());
            }
        }

        for (int id = 1; id <= MAX_MISSIONS; id++) {
            final int missionId = id;
            boolean exists = MISSIONS.stream().anyMatch(m -> m != null && m.id == missionId);
            if (!exists) {
                MISSIONS.add(new MissionDefinition(id));
            }
        }

        MISSIONS.removeIf(m -> m == null || m.id < 1 || m.id > MAX_MISSIONS);
        MISSIONS.sort(Comparator.comparingInt(m -> m.id));
        sanitize();
        loaded = true;
        save();
    }

    private static void sanitize() {
        for (MissionDefinition mission : MISSIONS) {
            if (mission.title == null || mission.title.isBlank()) {
                mission.title = "Mision " + mission.id;
            }
            if (mission.description == null) {
                mission.description = "";
            }
            if (mission.objectiveType == null || mission.objectiveType.isBlank()) {
                mission.objectiveType = "ninguno";
            }
            if (mission.target == null) {
                mission.target = "";
            }
            if (mission.amount < 1) {
                mission.amount = 1;
            }
        }
    }

    public static synchronized MissionDefinition get(int id) {
        ensureLoaded();
        for (MissionDefinition mission : MISSIONS) {
            if (mission.id == id) {
                return mission;
            }
        }
        return null;
    }

    public static synchronized List<MissionDefinition> all() {
        ensureLoaded();
        return new ArrayList<>(MISSIONS);
    }

    public static synchronized void save() {
        if (!loaded) {
            return;
        }

        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(MISSIONS, LIST_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[CastroSafio] No se pudo guardar " + FILE + ": " + e.getMessage());
        }
    }

    public static synchronized String toJson() {
        ensureLoaded();
        return GSON.toJson(MISSIONS, LIST_TYPE);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new MissionSyncPayload(toJson()));
    }

    public static void syncAll() {
        PacketDistributor.sendToAllPlayers(new MissionSyncPayload(toJson()));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
