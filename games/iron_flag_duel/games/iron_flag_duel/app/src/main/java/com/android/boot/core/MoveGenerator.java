package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.CellType;
import com.android.boot.entity.Move;
import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;
import com.android.boot.entity.Side;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class MoveGenerator {
    private static final int[][] ORTHO = new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] DIAG = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}};

    public List<Move> generate(Board board, Side side) {
        List<Move> moves = new ArrayList<>();
        for (Cell cell : board.allCells()) {
            Piece piece = cell.piece;
            if (piece == null || piece.getSide() != side || !piece.getType().isMovable() || !piece.isKnownBy(side)) {
                continue;
            }
            if (cell.type == CellType.HEADQUARTERS) {
                continue;
            }
            moves.addAll(generateForPiece(board, cell.row, cell.col));
        }
        return moves;
    }

    public List<Move> generateForPiece(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        Cell origin = board.get(row, col);
        if (origin == null || origin.piece == null || !origin.piece.getType().isMovable() || !origin.piece.isKnownBy(origin.piece.getSide())) {
            return moves;
        }
        addStepMoves(board, origin, moves, ORTHO);
        if (origin.type == CellType.CAMP) {
            addStepMoves(board, origin, moves, DIAG);
        }
        if (origin.type == CellType.RAIL) {
            if (origin.piece.getType() == PieceType.ENGINEER) {
                addEngineerRailMoves(board, origin, moves);
            } else {
                addRailLineMoves(board, origin, moves);
            }
        }
        return moves;
    }

    private void addStepMoves(Board board, Cell origin, List<Move> moves, int[][] dirs) {
        for (int[] dir : dirs) {
            Cell target = board.get(origin.row + dir[0], origin.col + dir[1]);
            if (target == null) {
                continue;
            }
            if (target.type == CellType.HEADQUARTERS && target.piece != null) {
                continue;
            }
            if (target.piece == null) {
                moves.add(new Move(origin.row, origin.col, target.row, target.col, false));
            } else if (target.piece.getSide() != origin.piece.getSide()) {
                if (target.type == CellType.CAMP) {
                    continue;
                }
                moves.add(new Move(origin.row, origin.col, target.row, target.col, true));
            }
        }
    }

    private void addRailLineMoves(Board board, Cell origin, List<Move> moves) {
        for (int[] dir : ORTHO) {
            int r = origin.row + dir[0];
            int c = origin.col + dir[1];
            while (true) {
                Cell target = board.get(r, c);
                if (target == null || target.type != CellType.RAIL || !board.areRailConnected(r - dir[0], c - dir[1], r, c)) {
                    break;
                }
                if (target.piece == null) {
                    moves.add(new Move(origin.row, origin.col, r, c, false));
                } else {
                    if (target.piece.getSide() != origin.piece.getSide()) {
                        if (target.type == CellType.CAMP) {
                            break;
                        }
                        moves.add(new Move(origin.row, origin.col, r, c, true));
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }

    private void addEngineerRailMoves(Board board, Cell origin, List<Move> moves) {
        Queue<Cell> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin.row + ":" + origin.col);
        while (!queue.isEmpty()) {
            Cell current = queue.poll();
            for (int[] dir : ORTHO) {
                Cell next = board.get(current.row + dir[0], current.col + dir[1]);
                if (next == null || next.type != CellType.RAIL
                        || !board.areRailConnected(current.row, current.col, next.row, next.col)) {
                    continue;
                }
                String key = next.row + ":" + next.col;
                if (visited.contains(key)) {
                    continue;
                }
                visited.add(key);
                if (next.piece == null) {
                    if (!(next.row == origin.row && next.col == origin.col)) {
                        moves.add(new Move(origin.row, origin.col, next.row, next.col, false));
                    }
                    queue.add(next);
                } else if (next.piece.getSide() != origin.piece.getSide()) {
                    if (next.type == CellType.CAMP) {
                        continue;
                    }
                    moves.add(new Move(origin.row, origin.col, next.row, next.col, true));
                }
            }
        }
    }
}
