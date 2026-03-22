package com.android.boot.core;

public enum BattlePhase {
    VANGUARD("Phase 1  Vanguard"),
    ARROW_STORM("Phase 2  Arrow Storm"),
    KNIGHT_CRASH("Phase 3  Knight Crash"),
    SANCTIFIED_PUSH("Phase 4  Sanctified Push"),
    FORTRESS_SIEGE("Phase 5  Fortress Siege");

    private final String label;

    BattlePhase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
