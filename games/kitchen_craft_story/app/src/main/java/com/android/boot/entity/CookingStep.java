package com.android.boot.entity;

public class CookingStep {
    public final String action;
    public final String tool;
    public final int targetTaps;
    public final float targetHeat;

    public CookingStep(String action, String tool, int targetTaps, float targetHeat) {
        this.action = action;
        this.tool = tool;
        this.targetTaps = targetTaps;
        this.targetHeat = targetHeat;
    }
}
