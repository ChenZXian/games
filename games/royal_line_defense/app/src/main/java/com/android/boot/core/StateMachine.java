package com.android.boot.core;

public class StateMachine {
    private GameState state = GameState.MENU;

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }
}
