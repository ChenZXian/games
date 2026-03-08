package com.android.boot.engine;

import com.android.boot.audio.ToneFx;
import com.android.boot.input.TouchState;
import com.android.boot.model.GameDefs;

import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    public static class Enemy {
        public float x;
        public float y;
        public float vx;
        public float vy;
        public int type;
        public int snowStage;
        public boolean alive = true;
        public boolean snowball;
    }

    public static class Snowball {
        public float x;
        public float y;
        public float vx;
        public float vy;
        public boolean active;
    }

    public static class Level {
        public final String name;
        public final boolean iceFloor;
        public final boolean breakableBlock;
        public final boolean movingPlatform;
        public final boolean windFan;
        public final boolean springPad;
        public final boolean icicleTrap;
        public final boolean portalGate;

        public Level(String name, boolean iceFloor, boolean breakableBlock, boolean movingPlatform, boolean windFan, boolean springPad, boolean icicleTrap, boolean portalGate) {
            this.name = name;
            this.iceFloor = iceFloor;
            this.breakableBlock = breakableBlock;
            this.movingPlatform = movingPlatform;
            this.windFan = windFan;
            this.springPad = springPad;
            this.icicleTrap = icicleTrap;
            this.portalGate = portalGate;
        }
    }

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Snowball> snowballs = new ArrayList<>();
    private final Level[] levels;
    private final ToneFx toneFx;
    private float playerX;
    private float playerY;
    private float playerVx;
    private float playerVy;
    private int state = GameDefs.STATE_MENU;
    private int levelIndex;
    private int unlockedLevel = 1;
    private int combo;
    private int stars;
    private float time;
    private float bobTimer;

    public GameEngine(ToneFx toneFx) {
        this.toneFx = toneFx;
        levels = new Level[]{
                new Level(GameDefs.LEVEL_NAMES[0], false, false, false, false, false, false, true),
                new Level(GameDefs.LEVEL_NAMES[1], true, false, false, false, false, false, true),
                new Level(GameDefs.LEVEL_NAMES[2], true, true, false, false, false, false, true),
                new Level(GameDefs.LEVEL_NAMES[3], true, true, true, true, false, false, true),
                new Level(GameDefs.LEVEL_NAMES[4], true, true, true, false, true, false, true),
                new Level(GameDefs.LEVEL_NAMES[5], true, true, true, true, true, false, true),
                new Level(GameDefs.LEVEL_NAMES[6], true, true, true, true, true, true, true),
                new Level(GameDefs.LEVEL_NAMES[7], true, true, true, true, true, true, true),
                new Level(GameDefs.LEVEL_NAMES[8], true, true, true, true, true, true, true),
                new Level(GameDefs.LEVEL_NAMES[9], true, true, true, true, true, true, true)
        };
    }

    public void setState(int nextState) {
        state = nextState;
    }

    public int getState() {
        return state;
    }

    public void startLevel(int index) {
        levelIndex = Math.max(0, Math.min(index, levels.length - 1));
        combo = 0;
        stars = 0;
        time = 0f;
        playerX = 120f;
        playerY = 360f;
        playerVx = 0f;
        playerVy = 0f;
        enemies.clear();
        snowballs.clear();
        spawnEnemy(GameDefs.ENEMY_WALKER, 500f, 360f);
        spawnEnemy(GameDefs.ENEMY_HOPPER, 620f, 360f);
        spawnEnemy(GameDefs.ENEMY_FLYER, 760f, 280f);
        spawnEnemy(GameDefs.ENEMY_SHIELD, 870f, 360f);
        if (levelIndex >= 7) {
            spawnEnemy(GameDefs.ENEMY_MINI_BOSS, 980f, 320f);
        }
        state = GameDefs.STATE_PLAYING;
    }

    private void spawnEnemy(int type, float x, float y) {
        Enemy enemy = new Enemy();
        enemy.type = type;
        enemy.x = x;
        enemy.y = y;
        enemy.vx = type == GameDefs.ENEMY_FLYER ? -52f : -35f;
        enemy.vy = 0f;
        enemies.add(enemy);
    }

    public void update(float dt, TouchState input) {
        if (state != GameDefs.STATE_PLAYING) {
            return;
        }
        float clamped = Math.max(0f, Math.min(0.033f, dt));
        time += clamped;
        bobTimer += clamped * 4f;
        float target = input.leftHeld ? -170f : 0f;
        float accel = levels[levelIndex].iceFloor ? 360f : 660f;
        if (playerVx < target) {
            playerVx = Math.min(target, playerVx + accel * clamped);
        } else {
            playerVx = Math.max(target, playerVx - accel * clamped);
        }
        if (input.jumpPressed && playerY >= 360f) {
            playerVy = -330f;
        }
        playerVy += 720f * clamped;
        playerX += playerVx * clamped;
        playerY += playerVy * clamped;
        if (playerY > 360f) {
            playerY = 360f;
            playerVy = 0f;
        }
        if (levels[levelIndex].windFan) {
            playerVx += 40f * clamped;
        }
        if (levels[levelIndex].springPad && playerX > 340f && playerX < 390f && playerY >= 360f) {
            playerVy = -420f;
        }
        if (input.sprayHeld) {
            toneFx.playSpray();
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.alive && !enemy.snowball && Math.abs(enemy.x - playerX) < 120f) {
                    enemy.snowStage += enemy.type == GameDefs.ENEMY_SHIELD ? 1 : 2;
                    if (enemy.snowStage >= 6) {
                        enemy.snowball = true;
                        Snowball sb = new Snowball();
                        sb.x = enemy.x;
                        sb.y = enemy.y;
                        sb.vx = 0f;
                        sb.active = true;
                        snowballs.add(sb);
                    }
                }
            }
        }
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (!enemy.alive) {
                continue;
            }
            if (!enemy.snowball) {
                enemy.x += enemy.vx * clamped;
                if (enemy.type == GameDefs.ENEMY_HOPPER && enemy.y >= 360f) {
                    enemy.vy = -230f;
                }
                if (enemy.type == GameDefs.ENEMY_FLYER) {
                    enemy.y = 280f + (float) Math.sin(time * 3f + i) * 40f;
                } else {
                    enemy.vy += 720f * clamped;
                    enemy.y += enemy.vy * clamped;
                    if (enemy.y > 360f) {
                        enemy.y = 360f;
                        enemy.vy = 0f;
                    }
                }
                if (levels[levelIndex].icicleTrap && enemy.x > 650f && enemy.x < 690f) {
                    enemy.alive = false;
                    combo++;
                }
            }
        }
        for (int i = 0; i < snowballs.size(); i++) {
            Snowball sb = snowballs.get(i);
            if (!sb.active) {
                continue;
            }
            if (input.kickPressed && Math.abs(sb.x - playerX) < 70f && Math.abs(sb.y - playerY) < 60f) {
                sb.vx = 420f;
                sb.vy = -120f;
                toneFx.playKick();
            }
            sb.vy += 640f * clamped;
            sb.x += sb.vx * clamped;
            sb.y += sb.vy * clamped;
            if (sb.y > 360f) {
                sb.y = 360f;
                sb.vy = -Math.abs(sb.vy) * 0.35f;
            }
            for (int j = 0; j < enemies.size(); j++) {
                Enemy enemy = enemies.get(j);
                if (enemy.alive && !enemy.snowball && Math.abs(enemy.x - sb.x) < 36f && Math.abs(enemy.y - sb.y) < 50f) {
                    enemy.alive = false;
                    combo++;
                }
            }
        }
        int aliveCount = 0;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive && !enemies.get(i).snowball) {
                aliveCount++;
            }
        }
        if (aliveCount == 0) {
            stars = calculateStars();
            if (levelIndex + 2 > unlockedLevel) {
                unlockedLevel = levelIndex + 2;
            }
            if (unlockedLevel > 10) {
                unlockedLevel = 10;
            }
            toneFx.playClear();
            state = GameDefs.STATE_GAME_OVER;
        }
        input.clearInstant();
    }

    private int calculateStars() {
        int value = 1;
        if (combo >= 3) {
            value++;
        }
        if (time < 55f) {
            value++;
        }
        return value;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Snowball> getSnowballs() {
        return snowballs;
    }

    public float getPlayerX() {
        return playerX;
    }

    public float getPlayerY() {
        return playerY + (float) Math.sin(bobTimer) * 3f;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public int getUnlockedLevel() {
        return unlockedLevel;
    }

    public int getCombo() {
        return combo;
    }

    public int getStars() {
        return stars;
    }

    public String getLevelTitle() {
        return levels[levelIndex].name;
    }
}
