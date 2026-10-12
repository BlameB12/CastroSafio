package com.castrosafio.menu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MissionClientData {
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<MissionDefinition>>() {}.getType();
    private static final Map<Integer, MissionDefinition> MISSIONS = new HashMap<>();

    private MissionClientData() {}

    public static void updateFromJson(String json) {
        List<MissionDefinition> list = GSON.fromJson(json, LIST_TYPE);
        MISSIONS.clear();
        if (list != null) {
            for (MissionDefinition mission : list) {
                if (mission != null) MISSIONS.put(mission.id, mission);
            }
        }
    }

    public static MissionDefinition get(int id) {
        return MISSIONS.get(id);
    }

    public static MissionDefinition active() {
        return MISSIONS.values().stream().filter(m -> m.active).min(Comparator.comparingInt(m -> m.id)).orElse(null);
    }

    public static List<MissionDefinition> all() {
        List<MissionDefinition> list = new ArrayList<>(MISSIONS.values());
        list.sort(Comparator.comparingInt(m -> m.id));
        return list;
    }
}
