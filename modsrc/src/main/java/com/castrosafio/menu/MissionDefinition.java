package com.castrosafio.menu;

public final class MissionDefinition {
    public int id;
    public String title;
    public String description;
    public String objectiveType;
    public String target;
    public int amount;
    public boolean unlocked;

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
    }

    public String objectiveSummary() {
        if (objectiveType == null || objectiveType.isBlank() || "ninguno".equalsIgnoreCase(objectiveType)) {
            return "Sin objetivo configurado";
        }
        String t = target == null ? "" : target;
        return objectiveType.toUpperCase() + " " + amount + (t.isBlank() ? "" : " x " + t);
    }
}
