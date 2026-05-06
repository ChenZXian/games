package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.ClearSummary;
import com.android.boot.entity.MatchGroup;
import com.android.boot.entity.Position;
import com.android.boot.entity.SpecialType;
import com.android.boot.entity.TileColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpecialEffectSystem {
    public SpecialType chooseGeneratedSpecial(MatchGroup group) {
        if (group.size() >= 5 && !group.isCrossLike()) {
            return SpecialType.COLOR;
        }
        if (group.isCrossLike()) {
            return SpecialType.BOMB;
        }
        if (group.size() == 4) {
            return group.isHorizontal() ? SpecialType.LINE_HORIZONTAL : SpecialType.LINE_VERTICAL;
        }
        return SpecialType.NONE;
    }

    public Set<Position> expandTriggeredTiles(BoardManager boardManager, Set<Position> baseClears, Position origin, TileColor colorTarget) {
        Set<Position> result = new HashSet<>(baseClears);
        List<Position> queue = new ArrayList<>(baseClears);
        Set<Position> processed = new HashSet<>();
        while (!queue.isEmpty()) {
            Position current = queue.remove(0);
            if (!processed.add(current)) {
                continue;
            }
            Cell cell = boardManager.getCell(current.row, current.col);
            SpecialType special = cell.getSpecialType();
            if (special == SpecialType.LINE_HORIZONTAL) {
                for (int col = 0; col < boardManager.getCols(); col++) {
                    Position target = new Position(current.row, col);
                    if (result.add(target)) {
                        queue.add(target);
                    }
                }
            } else if (special == SpecialType.LINE_VERTICAL) {
                for (int row = 0; row < boardManager.getRows(); row++) {
                    Position target = new Position(row, current.col);
                    if (result.add(target)) {
                        queue.add(target);
                    }
                }
            } else if (special == SpecialType.BOMB) {
                for (int row = current.row - 1; row <= current.row + 1; row++) {
                    for (int col = current.col - 1; col <= current.col + 1; col++) {
                        if (boardManager.inBounds(row, col)) {
                            Position target = new Position(row, col);
                            if (result.add(target)) {
                                queue.add(target);
                            }
                        }
                    }
                }
            } else if (special == SpecialType.COLOR) {
                TileColor targetColor = colorTarget;
                if (targetColor == null && origin != null && boardManager.getCell(origin.row, origin.col).hasTile()) {
                    targetColor = boardManager.getCell(origin.row, origin.col).getTileColor();
                }
                if (targetColor != null) {
                    for (int row = 0; row < boardManager.getRows(); row++) {
                        for (int col = 0; col < boardManager.getCols(); col++) {
                            Cell targetCell = boardManager.getCell(row, col);
                            if (targetCell.hasTile() && targetCell.getTileColor() == targetColor) {
                                Position target = new Position(row, col);
                                if (result.add(target)) {
                                    queue.add(target);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public Set<Position> resolveSpecialSwap(BoardManager boardManager, Position first, Position second) {
        Cell firstCell = boardManager.getCell(first.row, first.col);
        Cell secondCell = boardManager.getCell(second.row, second.col);
        SpecialType firstType = firstCell.getSpecialType();
        SpecialType secondType = secondCell.getSpecialType();
        Set<Position> result = new HashSet<>();
        result.add(first);
        result.add(second);
        if (firstType == SpecialType.COLOR && secondType == SpecialType.COLOR) {
            for (int row = 0; row < boardManager.getRows(); row++) {
                for (int col = 0; col < boardManager.getCols(); col++) {
                    result.add(new Position(row, col));
                }
            }
            return result;
        }
        if (firstType == SpecialType.COLOR || secondType == SpecialType.COLOR) {
            TileColor targetColor = firstType == SpecialType.COLOR ? secondCell.getTileColor() : firstCell.getTileColor();
            return expandTriggeredTiles(boardManager, result, null, targetColor);
        }
        if ((firstType == SpecialType.LINE_HORIZONTAL || firstType == SpecialType.LINE_VERTICAL) &&
                (secondType == SpecialType.LINE_HORIZONTAL || secondType == SpecialType.LINE_VERTICAL)) {
            for (int col = 0; col < boardManager.getCols(); col++) {
                result.add(new Position(first.row, col));
            }
            for (int row = 0; row < boardManager.getRows(); row++) {
                result.add(new Position(row, first.col));
            }
            for (int col = 0; col < boardManager.getCols(); col++) {
                result.add(new Position(second.row, col));
            }
            for (int row = 0; row < boardManager.getRows(); row++) {
                result.add(new Position(row, second.col));
            }
            return result;
        }
        if ((firstType == SpecialType.BOMB && (secondType == SpecialType.LINE_HORIZONTAL || secondType == SpecialType.LINE_VERTICAL)) ||
                (secondType == SpecialType.BOMB && (firstType == SpecialType.LINE_HORIZONTAL || firstType == SpecialType.LINE_VERTICAL))) {
            Position center = firstType == SpecialType.BOMB ? first : second;
            for (int row = center.row - 1; row <= center.row + 1; row++) {
                if (row >= 0 && row < boardManager.getRows()) {
                    for (int col = 0; col < boardManager.getCols(); col++) {
                        result.add(new Position(row, col));
                    }
                }
            }
            for (int col = center.col - 1; col <= center.col + 1; col++) {
                if (col >= 0 && col < boardManager.getCols()) {
                    for (int row = 0; row < boardManager.getRows(); row++) {
                        result.add(new Position(row, col));
                    }
                }
            }
            return result;
        }
        if (firstType == SpecialType.BOMB && secondType == SpecialType.BOMB) {
            int centerRow = (first.row + second.row) / 2;
            int centerCol = (first.col + second.col) / 2;
            for (int row = centerRow - 2; row <= centerRow + 2; row++) {
                for (int col = centerCol - 2; col <= centerCol + 2; col++) {
                    if (boardManager.inBounds(row, col)) {
                        result.add(new Position(row, col));
                    }
                }
            }
            return result;
        }
        return expandTriggeredTiles(boardManager, result, second, secondCell.getTileColor());
    }

    public int clearPositions(BoardManager boardManager, Set<Position> positions, ClearSummary clearSummary) {
        int specialCount = 0;
        for (Position position : positions) {
            if (!boardManager.inBounds(position.row, position.col)) {
                continue;
            }
            Cell cell = boardManager.getCell(position.row, position.col);
            if (cell.hasTile()) {
                clearSummary.addTile(position, cell.getTileColor());
                if (cell.getSpecialType() != SpecialType.NONE) {
                    specialCount++;
                }
                cell.clearTile();
            }
        }
        return specialCount;
    }
}
