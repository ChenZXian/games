package com.android.boot.system;

import com.android.boot.entity.Recipe;

public class OrderManager {
    public String customerName = "Mia";
    public float patience = 90f;

    public void reset(Recipe recipe) {
        customerName = recipe.style.equals("Bake") ? "Luna" : recipe.style.equals("Soup") ? "Noah" : "Mia";
        patience = 90f;
    }

    public void update(float delta) {
        patience -= delta * 4f;
        if (patience < 0f) patience = 0f;
    }
}
