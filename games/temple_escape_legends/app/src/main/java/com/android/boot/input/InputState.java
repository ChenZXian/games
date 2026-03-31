package com.android.boot.input;

public class InputState {
    public boolean leftPressed;
    public boolean rightPressed;
    public boolean upPressed;
    public boolean downPressed;

    public void consume() {
        leftPressed = false;
        rightPressed = false;
        upPressed = false;
        downPressed = false;
    }
}
