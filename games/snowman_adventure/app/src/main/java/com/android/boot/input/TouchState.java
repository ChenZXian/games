package com.android.boot.input;

public class TouchState {
    public boolean leftHeld;
    public boolean rightHeld;
    public boolean jumpPressed;
    public boolean sprayHeld;
    public boolean kickPressed;

    public void clearInstant() {
        jumpPressed = false;
        kickPressed = false;
    }
}
