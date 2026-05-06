package com.android.boot.entity;

public class BattleResult {
    public final BattleOutcome outcome;
    public final Piece attacker;
    public final Piece defender;
    public final String message;

    public BattleResult(BattleOutcome outcome, Piece attacker, Piece defender, String message) {
        this.outcome = outcome;
        this.attacker = attacker;
        this.defender = defender;
        this.message = message;
    }
}
