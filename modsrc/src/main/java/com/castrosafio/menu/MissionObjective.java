package com.castrosafio.menu;

public final class MissionObjective {
    public String type;
    public String target;
    public int amount;

    public MissionObjective() {
        this("conseguir", "minecraft:air", 1);
    }

    public MissionObjective(String type, String target, int amount) {
        this.type = type == null || type.isBlank() ? "conseguir" : type;
        this.target = target == null ? "" : target;
        this.amount = Math.max(1, amount);
    }

    public String summary() {
        String targetText = target == null ? "" : target;
        return type.toUpperCase() + " " + amount + (targetText.isBlank() ? "" : " x " + targetText);
    }
}
