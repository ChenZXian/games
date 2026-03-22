package com.android.boot.core;

public enum RuneFavor {
    FLAME("Flame Favor"),
    STORM("Storm Favor"),
    VITAL("Vital Favor");

    private final String label;

    RuneFavor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
