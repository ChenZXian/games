package com.android.boot.ai;

import com.android.boot.core.Board;
import com.android.boot.entity.Cell;
import com.android.boot.entity.Move;
import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;

public class AiEvaluator {
    private final KnowledgeModel knowledgeModel;

    public AiEvaluator(KnowledgeModel knowledgeModel) {
        this.knowledgeModel = knowledgeModel;
    }

    public float score(Board board, Move move) {
        Cell from = board.get(move.fromRow, move.fromCol);
        Cell to = board.get(move.toRow, move.toCol);
        Piece attacker = from.piece;
        float score = 0f;
        score += advanceBias(attacker, move);
        score += formationBias(move);
        if (move.attack) {
            score += attackBias(board, attacker, to);
        } else {
            score += scoutBias(attacker, move);
        }
        score += engineerBias(attacker, move, to);
        return score;
    }

    private float advanceBias(Piece attacker, Move move) {
        int delta = move.toRow - move.fromRow;
        float value = -delta * 3f;
        if (attacker.getType() == PieceType.COMMANDER || attacker.getType() == PieceType.CORPS) {
            value *= 0.6f;
        }
        return value;
    }

    private float formationBias(Move move) {
        float score = 0f;
        if (move.toRow <= 2 && (move.toCol == 1 || move.toCol == 3)) {
            score += 16f;
        }
        if (move.fromRow <= 2 && move.toRow > move.fromRow) {
            score -= 20f;
        }
        if (move.toRow <= 1) {
            score += 12f;
        }
        return score;
    }

    private float scoutBias(Piece attacker, Move move) {
        float score = 0f;
        if (attacker.getType().getValue() <= PieceType.COMPANY.getValue()) {
            score += 8f;
        }
        if (move.toRow >= 9) {
            score += 10f;
        }
        return score;
    }

    private float engineerBias(Piece attacker, Move move, Cell to) {
        if (attacker.getType() != PieceType.ENGINEER) {
            return 0f;
        }
        float score = 6f;
        if (move.toRow >= 9) {
            score += 12f;
        }
        if (to.piece != null) {
            score += 10f;
        }
        return score;
    }

    private float attackBias(Board board, Piece attacker, Cell target) {
        double winChance = knowledgeModel.estimateWinChance(board, target, attacker);
        float score = (float) (winChance * 120f);
        if (target.piece != null && target.piece.isKnownBy(com.android.boot.entity.Side.AI)) {
            score += target.piece.getType().getValue() * (float) winChance * 0.4f;
        } else {
            score += 18f;
        }
        if (attacker.getType().getValue() > 250) {
            score -= (float) ((1.0 - winChance) * 70f);
        }
        if (target.row >= 10 && attacker.getType() == PieceType.ENGINEER) {
            score += 18f;
        }
        return score;
    }
}
