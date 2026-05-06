package com.android.boot.entity;

public class Cell {
    public final int row;
    public final int col;
    public final CellType type;
    public Piece piece;

    public Cell(int row, int col, CellType type) {
        this.row = row;
        this.col = col;
        this.type = type;
    }
}
