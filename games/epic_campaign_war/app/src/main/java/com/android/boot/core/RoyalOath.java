package com.android.boot.core;

public enum RoyalOath {
    EMBER,
    STORM,
    SANCTUM;

    public String displayName() {
        if (this == EMBER) {
            return "Ember Oath";
        }
        if (this == STORM) {
            return "Storm Oath";
        }
        return "Sanctum Oath";
    }
}
