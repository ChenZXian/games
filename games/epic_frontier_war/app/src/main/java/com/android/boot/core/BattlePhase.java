package com.android.boot.core;

public enum BattlePhase {
    PHASE_ONE("Swarm Vanguard"),
    PHASE_TWO("Arrow Skirmish"),
    PHASE_THREE("Knight Breakers"),
    PHASE_FOUR("Priest Rally"),
    PHASE_FIVE("Fortress Siege");

    public final String label;

    BattlePhase(String label) {
        this.label = label;
    }
}
