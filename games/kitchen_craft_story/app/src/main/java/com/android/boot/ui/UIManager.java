package com.android.boot.ui;

public class UIManager {
    public String stepLabel(String action, String tool) {
        return "Use " + tool + " to " + action;
    }

    public String rewardLabel(int coins, int stars) {
        return "Coins " + coins + "  Stars " + stars;
    }
}
