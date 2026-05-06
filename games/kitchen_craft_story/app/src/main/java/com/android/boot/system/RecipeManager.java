package com.android.boot.system;

import com.android.boot.entity.CookingStep;
import com.android.boot.entity.Recipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipeManager {
    private final List<Recipe> recipes = new ArrayList<>();

    public RecipeManager() {
        recipes.add(new Recipe("stir_rice", "Sunny Fried Rice", "Stir", Arrays.asList("rice", "egg", "tomato", "salt"), Arrays.asList(new CookingStep("wash", "sink", 2, 0.2f), new CookingStep("cut", "knife", 4, 0.2f), new CookingStep("stir", "pan", 6, 0.7f), new CookingStep("plate", "plate", 2, 0.3f), new CookingStep("decorate", "basil", 2, 0.1f)), 120, "Golden rice with bright tomato and basil"));
        recipes.add(new Recipe("seafood_soup", "Cozy Seafood Soup", "Soup", Arrays.asList("shrimp", "tomato", "salt", "basil"), Arrays.asList(new CookingStep("wash", "sink", 3, 0.2f), new CookingStep("boil", "pot", 5, 0.8f), new CookingStep("season", "bottle", 2, 0.5f), new CookingStep("plate", "plate", 2, 0.2f)), 140, "Warm soup with soft seafood colors"));
        recipes.add(new Recipe("beef_noodle", "Happy Beef Noodle", "Noodle", Arrays.asList("beef", "noodle", "salt", "basil"), Arrays.asList(new CookingStep("cut", "knife", 5, 0.2f), new CookingStep("boil", "pot", 5, 0.8f), new CookingStep("stir", "pan", 4, 0.7f), new CookingStep("plate", "plate", 2, 0.3f)), 160, "Noodles topped with tender beef"));
        recipes.add(new Recipe("berry_cake", "Cloud Cream Cake", "Bake", Arrays.asList("flour", "egg", "cream"), Arrays.asList(new CookingStep("mix", "bowl", 7, 0.2f), new CookingStep("bake", "oven", 5, 0.75f), new CookingStep("decorate", "cream", 4, 0.1f)), 180, "Soft cake with glossy cream"));
    }

    public List<Recipe> all() {
        return recipes;
    }

    public Recipe get(int index) {
        return recipes.get(index % recipes.size());
    }
}
