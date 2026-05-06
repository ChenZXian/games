package com.android.boot.entity;

public class SwapData {
    private final Position first;
    private final Position second;

    public SwapData(Position first, Position second) {
        this.first = first;
        this.second = second;
    }

    public Position getFirst() {
        return first;
    }

    public Position getSecond() {
        return second;
    }
}
