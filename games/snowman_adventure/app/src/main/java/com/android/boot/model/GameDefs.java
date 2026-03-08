package com.android.boot.model;

public final class GameDefs {
    public static final int STATE_MENU = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_GAME_OVER = 3;
    public static final int STATE_LEVEL_SELECT = 4;
    public static final int ENEMY_WALKER = 0;
    public static final int ENEMY_HOPPER = 1;
    public static final int ENEMY_FLYER = 2;
    public static final int ENEMY_SHIELD = 3;
    public static final int ENEMY_MINI_BOSS = 4;
    public static final String[] LEVEL_NAMES = {
            "First Snowfield",
            "Slippery Steps",
            "Broken Bridge",
            "Wind Tunnel",
            "Spring Cave",
            "Frozen Factory",
            "Icicle Hall",
            "Mirror Basin",
            "Blizzard Tower",
            "Snow King Keep"
    };

    private GameDefs() {
    }
}
