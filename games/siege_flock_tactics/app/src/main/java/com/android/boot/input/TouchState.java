package com.android.boot.input;

public class TouchState {
    public boolean touching;
    public float downX;
    public float downY;
    public float nowX;
    public float nowY;
    public boolean released;
    public boolean tapped;

    public void resetFrame() {
        released = false;
        tapped = false;
    }
}
