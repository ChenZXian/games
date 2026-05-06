package com.android.boot.system;

import com.android.boot.entity.Ingredient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IngredientSystem {
    private final HashMap<String, Ingredient> data = new HashMap<>();

    public IngredientSystem() {
        add(new Ingredient("tomato", "Tomato", "Vegetable", true, true, 8));
        add(new Ingredient("beef", "Beef", "Meat", true, true, 10));
        add(new Ingredient("shrimp", "Shrimp", "Seafood", false, true, 9));
        add(new Ingredient("rice", "Rice", "Staple", false, true, 7));
        add(new Ingredient("noodle", "Noodle", "Staple", false, true, 7));
        add(new Ingredient("salt", "Salt", "Seasoning", false, false, 4));
        add(new Ingredient("basil", "Basil", "Decoration", true, false, 6));
        add(new Ingredient("cream", "Cream", "Dessert", false, false, 8));
        add(new Ingredient("flour", "Flour", "Dessert", false, true, 8));
        add(new Ingredient("egg", "Egg", "Dessert", false, true, 8));
    }

    private void add(Ingredient item) {
        data.put(item.id, item);
    }

    public Ingredient get(String id) {
        return data.get(id);
    }

    public List<Ingredient> all() {
        return new ArrayList<>(data.values());
    }
}
