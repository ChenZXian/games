package com.android.boot.model;

public enum BallType {
    NORMAL("Normal", 1f),
    BOMB("Bomb", 0.9f),
    GIANT("Giant", 1.45f);

    public final String label;
    public final float scale;

    BallType(String label, float scale) {
        this.label = label;
        this.scale = scale;
    }
}
