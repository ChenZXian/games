package com.android.boot.entity;

import java.util.List;

public class Recipe {
    public final String id;
    public final String name;
    public final String style;
    public final List<String> ingredients;
    public final List<CookingStep> steps;
    public final int reward;
    public final String finishLook;

    public Recipe(String id, String name, String style, List<String> ingredients, List<CookingStep> steps, int reward, String finishLook) {
        this.id = id;
        this.name = name;
        this.style = style;
        this.ingredients = ingredients;
        this.steps = steps;
        this.reward = reward;
        this.finishLook = finishLook;
    }
}
