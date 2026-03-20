package com.android.boot.entity;

public class ExplosionPulse {
    public boolean active;
    public int centerRow;
    public int centerCol;
    public float life;

    public void trigger(int row, int col) {
        active = true;
        centerRow = row;
        centerCol = col;
        life = 0.4f;
    }
}
