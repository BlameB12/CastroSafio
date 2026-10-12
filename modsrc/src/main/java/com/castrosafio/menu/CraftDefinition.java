package com.castrosafio.menu;

import java.util.ArrayList;
import java.util.List;

public final class CraftDefinition {
    public int id;
    public String name;
    public List<String> ingredients;
    public String result;
    public int resultAmount;
    public boolean active;

    public CraftDefinition() {}

    public CraftDefinition(int id) {
        this.id = id;
        this.name = "Crafteo " + id;
        this.ingredients = new ArrayList<>();
        for (int i = 0; i < 9; i++) ingredients.add("");
        this.result = "";
        this.resultAmount = 1;
        this.active = false;
    }
}
