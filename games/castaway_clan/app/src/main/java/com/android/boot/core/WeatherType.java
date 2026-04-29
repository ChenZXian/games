package com.android.boot.core;

public enum WeatherType {
    CLEAR("Clear"),
    WINDY("Windy"),
    RAIN("Rain"),
    STORM("Storm");

    public final String label;

    WeatherType(String label) {
        this.label = label;
    }
}
