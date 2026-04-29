package com.android.boot.core;

public enum SurvivorRole {
    IDLE("Idle"),
    GATHERER("Gatherer"),
    FISHER("Fisher"),
    BUILDER("Builder"),
    GUARD("Guard"),
    HEALER("Healer");

    public final String label;

    SurvivorRole(String label) {
        this.label = label;
    }
}
