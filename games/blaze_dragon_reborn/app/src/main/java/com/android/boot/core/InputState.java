package com.android.boot.core;

public class InputState {
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean dodge;
    public boolean attack;
    public boolean chi;

    public void clearOneShot() {
        jump = false;
        dodge = false;
        attack = false;
        chi = false;
    }
}
