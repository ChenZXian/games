package com.android.boot.input;

public final class TouchState {
    private float x;
    private float y;
    private boolean tapped;

    public void setTap(float x, float y) {
        this.x = x;
        this.y = y;
        tapped = true;
    }

    public boolean consumeTap() {
        boolean value = tapped;
        tapped = false;
        return value;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
