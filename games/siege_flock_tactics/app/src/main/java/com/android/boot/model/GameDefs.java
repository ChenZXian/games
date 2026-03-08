package com.android.boot.model;

public final class GameDefs {
    public static final int MENU = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;
    public static final int GAME_OVER = 3;
    public static final int RAM_BIRD = 0;
    public static final int SPLIT_BIRD = 1;
    public static final int BOMB_BIRD = 2;
    public static final int DRILL_BIRD = 3;
    public static final int SHIELD_BIRD = 4;
    public static final int MAT_WOOD = 0;
    public static final int MAT_STONE = 1;
    public static final int MAT_GLASS = 2;
    public static final int MAT_METAL = 3;

    public static final String[] LEVEL_NAMES = {
            "Wooden Outpost",
            "Glass Corridor",
            "Stone Bastion",
            "Crosswind Ridge",
            "Shield Citadel"
    };

    private GameDefs() {
    }
}
