package com.android.boot.entity;

public class BallProjectile {
    public boolean active;
    public BallType type;
    public float x;
    public float y;
    public int row;
    public int verticalDir;
    public int bouncesLeft;
    public float hitPulse;
    public float heavyPulse;
    public float squash;
    public int collisionCount;

    public void reset(BallType ballType, int targetRow, float startX, float startY) {
        active = true;
        type = ballType;
        x = startX;
        y = startY;
        row = targetRow;
        verticalDir = targetRow < 2 ? 1 : -1;
        if (targetRow == 2) {
            verticalDir = 1;
        }
        bouncesLeft = 5;
        hitPulse = 0f;
        heavyPulse = 0f;
        squash = 0f;
        collisionCount = 0;
    }
}
