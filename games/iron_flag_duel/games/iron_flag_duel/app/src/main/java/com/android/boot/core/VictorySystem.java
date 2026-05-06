package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.Side;

public class VictorySystem {
    public String checkVictory(Board board, MoveGenerator moveGenerator) {
        boolean playerFlagAlive = false;
        boolean aiFlagAlive = false;
        for (Cell cell : board.allCells()) {
            if (cell.piece != null && cell.piece.isAlive()) {
                if (cell.piece.getSide() == Side.PLAYER && cell.piece.getType().isFlag()) {
                    playerFlagAlive = true;
                }
                if (cell.piece.getSide() == Side.AI && cell.piece.getType().isFlag()) {
                    aiFlagAlive = true;
                }
            }
        }
        if (!playerFlagAlive) {
            return "AI captured your flag";
        }
        if (!aiFlagAlive) {
            return "You captured the enemy flag";
        }
        if (moveGenerator.generate(board, Side.PLAYER).isEmpty() && !hasHiddenPiece(board, Side.PLAYER)) {
            return "Player has no legal moves";
        }
        if (moveGenerator.generate(board, Side.AI).isEmpty() && !hasHiddenPiece(board, Side.AI)) {
            return "AI has no legal moves";
        }
        return null;
    }

    private boolean hasHiddenPiece(Board board, Side side) {
        for (Cell cell : board.allCells()) {
            if (cell.piece != null && cell.piece.isAlive() && cell.piece.getSide() == side && !cell.piece.isKnownBy(side)) {
                return true;
            }
        }
        return false;
    }
}
