package com.android.boot.core;

import com.android.boot.entity.BattleOutcome;
import com.android.boot.entity.BattleResult;
import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;

public class BattleResolver {
    public BattleResult resolve(Piece attacker, Piece defender) {
        if (defender.getType().isFlag()) {
            return new BattleResult(BattleOutcome.CAPTURE_FLAG, attacker, defender, attacker.getType().getLabel() + " captured the flag");
        }
        if (attacker.getType().isBomb() || defender.getType().isBomb()) {
            return new BattleResult(BattleOutcome.BOTH_DIE, attacker, defender, "Bomb blast removed both pieces");
        }
        if (defender.getType().isMine()) {
            if (attacker.getType() == PieceType.ENGINEER) {
                return new BattleResult(BattleOutcome.ATTACKER_WINS, attacker, defender, "Engineer cleared a mine");
            }
            return new BattleResult(BattleOutcome.DEFENDER_WINS, attacker, defender, defender.getType().getLabel() + " stopped the attack");
        }
        if (attacker.getType().getRank() > defender.getType().getRank()) {
            return new BattleResult(BattleOutcome.ATTACKER_WINS, attacker, defender, attacker.getType().getLabel() + " won the battle");
        }
        if (attacker.getType().getRank() < defender.getType().getRank()) {
            return new BattleResult(BattleOutcome.DEFENDER_WINS, attacker, defender, defender.getType().getLabel() + " defended successfully");
        }
        return new BattleResult(BattleOutcome.BOTH_DIE, attacker, defender, "Equal ranks removed both pieces");
    }
}
