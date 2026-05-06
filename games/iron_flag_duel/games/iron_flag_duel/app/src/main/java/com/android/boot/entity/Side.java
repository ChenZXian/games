package com.android.boot.entity;

public enum Side {
    PLAYER,
    AI;

    public Side opponent() {
        return this == PLAYER ? AI : PLAYER;
    }
}
