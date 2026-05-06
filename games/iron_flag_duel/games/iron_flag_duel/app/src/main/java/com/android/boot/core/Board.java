package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.CellType;
import com.android.boot.entity.Side;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int ROWS = 12;
    public static final int COLS = 5;
    private final Cell[][] cells = new Cell[ROWS][COLS];

    public Board() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                cells[r][c] = new Cell(r, c, resolveType(r, c));
            }
        }
    }

    private CellType resolveType(int row, int col) {
        if ((row == 0 || row == 11) && (col == 1 || col == 3)) {
            return CellType.HEADQUARTERS;
        }
        if (isCamp(row, col)) {
            return CellType.CAMP;
        }
        if (isRail(row, col)) {
            return CellType.RAIL;
        }
        return CellType.ROAD;
    }

    public static boolean isCamp(int row, int col) {
        return (row == 2 && (col == 1 || col == 3)) ||
                (row == 3 && col == 2) ||
                (row == 4 && (col == 1 || col == 3)) ||
                (row == 7 && (col == 1 || col == 3)) ||
                (row == 8 && col == 2) ||
                (row == 9 && (col == 1 || col == 3));
    }

    public static boolean isRail(int row, int col) {
        return row == 1 || row == 5 || row == 6 || row == 10 || col == 0 || col == 4 || col == 2;
    }

    public boolean areRailConnected(int rowA, int colA, int rowB, int colB) {
        Cell a = get(rowA, colA);
        Cell b = get(rowB, colB);
        if (a == null || b == null || a.type != CellType.RAIL || b.type != CellType.RAIL) {
            return false;
        }
        int dr = Math.abs(rowA - rowB);
        int dc = Math.abs(colA - colB);
        if (dr + dc != 1) {
            return false;
        }
        if (rowA == rowB) {
            return rowA == 1 || rowA == 5 || rowA == 6 || rowA == 10;
        }
        if (colA != colB) {
            return false;
        }
        // Side rails are split by the river; only center rail bridges top/bottom halves.
        if ((rowA == 5 && rowB == 6) || (rowA == 6 && rowB == 5)) {
            return colA == 2;
        }
        return colA == 0 || colA == 2 || colA == 4;
    }

    public Cell get(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        return cells[row][col];
    }

    public boolean inPlayerZone(int row) {
        return row >= 6;
    }

    public boolean inAiZone(int row) {
        return row <= 5;
    }

    public boolean inLastTwoRows(Side side, int row) {
        return side == Side.PLAYER ? row >= 10 : row <= 1;
    }

    public List<Cell> allCells() {
        List<Cell> result = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                result.add(cells[r][c]);
            }
        }
        return result;
    }
}
