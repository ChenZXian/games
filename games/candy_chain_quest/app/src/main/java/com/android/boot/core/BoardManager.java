package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.LevelConfig;
import com.android.boot.entity.MatchGroup;
import com.android.boot.entity.ObstacleSeed;
import com.android.boot.entity.ObstacleType;
import com.android.boot.entity.Position;
import com.android.boot.entity.SpawnProfile;
import com.android.boot.entity.SpecialType;
import com.android.boot.entity.TileColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardManager {
    private final RandomProvider randomProvider;
    private Cell[][] grid;
    private int rows;
    private int cols;
    private SpawnProfile spawnProfile;

    public BoardManager(RandomProvider randomProvider) {
        this.randomProvider = randomProvider;
    }

    public void loadLevel(LevelConfig levelConfig) {
        rows = levelConfig.getRows();
        cols = levelConfig.getCols();
        spawnProfile = levelConfig.getSpawnProfile();
        grid = new Cell[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                grid[row][col] = new Cell();
            }
        }
        for (ObstacleSeed seed : levelConfig.getObstacleSeeds()) {
            Cell cell = grid[seed.getRow()][seed.getCol()];
            boolean blocksMove = seed.getObstacleType() == ObstacleType.CRATE || seed.getObstacleType() == ObstacleType.STONE || seed.getObstacleType() == ObstacleType.CHAIN;
            boolean blocksSpawn = seed.getObstacleType() == ObstacleType.CRATE || seed.getObstacleType() == ObstacleType.STONE;
            cell.setObstacle(seed.getObstacleType(), seed.getHealth(), blocksMove, blocksSpawn);
        }
        fillStableBoard();
    }

    private void fillStableBoard() {
        do {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    Cell cell = grid[row][col];
                    if (cell.canHostTile()) {
                        fillCellAvoidImmediateMatch(row, col);
                    } else {
                        cell.clearTile();
                    }
                }
            }
        } while (!hasAnyMove() || !findMatches().isEmpty());
    }

    private void fillCellAvoidImmediateMatch(int row, int col) {
        Cell cell = grid[row][col];
        TileColor candidate;
        int guard = 0;
        do {
            candidate = randomProvider.nextColor(spawnProfile);
            guard++;
            cell.setTile(candidate, SpecialType.NONE);
        } while (createsImmediateMatch(row, col) && guard < 30);
    }

    private boolean createsImmediateMatch(int row, int col) {
        Cell cell = grid[row][col];
        TileColor color = cell.getTileColor();
        if (color == null) {
            return false;
        }
        if (col >= 2 && sameColor(color, row, col - 1) && sameColor(color, row, col - 2)) {
            return true;
        }
        if (row >= 2 && sameColor(color, row - 1, col) && sameColor(color, row - 2, col)) {
            return true;
        }
        return false;
    }

    private boolean sameColor(TileColor color, int row, int col) {
        if (!inBounds(row, col)) {
            return false;
        }
        Cell cell = grid[row][col];
        return cell.hasTile() && cell.getTileColor() == color;
    }

    public Cell getCell(int row, int col) {
        return grid[row][col];
    }

    public Cell[][] snapshot() {
        Cell[][] result = new Cell[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                result[row][col] = grid[row][col].copy();
            }
        }
        return result;
    }

    public boolean trySwap(Position first, Position second) {
        if (!inBounds(first.row, first.col) || !inBounds(second.row, second.col)) {
            return false;
        }
        if (!first.isAdjacent(second)) {
            return false;
        }
        if (grid[first.row][first.col].isMoveBlocked() || grid[second.row][second.col].isMoveBlocked()) {
            return false;
        }
        swap(first, second);
        return true;
    }

    public void swap(Position first, Position second) {
        Cell firstCell = grid[first.row][first.col];
        Cell secondCell = grid[second.row][second.col];
        TileColor firstColor = firstCell.getTileColor();
        SpecialType firstSpecial = firstCell.getSpecialType();
        firstCell.setTile(secondCell.getTileColor(), secondCell.getSpecialType());
        secondCell.setTile(firstColor, firstSpecial);
    }

    public List<MatchGroup> findMatches() {
        List<MatchGroup> groups = new ArrayList<>();
        boolean[][] added = new boolean[rows][cols];
        for (int row = 0; row < rows; row++) {
            int start = 0;
            while (start < cols) {
                Cell startCell = grid[row][start];
                if (!startCell.hasTile()) {
                    start++;
                    continue;
                }
                TileColor color = startCell.getTileColor();
                int end = start + 1;
                while (end < cols && sameColor(color, row, end)) {
                    end++;
                }
                if (end - start >= 3) {
                    MatchGroup group = new MatchGroup();
                    group.setHorizontal(true);
                    for (int col = start; col < end; col++) {
                        group.add(new Position(row, col));
                        added[row][col] = true;
                    }
                    groups.add(group);
                }
                start = end;
            }
        }
        for (int col = 0; col < cols; col++) {
            int start = 0;
            while (start < rows) {
                Cell startCell = grid[start][col];
                if (!startCell.hasTile()) {
                    start++;
                    continue;
                }
                TileColor color = startCell.getTileColor();
                int end = start + 1;
                while (end < rows && sameColor(color, end, col)) {
                    end++;
                }
                if (end - start >= 3) {
                    List<Position> positions = new ArrayList<>();
                    for (int row = start; row < end; row++) {
                        positions.add(new Position(row, col));
                    }
                    boolean merged = false;
                    for (MatchGroup group : groups) {
                        for (Position position : positions) {
                            if (group.getPositions().contains(position)) {
                                group.setVertical(true);
                                for (Position item : positions) {
                                    if (!group.getPositions().contains(item)) {
                                        group.add(item);
                                    }
                                }
                                merged = true;
                                break;
                            }
                        }
                        if (merged) {
                            break;
                        }
                    }
                    if (!merged) {
                        MatchGroup group = new MatchGroup();
                        group.setVertical(true);
                        for (Position position : positions) {
                            group.add(position);
                        }
                        groups.add(group);
                    }
                }
                start = end;
            }
        }
        return groups;
    }

    public boolean hasAnyMove() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Position current = new Position(row, col);
                if (col + 1 < cols && canMovePair(current, new Position(row, col + 1))) {
                    return true;
                }
                if (row + 1 < rows && canMovePair(current, new Position(row + 1, col))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canMovePair(Position first, Position second) {
        if (grid[first.row][first.col].isMoveBlocked() || grid[second.row][second.col].isMoveBlocked()) {
            return false;
        }
        swap(first, second);
        boolean valid = !findMatches().isEmpty() || grid[first.row][first.col].getSpecialType() != SpecialType.NONE || grid[second.row][second.col].getSpecialType() != SpecialType.NONE;
        swap(first, second);
        return valid;
    }

    public void shuffleUntilPlayable() {
        List<Cell> movableTiles = new ArrayList<>();
        List<Position> movablePositions = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = grid[row][col];
                if (cell.canHostTile()) {
                    movableTiles.add(cell.copy());
                    movablePositions.add(new Position(row, col));
                }
            }
        }
        int guard = 0;
        do {
            Collections.shuffle(movableTiles, randomProvider.getRandom());
            for (int i = 0; i < movablePositions.size(); i++) {
                Position position = movablePositions.get(i);
                Cell source = movableTiles.get(i);
                grid[position.row][position.col].setTile(source.getTileColor(), source.getSpecialType());
            }
            guard++;
        } while ((!findMatches().isEmpty() || !hasAnyMove()) && guard < 100);
    }

    public int collapseAndRefill() {
        int spawned = 0;
        for (int col = 0; col < cols; col++) {
            int writeRow = rows - 1;
            for (int row = rows - 1; row >= 0; row--) {
                Cell cell = grid[row][col];
                if (!cell.canHostTile()) {
                    writeRow = row - 1;
                    continue;
                }
                if (cell.hasTile()) {
                    if (writeRow != row) {
                        Cell target = grid[writeRow][col];
                        target.setTile(cell.getTileColor(), cell.getSpecialType());
                        cell.clearTile();
                    }
                    writeRow--;
                }
            }
            for (int row = writeRow; row >= 0; row--) {
                Cell target = grid[row][col];
                if (target.canHostTile() && !target.isSpawnBlocked()) {
                    target.setTile(randomProvider.nextColor(spawnProfile), SpecialType.NONE);
                    spawned++;
                }
            }
        }
        return spawned;
    }

    public int countObstacle(ObstacleType obstacleType) {
        int count = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col].getObstacleType() == obstacleType) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && col >= 0 && row < rows && col < cols;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
}
