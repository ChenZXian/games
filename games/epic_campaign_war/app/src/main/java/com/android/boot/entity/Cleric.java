package com.android.boot.entity;

public class Cleric extends Unit {
    public Cleric(float x, float y) {
        super("Cleric", Team.ALLY, false, true, false, false, x, y, 31f, 90f, 16f, 70f, 38f, 1.5f);
    }
}
