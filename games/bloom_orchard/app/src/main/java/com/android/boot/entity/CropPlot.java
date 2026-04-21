package com.android.boot.entity;

public class CropPlot {
    public int index;
    public boolean unlocked;
    public CropType crop;
    public float growth;
    public int watered;
    public String fertilizer;
    public boolean mature;
    public float sway;
    public float sparkle;

    public CropPlot(int index, boolean unlocked) {
        this.index = index;
        this.unlocked = unlocked;
        fertilizer = "NONE";
    }

    public boolean isEmpty() {
        return crop == null;
    }
}
