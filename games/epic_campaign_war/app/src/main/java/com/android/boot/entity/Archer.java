package com.android.boot.entity;

public class Archer extends Unit {
    public Archer(float x, float y) {
        super("Archer", Team.ALLY, true, false, false, false, x, y, 30f, 80f, 14f, 170f, 44f, 1.1f);
    }
}
