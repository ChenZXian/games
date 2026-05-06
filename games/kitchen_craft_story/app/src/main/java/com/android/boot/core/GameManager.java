package com.android.boot.core;

import com.android.boot.entity.CookingStep;
import com.android.boot.entity.Recipe;
import com.android.boot.system.KitchenToolSystem;
import com.android.boot.system.OrderManager;
import com.android.boot.system.RecipeManager;
import com.android.boot.system.RewardManager;
import com.android.boot.system.ScoringSystem;

public class GameManager {
    public final RecipeManager recipeManager = new RecipeManager();
    public final KitchenToolSystem toolSystem = new KitchenToolSystem();
    public final OrderManager orderManager = new OrderManager();
    public final ScoringSystem scoringSystem = new ScoringSystem();
    public final RewardManager rewardManager = new RewardManager();
    public GameState state = GameState.MENU;
    public int recipeIndex;
    public Recipe recipe;
    public int stepIndex;
    public int stepProgress;
    public int perfectSteps;
    public int misses;
    public float heat;
    public float heatError;
    public int finalScore;
    public int stars;
    public int coins;
    public String unlockText = "Practice badge";

    public void selectRecipe(int index) {
        recipeIndex = index;
        recipe = recipeManager.get(index);
        orderManager.reset(recipe);
        stepIndex = 0;
        stepProgress = 0;
        perfectSteps = 0;
        misses = 0;
        heat = 0.5f;
        heatError = 0f;
        finalScore = 0;
        state = GameState.PLAYING;
    }

    public void restart() {
        selectRecipe(recipeIndex);
    }

    public void menu() {
        state = GameState.MENU;
    }

    public void pauseToggle() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    public CookingStep step() {
        if (recipe == null || stepIndex >= recipe.steps.size()) return null;
        return recipe.steps.get(stepIndex);
    }

    public void update(float delta) {
        if (state != GameState.PLAYING) return;
        orderManager.update(delta);
        heat += delta * 0.08f;
        if (heat > 1f) heat = 0f;
        CookingStep step = step();
        if (step != null) {
            heatError += Math.abs(step.targetHeat - heat) * delta * 0.12f;
        }
        if (orderManager.patience <= 0f) finish();
    }

    public boolean useTool(String tool) {
        if (state != GameState.PLAYING) return false;
        CookingStep step = step();
        if (step == null) return false;
        if (!toolSystem.matches(step.action, tool)) {
            misses++;
            return false;
        }
        stepProgress++;
        if (stepProgress >= step.targetTaps) {
            if (Math.abs(step.targetHeat - heat) < 0.35f) perfectSteps++;
            nextStep();
        }
        return true;
    }

    private void nextStep() {
        stepIndex++;
        stepProgress = 0;
        if (stepIndex >= recipe.steps.size()) finish();
    }

    public void finish() {
        finalScore = scoringSystem.score(perfectSteps, misses, heatError, orderManager.patience);
        stars = scoringSystem.stars(finalScore);
        coins = rewardManager.coins(finalScore, recipe == null ? 0 : recipe.reward);
        unlockText = rewardManager.unlock(stars);
        state = GameState.GAME_OVER;
    }
}
