package com.android.boot.core;

public class FallSystem {
    public int apply(BoardManager boardManager) {
        return boardManager.collapseAndRefill();
    }
}
