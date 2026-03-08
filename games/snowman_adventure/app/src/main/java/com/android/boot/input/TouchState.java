package com.android.boot.input;

public class TouchState {
    public boolean leftHeld;
    public boolean jumpPressed;
    public boolean sprayHeld;
    public boolean kickPressed;

    public void clearInstant() {
        jumpPressed = false;
        kickPressed = false;
    }
}
