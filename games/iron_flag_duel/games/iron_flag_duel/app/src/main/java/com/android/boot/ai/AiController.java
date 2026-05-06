package com.android.boot.ai;

import com.android.boot.core.Board;
import com.android.boot.core.MoveGenerator;
import com.android.boot.entity.Cell;
import com.android.boot.entity.Move;
import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;
import com.android.boot.entity.Side;

import java.util.List;
import java.util.Random;

public class AiController {
    private final MoveGenerator moveGenerator;
    private final AiEvaluator evaluator;
    private final Random random = new Random();

    public AiController(MoveGenerator moveGenerator, AiEvaluator evaluator) {
        this.moveGenerator = moveGenerator;
        this.evaluator = evaluator;
    }

    public Move chooseMove(Board board) {
        List<Move> moves = moveGenerator.generate(board, Side.AI);
        if (moves.isEmpty()) {
            return null;
        }
        List<Move> attackMoves = new java.util.ArrayList<>();
        for (Move move : moves) {
            if (move.attack) {
                attackMoves.add(move);
            }
        }
        List<Move> candidates = attackMoves.isEmpty() ? moves : attackMoves;
        Move best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Move move : candidates) {
            float score = evaluator.score(board, move)
                    + tacticalBias(board, move)
                    + objectivePriority(board, move)
                    + random.nextFloat() * 0.8f;
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    private float tacticalBias(Board board, Move move) {
        Cell from = board.get(move.fromRow, move.fromCol);
        Cell to = board.get(move.toRow, move.toCol);
        if (from == null || from.piece == null || to == null) {
            return 0f;
        }
        Piece attacker = from.piece;
        float score = 0f;
        if (move.attack && to.piece != null) {
            Piece target = to.piece;
            if (target.isKnownBy(Side.AI)) {
                score += target.getType().getValue() * 0.07f;
            } else {
                score += 8f;
            }
            if (target.getType() == PieceType.FLAG) {
                score += 200f;
            }
        }
        if (attacker.getType().getValue() >= PieceType.CORPS.getValue()) {
            // Keep high-value units slightly away from deep frontline if not attacking.
            if (!move.attack && move.toRow > 8) {
                score -= 10f;
            }
        }
        return score;
    }

    private float objectivePriority(Board board, Move move) {
        if (!move.attack) {
            return 0f;
        }
        Cell from = board.get(move.fromRow, move.fromCol);
        Cell to = board.get(move.toRow, move.toCol);
        if (from == null || from.piece == null || to == null || to.piece == null) {
            return 0f;
        }
        Piece attacker = from.piece;
        Piece defender = to.piece;
        boolean enemyHasMines = hasAliveEnemyMines(board);

        if (defender.getType() == PieceType.MINE && attacker.getType() == PieceType.ENGINEER) {
            return 80000f;
        }
        if (defender.getType() == PieceType.FLAG) {
            return enemyHasMines ? 2000f : 90000f;
        }
        if (defender.getType() == PieceType.MINE && attacker.getType() != PieceType.ENGINEER) {
            return -25000f;
        }
        if (defender.isKnownBy(Side.AI)
                && attacker.getType().getValue() >= PieceType.DIVISION.getValue()
                && defender.getType().getValue() <= PieceType.COMPANY.getValue()
                && defender.getType() != PieceType.FLAG
                && defender.getType() != PieceType.MINE) {
            // Keep top units from trading into low-value pieces.
            return -15000f;
        }
        if (defender.isKnownBy(Side.AI)
                && attacker.getType().getValue() - defender.getType().getValue() >= 120
                && defender.getType() != PieceType.FLAG
                && defender.getType() != PieceType.MINE) {
            return -3000f;
        }
        return 0f;
    }

    private boolean hasAliveEnemyMines(Board board) {
        for (Cell cell : board.allCells()) {
            if (cell.piece == null || !cell.piece.isAlive()) {
                continue;
            }
            if (cell.piece.getSide() == Side.PLAYER && cell.piece.getType() == PieceType.MINE) {
                return true;
            }
        }
        return false;
    }
}
