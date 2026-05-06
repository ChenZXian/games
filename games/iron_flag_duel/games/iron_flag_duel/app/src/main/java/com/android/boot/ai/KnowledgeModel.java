package com.android.boot.ai;

import com.android.boot.core.Board;
import com.android.boot.entity.Cell;
import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;
import com.android.boot.entity.Side;

import java.util.EnumMap;
import java.util.Map;

public class KnowledgeModel {
    private final Map<PieceType, Integer> unseenCounts = new EnumMap<>(PieceType.class);

    public KnowledgeModel() {
        reset();
    }

    public void reset() {
        unseenCounts.clear();
        add(PieceType.FLAG, 1);
        add(PieceType.MINE, 3);
        add(PieceType.BOMB, 2);
        add(PieceType.ENGINEER, 3);
        add(PieceType.PLATOON, 3);
        add(PieceType.COMPANY, 3);
        add(PieceType.BATTALION, 2);
        add(PieceType.REGIMENT, 2);
        add(PieceType.BRIGADE, 2);
        add(PieceType.DIVISION, 2);
        add(PieceType.CORPS, 1);
        add(PieceType.COMMANDER, 1);
    }

    private void add(PieceType type, int count) {
        unseenCounts.put(type, count);
    }

    public void onReveal(Piece piece) {
        PieceType type = piece.getType();
        unseenCounts.put(type, Math.max(0, unseenCounts.get(type) - 1));
    }

    public double estimateWinChance(Board board, Cell target, Piece attacker) {
        Piece defender = target.piece;
        if (defender != null && defender.isKnownBy(Side.AI)) {
            return resolveKnown(attacker.getType(), defender.getType());
        }
        double total = 0.0;
        double score = 0.0;
        for (Map.Entry<PieceType, Integer> entry : unseenCounts.entrySet()) {
            int count = entry.getValue();
            if (count <= 0) {
                continue;
            }
            double weight = count * positionWeight(entry.getKey(), target.row, target.col);
            total += weight;
            score += weight * resolveKnown(attacker.getType(), entry.getKey());
        }
        if (total <= 0.0) {
            return 0.5;
        }
        return score / total;
    }

    private double positionWeight(PieceType type, int row, int col) {
        double weight = 1.0;
        if (row >= 10 && (col == 1 || col == 3) && type == PieceType.FLAG) {
            weight += 4.0;
        }
        if (row >= 10 && type == PieceType.MINE) {
            weight += 2.5;
        }
        if (row >= 9 && type == PieceType.BOMB) {
            weight += 1.5;
        }
        if (row <= 8 && type == PieceType.ENGINEER) {
            weight += 0.4;
        }
        return weight;
    }

    private double resolveKnown(PieceType attacker, PieceType defender) {
        if (defender.isFlag()) {
            return 1.0;
        }
        if (attacker.isBomb() || defender.isBomb()) {
            return 0.55;
        }
        if (defender.isMine()) {
            return attacker == PieceType.ENGINEER ? 1.0 : 0.0;
        }
        if (attacker.getRank() > defender.getRank()) {
            return 1.0;
        }
        if (attacker.getRank() < defender.getRank()) {
            return 0.0;
        }
        return 0.35;
    }
}
