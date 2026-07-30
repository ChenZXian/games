package com.android.boot.model;

public final class GameDefs {
    public static final int MENU = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;
    public static final int GAME_OVER = 3;

    public static final int BIRD_GUST = 0;
    public static final int BIRD_EMBER = 1;
    public static final int BIRD_BOLT = 2;
    public static final int BIRD_FROST = 3;
    public static final int BIRD_RAM = 4;

    public static final int MAT_WOOD = 0;
    public static final int MAT_STONE = 1;
    public static final int MAT_GLASS = 2;
    public static final int MAT_METAL = 3;

    public static final int TARGET_CORE = 0;
    public static final int TARGET_SENTRY = 1;
    public static final int TARGET_RELAY = 2;
    public static final int TARGET_BARREL = 3;

    public static final int WEATHER_CLEAR = 0;
    public static final int WEATHER_CROSSWIND = 1;
    public static final int WEATHER_UPDRAFT = 2;
    public static final int WEATHER_STORM = 3;
    public static final int WEATHER_FREEZE = 4;

    public static final String[] CHAPTER_NAMES = {
            "Cliff Nest",
            "Storm Bridge",
            "Iron Cloud Bastion",
            "Crystal Tempest",
            "Skyfall Keep"
    };

    public static final String[] OBJECTIVE_TEXT = {
            "Break the storm core",
            "Clear all sentries",
            "Overload the relay chain",
            "Shatter the frozen braces",
            "Destroy the boss citadel"
    };

    public static final String[] WEATHER_TEXT = {
            "Clear air",
            "Crosswind surge",
            "Thermal updraft",
            "Lightning storm",
            "Frozen pressure"
    };

    public static final String[] BIRD_NAMES = {
            "Gust Wing",
            "Ember Dive",
            "Bolt Splitter",
            "Frost Halo",
            "Ram Crest"
    };

    private GameDefs() {
    }
}
