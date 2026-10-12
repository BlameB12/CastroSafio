package com.castrosafio.menu;

import java.util.ArrayList;
import java.util.List;

public final class MissionDefinition {
    public int id;
    public String title;
    public String description;
    public String objectiveType;
    public String target;
    public int amount;
    public boolean unlocked;
    public boolean active;
    public List<MissionObjective> objectives;

    public MissionDefinition() {
    }

    public MissionDefinition(int id) {
        this.id = id;
        this.title = "Mision " + id;
        this.description = "Mision aun sin configurar";
        this.objectiveType = "ninguno";
        this.target = "";
        this.amount = 1;
        this.unlocked = false;
        this.active = false;
        this.objectives = new ArrayList<>();
    }

    public String objectiveSummary() {
        if (objectives != null && !objectives.isEmpty()) {
            return objectives.size() + " objetivos secundarios";
        }
        if (objectiveType == null || objectiveType.isBlank() || "ninguno".equalsIgnoreCase(objectiveType)) {
            return "Sin objetivos configurados";
        }
        String t = target == null ? "" : target;
        return objectiveType.toUpperCase() + " " + amount + (t.isBlank() ? "" : " x " + t);
    }
}
