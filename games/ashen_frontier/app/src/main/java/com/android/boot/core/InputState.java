package com.android.boot.core;

public class InputState {
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean attack;
    public boolean skill;

    public void clearOneShot() {
        jump = false;
        attack = false;
        skill = false;
    }
}
