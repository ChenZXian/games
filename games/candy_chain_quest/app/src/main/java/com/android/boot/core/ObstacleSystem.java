package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.ClearSummary;
import com.android.boot.entity.ObstacleType;
import com.android.boot.entity.Position;

import java.util.HashSet;
import java.util.Set;

public class ObstacleSystem {
    public void applyAdjacentHits(BoardManager boardManager, Set<Position> hitPositions, ClearSummary clearSummary) {
        Set<Position> visited = new HashSet<>();
        for (Position position : hitPositions) {
            hit(boardManager, position.row, position.col, clearSummary, visited);
            hit(boardManager, position.row - 1, position.col, clearSummary, visited);
            hit(boardManager, position.row + 1, position.col, clearSummary, visited);
            hit(boardManager, position.row, position.col - 1, clearSummary, visited);
            hit(boardManager, position.row, position.col + 1, clearSummary, visited);
        }
    }

    private void hit(BoardManager boardManager, int row, int col, ClearSummary clearSummary, Set<Position> visited) {
        if (!boardManager.inBounds(row, col)) {
            return;
        }
        Position key = new Position(row, col);
        if (!visited.add(key)) {
            return;
        }
        Cell cell = boardManager.getCell(row, col);
        ObstacleType before = cell.getObstacleType();
        int health = cell.getObstacleHealth();
        cell.damageObstacle();
        if (before != ObstacleType.NONE && before != ObstacleType.STONE && health > 0 && cell.getObstacleType() == ObstacleType.NONE) {
            clearSummary.addObstacle(before);
        }
    }
}
