package com.android.boot.core;

import com.android.boot.entity.Cell;
import com.android.boot.entity.CellType;
import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;
import com.android.boot.entity.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DeploymentSystem {
    private final Random random = new Random();

    public void randomFlipDeploy(Board board, List<Piece> playerPieces, List<Piece> aiPieces) {
        clear(board, Side.PLAYER);
        clear(board, Side.AI);
        List<Cell> stations = new ArrayList<>();
        for (Cell cell : board.allCells()) {
            if (cell.type != CellType.CAMP) {
                cell.piece = null;
                stations.add(cell);
            }
        }
        List<Piece> allPieces = new ArrayList<>();
        allPieces.addAll(playerPieces);
        allPieces.addAll(aiPieces);
        Collections.shuffle(allPieces, random);
        Collections.shuffle(stations, random);
        int count = Math.min(allPieces.size(), stations.size());
        for (int i = 0; i < count; i++) {
            stations.get(i).piece = allPieces.get(i);
        }
    }

    public void autoDeploy(Board board, Side side, List<Piece> pieces) {
        clear(board, side);
        List<Cell> cells = zoneCells(board, side);
        List<Piece> order = new ArrayList<>(pieces);
        placeFlag(board, side, order);
        placeMines(board, side, order);
        Collections.shuffle(cells, random);
        int index = 0;
        for (Piece piece : order) {
            while (index < cells.size() && cells.get(index).piece != null) {
                index++;
            }
            if (index < cells.size()) {
                cells.get(index).piece = piece;
                index++;
            }
        }
    }

    private void clear(Board board, Side side) {
        for (Cell cell : board.allCells()) {
            if (cell.piece != null && cell.piece.getSide() == side) {
                cell.piece = null;
            }
        }
    }

    private List<Cell> zoneCells(Board board, Side side) {
        List<Cell> result = new ArrayList<>();
        for (Cell cell : board.allCells()) {
            if (side == Side.PLAYER && board.inPlayerZone(cell.row)) {
                result.add(cell);
            }
            if (side == Side.AI && board.inAiZone(cell.row)) {
                result.add(cell);
            }
        }
        return result;
    }

    private void placeFlag(Board board, Side side, List<Piece> pieces) {
        Piece flag = findAndRemove(pieces, PieceType.FLAG);
        int row = side == Side.PLAYER ? 11 : 0;
        int col = random.nextBoolean() ? 1 : 3;
        board.get(row, col).piece = flag;
    }

    private void placeMines(Board board, Side side, List<Piece> pieces) {
        for (int i = 0; i < 3; i++) {
            Piece mine = findAndRemove(pieces, PieceType.MINE);
            List<Cell> candidates = new ArrayList<>();
            for (Cell cell : board.allCells()) {
                if (cell.piece == null && board.inLastTwoRows(side, cell.row) && cell.type != CellType.CAMP) {
                    if ((side == Side.PLAYER && board.inPlayerZone(cell.row)) || (side == Side.AI && board.inAiZone(cell.row))) {
                        candidates.add(cell);
                    }
                }
            }
            Collections.shuffle(candidates, random);
            candidates.get(0).piece = mine;
        }
    }

    private Piece findAndRemove(List<Piece> pieces, PieceType type) {
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).getType() == type) {
                return pieces.remove(i);
            }
        }
        return null;
    }

    public boolean canPlaceInCell(Board board, Side side, Piece piece, Cell target) {
        if (piece == null || target == null) {
            return false;
        }
        if (side == Side.PLAYER && !board.inPlayerZone(target.row)) {
            return false;
        }
        if (side == Side.AI && !board.inAiZone(target.row)) {
            return false;
        }
        if (piece.getType() == PieceType.FLAG) {
            return target.type == CellType.HEADQUARTERS && ((side == Side.PLAYER && target.row == 11) || (side == Side.AI && target.row == 0));
        }
        if (piece.getType() == PieceType.MINE) {
            return board.inLastTwoRows(side, target.row) && target.type != CellType.CAMP;
        }
        return target.type != CellType.HEADQUARTERS || target.piece == null;
    }

    public void swap(Cell first, Cell second) {
        Piece tmp = first.piece;
        first.piece = second.piece;
        second.piece = tmp;
    }
}
