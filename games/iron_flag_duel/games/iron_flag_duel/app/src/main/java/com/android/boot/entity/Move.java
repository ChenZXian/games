package com.android.boot.entity;

public class Move {
    public final int fromRow;
    public final int fromCol;
    public final int toRow;
    public final int toCol;
    public final boolean attack;

    public Move(int fromRow, int fromCol, int toRow, int toCol, boolean attack) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.attack = attack;
    }
}
