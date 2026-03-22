package com.android.boot.engine;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.boot.audio.ToneFx;
import com.android.boot.model.BallType;
import com.android.boot.model.EnemyType;
import com.android.boot.model.GameSnapshot;
import com.android.boot.model.LevelDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GameEngine {
    public static final int ROWS = 5;
    public static final int COLS = 9;
    public static final String STATE_MENU = "MENU";
    public static final String STATE_PLAYING = "PLAYING";
    public static final String STATE_PAUSED = "PAUSED";
    public static final String STATE_GAME_OVER = "GAME_OVER";
    public static final String STATE_LEVEL_CLEAR = "LEVEL_CLEAR";
    private static final String PREFS = "bowling_barrage_prefs";
    private static final String KEY_UNLOCKED = "unlocked_levels";
    private static final String KEY_STARS_PREFIX = "stars_";
    private static final float BOARD_TOP_RATIO = 0.24f;
    private static final float BOARD_BOTTOM_RATIO = 0.92f;
    private static final float BOARD_LEFT_RATIO = 0.08f;
    private static final float BOARD_RIGHT_RATIO = 0.92f;
    private static final float CONVEYOR_TOP_RATIO = 0.08f;
    private static final float CONVEYOR_BOTTOM_RATIO = 0.19f;
    private static final float BALL_SPEED = 6.5f;
    private static final float BOUNCE_VERTICAL_SPEED = 1.38f;
    private static final float GIANT_SPEED = 8.8f;
    private static final int MAX_RICOCHETS = 6;
    private final SharedPreferences preferences;
    private final Random random = new Random(3206L);
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Ball> balls = new ArrayList<>();
    private final ArrayList<Fx> fxList = new ArrayList<>();
    private final GameSnapshot snapshot = new GameSnapshot();
    private final LevelDef[] levels;
    private ToneFx audio;
    private String state = STATE_MENU;
    private int selectedLevel;
    private int menuSelectedLevel;
    private int unlockedLevels;
    private int score;
    private int integrity;
    private int spawnIndex;
    private float elapsed;
    private float conveyorCooldown;
    private float conveyorPulse;
    private boolean conveyorReady = true;
    private boolean ballArmed;
    private BallType armedBall = BallType.NORMAL;
    private BallType currentBall = BallType.NORMAL;
    private final BallType[] nextBalls = new BallType[] {BallType.NORMAL, BallType.NORMAL, BallType.NORMAL};
    private float laneHighlightTimer;
    private int highlightedRow = -1;
    private float shakeTime;
    private float shakeStrength;
    private boolean clearSuccess;

    public GameEngine(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        unlockedLevels = Math.max(1, preferences.getInt(KEY_UNLOCKED, 1));
        levels = createLevels();
        refillQueue();
        updateSnapshot();
    }

    public void setAudio(ToneFx audio) {
        this.audio = audio;
        snapshot.muted = audio != null && audio.isMuted();
    }

    public void update(float dt) {
        float clamped = Math.max(0f, Math.min(0.033f, dt));
        conveyorPulse += clamped * 5.6f;
        if (laneHighlightTimer > 0f) {
            laneHighlightTimer = Math.max(0f, laneHighlightTimer - clamped);
        }
        if (shakeTime > 0f) {
            shakeTime = Math.max(0f, shakeTime - clamped);
            if (shakeTime == 0f) {
                shakeStrength = 0f;
            }
        }
        updateFx(clamped);
        if (STATE_PLAYING.equals(state)) {
            elapsed += clamped;
            updateSpawns();
            updateConveyor(clamped);
            updateBalls(clamped);
            updateEnemies(clamped);
            checkLevelOutcome();
        }
        updateSnapshot();
    }

    public void handleTap(float xPx, float yPx, float width, float height) {
        if (!STATE_PLAYING.equals(state)) {
            return;
        }
        if (isTapInConveyor(xPx, yPx, width, height)) {
            armBall();
            return;
        }
        int row = rowForTap(yPx, height);
        if (row >= 0) {
            highlightedRow = row;
            laneHighlightTimer = 0.24f;
            if (ballArmed) {
                launchBall(row);
            }
        }
    }

    public void startMenuLevel(int levelIndex) {
        menuSelectedLevel = Math.max(0, Math.min(levels.length - 1, levelIndex));
        updateSnapshot();
    }

    public void startSelectedLevel() {
        startLevel(menuSelectedLevel);
    }

    public void startLevel(int levelIndex) {
        selectedLevel = Math.max(0, Math.min(levels.length - 1, levelIndex));
        menuSelectedLevel = selectedLevel;
        enemies.clear();
        balls.clear();
        fxList.clear();
        score = 0;
        spawnIndex = 0;
        elapsed = 0f;
        integrity = levels[selectedLevel].baseIntegrity;
        conveyorCooldown = 0f;
        conveyorReady = true;
        ballArmed = false;
        laneHighlightTimer = 0f;
        highlightedRow = -1;
        shakeTime = 0f;
        shakeStrength = 0f;
        clearSuccess = false;
        refillQueue();
        state = STATE_PLAYING;
        updateSnapshot();
    }

    public void restartLevel() {
        startLevel(selectedLevel);
    }

    public void goToMenu() {
        state = STATE_MENU;
        enemies.clear();
        balls.clear();
        fxList.clear();
        ballArmed = false;
        clearSuccess = false;
        updateSnapshot();
    }

    public void pause() {
        if (STATE_PLAYING.equals(state)) {
            state = STATE_PAUSED;
            updateSnapshot();
        }
    }

    public void resume() {
        if (STATE_PAUSED.equals(state)) {
            state = STATE_PLAYING;
            updateSnapshot();
        }
    }

    public void toggleMuted() {
        if (audio != null) {
            audio.setMuted(!audio.isMuted());
        }
        updateSnapshot();
    }

    public boolean isMuted() {
        return audio != null && audio.isMuted();
    }

    public void startNextLevelOrMenu() {
        if (clearSuccess && selectedLevel + 1 < levels.length && isLevelUnlocked(selectedLevel + 1)) {
            startLevel(selectedLevel + 1);
        } else {
            goToMenu();
        }
    }

    public int getLevelCount() {
        return levels.length;
    }

    public boolean isLevelUnlocked(int index) {
        return index + 1 <= unlockedLevels;
    }

    public int getLevelStars(int index) {
        return preferences.getInt(KEY_STARS_PREFIX + (index + 1), 0);
    }

    public String getLevelName(int index) {
        return levels[index].name;
    }

    public int getMenuSelectedLevel() {
        return menuSelectedLevel;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public List<Fx> getFxList() {
        return fxList;
    }

    public BallType getCurrentBall() {
        return currentBall;
    }

    public BallType[] getNextBalls() {
        return nextBalls;
    }

    public boolean isConveyorReady() {
        return conveyorReady;
    }

    public boolean isBallArmed() {
        return ballArmed;
    }

    public int getHighlightedRow() {
        return highlightedRow;
    }

    public float getLaneHighlightAlpha() {
        return laneHighlightTimer / 0.24f;
    }

    public float getConveyorPulse() {
        return conveyorPulse;
    }

    public float getConveyorCooldownProgress() {
        if (conveyorReady) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, conveyorCooldown / 1.12f));
    }

    public float getShakeOffsetX() {
        if (shakeTime <= 0f) {
            return 0f;
        }
        return (float) Math.sin(elapsed * 90f) * shakeStrength;
    }

    public float getShakeOffsetY() {
        if (shakeTime <= 0f) {
            return 0f;
        }
        return (float) Math.cos(elapsed * 76f) * shakeStrength * 0.6f;
    }

    public GameSnapshot getSnapshot() {
        return snapshot;
    }

    private void updateConveyor(float dt) {
        if (!conveyorReady) {
            conveyorCooldown += dt;
            if (conveyorCooldown >= 1.12f) {
                conveyorCooldown = 1.12f;
                conveyorReady = true;
            }
        }
    }

    private void armBall() {
        if (conveyorReady && !ballArmed) {
            ballArmed = true;
            armedBall = currentBall;
            if (audio != null) {
                audio.playArm();
            }
            createPopupFx(0.18f, 0.2f, 1.6f, 0);
        }
    }

    private void launchBall(int row) {
        Ball ball = new Ball();
        ball.type = armedBall;
        ball.row = row;
        ball.x = 0.28f;
        ball.y = row + 0.5f;
        ball.vx = armedBall == BallType.GIANT ? GIANT_SPEED : BALL_SPEED;
        ball.vy = 0f;
        ball.scale = armedBall.scale;
        ball.durability = armedBall == BallType.NORMAL ? MAX_RICOCHETS : 1;
        balls.add(ball);
        if (audio != null) {
            audio.playLaunch();
        }
        ballArmed = false;
        conveyorReady = false;
        conveyorCooldown = 0f;
        currentBall = nextBalls[0];
        nextBalls[0] = nextBalls[1];
        nextBalls[1] = nextBalls[2];
        nextBalls[2] = rollBallType();
    }

    private void updateSpawns() {
        LevelDef level = levels[selectedLevel];
        while (spawnIndex < level.spawnTimes.length && elapsed >= level.spawnTimes[spawnIndex]) {
            Enemy enemy = new Enemy();
            enemy.type = level.spawnTypes[spawnIndex];
            enemy.row = level.spawnRows[spawnIndex];
            enemy.x = COLS + 0.4f + (spawnIndex % 3) * 0.18f;
            enemy.hp = enemy.type.hp;
            enemy.maxHp = enemy.type.hp;
            enemy.flash = 0f;
            enemies.add(enemy);
            spawnIndex++;
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.flash = Math.max(0f, enemy.flash - dt * 4.2f);
            enemy.hitScale = Math.max(0f, enemy.hitScale - dt * 3.8f);
            enemy.x -= enemy.type.speed * dt;
            if (enemy.x <= -0.28f) {
                enemies.remove(i);
                integrity--;
                comboReset();
                createLaneFx(enemy.row, Fx.TYPE_WARNING, 0.34f, 0);
                if (audio != null) {
                    audio.playDanger();
                }
                if (integrity <= 0) {
                    clearSuccess = false;
                    state = STATE_GAME_OVER;
                    shake(0.28f, 20f);
                    return;
                }
            }
        }
    }

    private void updateBalls(float dt) {
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball ball = balls.get(i);
            ball.glow += dt * 7f;
            ball.hitPulse = Math.max(0f, ball.hitPulse - dt * 4.5f);
            ball.x += ball.vx * dt;
            ball.y += ball.vy * dt;
            if (ball.vy != 0f) {
                if (ball.y <= 0.28f) {
                    ball.y = 0.28f;
                    ball.vy = Math.abs(ball.vy);
                    createLaneFx(0, Fx.TYPE_SPARK, 0.18f, 1);
                } else if (ball.y >= ROWS - 0.28f) {
                    ball.y = ROWS - 0.28f;
                    ball.vy = -Math.abs(ball.vy);
                    createLaneFx(ROWS - 1, Fx.TYPE_SPARK, 0.18f, -1);
                }
            }
            boolean removeBall = false;
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                if (Math.abs(enemy.row + 0.5f - ball.y) <= 0.48f && Math.abs(enemy.x - ball.x) <= 0.38f) {
                    removeBall = resolveHit(ball, enemy, j);
                    break;
                }
            }
            if (!removeBall && ball.type == BallType.GIANT) {
                damageRowSegment(ball);
            }
            if (ball.x >= COLS + 0.72f || removeBall) {
                balls.remove(i);
            }
        }
    }

    private boolean resolveHit(Ball ball, Enemy enemy, int enemyIndex) {
        enemy.flash = 1f;
        enemy.hitScale = 1f;
        if (ball.type == BallType.NORMAL) {
            enemy.hp -= 1;
            createHitFx(enemy.row + 0.5f, enemy.x, 1);
            if (audio != null) {
                audio.playHit();
            }
            if (ball.vy == 0f) {
                ball.vy = chooseInitialVerticalDir(enemy.row) * BOUNCE_VERTICAL_SPEED;
            } else {
                ball.vy = -ball.vy;
            }
            ball.durability--;
            ball.hitPulse = 1f;
            if (enemy.hp <= 0) {
                defeatEnemy(enemyIndex, enemy, false);
            }
            return ball.durability <= 0;
        }
        if (ball.type == BallType.BOMB) {
            explode(enemy.row, Math.round(enemy.x));
            if (audio != null) {
                audio.playBlast();
            }
            shake(0.18f, 12f);
            return true;
        }
        defeatEnemy(enemyIndex, enemy, true);
        if (audio != null) {
            audio.playBlast();
        }
        shake(0.16f, 8f);
        return false;
    }

    private void damageRowSegment(Ball ball) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.row == ball.row && enemy.x <= ball.x + 0.22f && enemy.x >= ball.x - 0.72f) {
                defeatEnemy(i, enemy, true);
            }
        }
        createLaneFx(ball.row, Fx.TYPE_GIANT_SWEEP, 0.16f, 0);
    }

    private void explode(int centerRow, int centerCol) {
        Fx fx = new Fx();
        fx.type = Fx.TYPE_EXPLOSION;
        fx.duration = 0.44f;
        fx.time = 0f;
        fx.rowLike = centerRow + 0.5f;
        fx.colLike = centerCol + 0.5f;
        fx.value = 0;
        fxList.add(fx);
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            int col = Math.round(enemy.x);
            if (Math.abs(enemy.row - centerRow) <= 1 && Math.abs(col - centerCol) <= 1) {
                enemy.hp -= 3;
                createHitFx(enemy.row + 0.5f, enemy.x, 3);
                if (enemy.hp <= 0) {
                    defeatEnemy(i, enemy, true);
                } else {
                    enemy.flash = 1f;
                }
            }
        }
    }

    private void defeatEnemy(int index, Enemy enemy, boolean heavyImpact) {
        enemies.remove(index);
        score += enemy.type.scoreValue;
        createPopupFx(enemy.row + 0.2f, enemy.x, enemy.type.scoreValue, 0);
        createHitFx(enemy.row + 0.5f, enemy.x, heavyImpact ? 3 : 1);
        if (heavyImpact) {
            shake(0.12f, enemy.type == EnemyType.TANK ? 9f : 6f);
        }
    }

    private void createHitFx(float rowLike, float colLike, int value) {
        Fx fx = new Fx();
        fx.type = Fx.TYPE_DAMAGE_POPUP;
        fx.duration = 0.5f;
        fx.rowLike = rowLike;
        fx.colLike = colLike;
        fx.value = value;
        fxList.add(fx);
        for (int i = 0; i < 6; i++) {
            Fx spark = new Fx();
            spark.type = Fx.TYPE_PARTICLE;
            spark.duration = 0.28f + i * 0.015f;
            spark.rowLike = rowLike;
            spark.colLike = colLike;
            spark.value = i;
            fxList.add(spark);
        }
    }

    private void createPopupFx(float rowLike, float colLike, int value, int type) {
        Fx fx = new Fx();
        fx.type = type == 0 ? Fx.TYPE_SCORE_POPUP : type;
        fx.duration = 0.46f;
        fx.rowLike = rowLike;
        fx.colLike = colLike;
        fx.value = value;
        fxList.add(fx);
    }

    private void createLaneFx(int row, int type, float duration, int value) {
        Fx fx = new Fx();
        fx.type = type;
        fx.duration = duration;
        fx.rowLike = row + 0.5f;
        fx.colLike = 0.15f;
        fx.value = value;
        fxList.add(fx);
    }

    private void updateFx(float dt) {
        for (int i = fxList.size() - 1; i >= 0; i--) {
            Fx fx = fxList.get(i);
            fx.time += dt;
            if (fx.time >= fx.duration) {
                fxList.remove(i);
            }
        }
    }

    private void checkLevelOutcome() {
        if (spawnIndex >= levels[selectedLevel].spawnTimes.length && enemies.isEmpty() && balls.isEmpty()) {
            clearSuccess = true;
            state = STATE_LEVEL_CLEAR;
            if (audio != null) {
                audio.playSuccess();
            }
            saveProgress();
        }
    }

    private void saveProgress() {
        SharedPreferences.Editor editor = preferences.edit();
        int stars = computeStars();
        int oldStars = preferences.getInt(KEY_STARS_PREFIX + (selectedLevel + 1), 0);
        if (stars > oldStars) {
            editor.putInt(KEY_STARS_PREFIX + (selectedLevel + 1), stars);
        }
        if (selectedLevel + 2 > unlockedLevels && selectedLevel + 1 < levels.length) {
            unlockedLevels = selectedLevel + 2;
            editor.putInt(KEY_UNLOCKED, unlockedLevels);
        }
        editor.apply();
    }

    private int computeStars() {
        int stars = clearSuccess ? 1 : 0;
        if (integrity >= Math.max(1, levels[selectedLevel].baseIntegrity - 1)) {
            stars++;
        }
        if (score >= levels[selectedLevel].targetScore) {
            stars++;
        }
        return Math.min(3, stars);
    }

    private void refillQueue() {
        currentBall = rollBallType();
        nextBalls[0] = rollBallType();
        nextBalls[1] = rollBallType();
        nextBalls[2] = rollBallType();
        armedBall = currentBall;
    }

    private BallType rollBallType() {
        int value = random.nextInt(100);
        if (value < 80) {
            return BallType.NORMAL;
        }
        if (value < 90) {
            return BallType.BOMB;
        }
        return BallType.GIANT;
    }

    private void comboReset() {
        highlightedRow = -1;
        laneHighlightTimer = 0f;
    }

    private void shake(float duration, float strength) {
        shakeTime = Math.max(shakeTime, duration);
        shakeStrength = Math.max(shakeStrength, strength);
    }

    private float chooseInitialVerticalDir(int row) {
        if (row <= 1) {
            return 1f;
        }
        if (row >= 3) {
            return -1f;
        }
        return random.nextBoolean() ? 1f : -1f;
    }

    private boolean isTapInConveyor(float xPx, float yPx, float width, float height) {
        float top = height * CONVEYOR_TOP_RATIO;
        float bottom = height * CONVEYOR_BOTTOM_RATIO;
        float left = width * BOARD_LEFT_RATIO;
        float right = width * BOARD_LEFT_RATIO + width * 0.18f;
        return xPx >= left && xPx <= right && yPx >= top && yPx <= bottom;
    }

    private int rowForTap(float yPx, float height) {
        float top = height * BOARD_TOP_RATIO;
        float bottom = height * BOARD_BOTTOM_RATIO;
        if (yPx < top || yPx > bottom) {
            return -1;
        }
        float rowHeight = (bottom - top) / ROWS;
        return Math.max(0, Math.min(ROWS - 1, (int) ((yPx - top) / rowHeight)));
    }

    private void updateSnapshot() {
        snapshot.state = state;
        snapshot.levelIndex = menuSelectedLevel + 1;
        snapshot.levelName = levels[menuSelectedLevel].name;
        if (!STATE_MENU.equals(state)) {
            snapshot.levelIndex = selectedLevel + 1;
            snapshot.levelName = levels[selectedLevel].name;
        }
        snapshot.score = score;
        snapshot.integrity = Math.max(integrity, 0);
        snapshot.waveIndex = getWaveIndex();
        snapshot.queueText = queueText();
        snapshot.unlockedLevels = unlockedLevels;
        snapshot.muted = audio != null && audio.isMuted();
        snapshot.clearSuccess = clearSuccess;
        snapshot.resultTitle = clearSuccess ? "Level Clear" : "Game Over";
        snapshot.resultBody = "Score " + score + "   Target " + levels[Math.max(0, STATE_MENU.equals(state) ? menuSelectedLevel : selectedLevel)].targetScore + "   Stars " + computeStars();
        snapshot.conveyorReady = conveyorReady;
        snapshot.ballArmed = ballArmed;
    }

    private int getWaveIndex() {
        LevelDef level = levels[Math.max(0, STATE_MENU.equals(state) ? menuSelectedLevel : selectedLevel)];
        int wave = 1;
        for (int start : level.waveStarts) {
            if (spawnIndex > start) {
                wave++;
            }
        }
        return wave;
    }

    private String queueText() {
        return currentBall.label + "  " + nextBalls[0].label + "  " + nextBalls[1].label + "  " + nextBalls[2].label;
    }

    private LevelDef[] createLevels() {
        return new LevelDef[] {
                new LevelDef("Warm Up Roll", 4, 900,
                        new float[] {0.9f, 1.8f, 2.6f, 3.5f, 4.4f, 5.2f, 6.1f, 7.0f, 8.0f},
                        new int[] {2, 1, 3, 0, 4, 2, 1, 3, 2},
                        new EnemyType[] {EnemyType.SCOUT, EnemyType.SCOUT, EnemyType.SCOUT, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER},
                        new int[] {2, 5, 7}),
                new LevelDef("Split Spare", 4, 1600,
                        new float[] {0.7f, 1.2f, 1.6f, 2.5f, 3.0f, 3.6f, 4.2f, 4.9f, 5.4f, 6.0f, 6.8f, 7.6f},
                        new int[] {0, 4, 2, 1, 3, 0, 4, 2, 1, 3, 2, 2},
                        new EnemyType[] {EnemyType.SCOUT, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.TANK},
                        new int[] {2, 5, 9}),
                new LevelDef("Bruiser March", 5, 2500,
                        new float[] {0.6f, 1.1f, 1.8f, 2.3f, 2.9f, 3.5f, 4.1f, 4.8f, 5.4f, 6.0f, 6.8f, 7.6f, 8.5f},
                        new int[] {2, 2, 1, 3, 0, 4, 2, 1, 3, 2, 0, 4, 2},
                        new EnemyType[] {EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.SCOUT, EnemyType.TANK, EnemyType.BRUISER, EnemyType.BRUISER, EnemyType.TANK, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.TANK},
                        new int[] {3, 7, 10}),
                new LevelDef("Heavy Mix", 5, 3600,
                        new float[] {0.5f, 0.9f, 1.4f, 1.8f, 2.3f, 2.8f, 3.4f, 4.0f, 4.6f, 5.2f, 5.8f, 6.4f, 7.0f, 7.7f, 8.4f, 9.2f},
                        new int[] {0, 2, 4, 1, 3, 2, 0, 4, 1, 3, 2, 0, 4, 1, 3, 2},
                        new EnemyType[] {EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.BRUISER, EnemyType.BRUISER, EnemyType.TANK, EnemyType.SCOUT, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.BRUISER, EnemyType.SCOUT, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.BRUISER},
                        new int[] {4, 8, 12}),
                new LevelDef("Tank Stampede", 6, 5200,
                        new float[] {0.5f, 0.9f, 1.3f, 1.7f, 2.1f, 2.6f, 3.0f, 3.5f, 4.0f, 4.6f, 5.2f, 5.8f, 6.5f, 7.1f, 7.8f, 8.6f, 9.3f, 10.0f},
                        new int[] {2, 0, 4, 1, 3, 2, 0, 4, 1, 3, 2, 0, 4, 1, 3, 2, 1, 3},
                        new EnemyType[] {EnemyType.BRUISER, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.SCOUT, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.TANK, EnemyType.BRUISER, EnemyType.TANK, EnemyType.TANK, EnemyType.TANK},
                        new int[] {4, 9, 13})
        };
    }

    public static final class Enemy {
        public EnemyType type;
        public int row;
        public float x;
        public int hp;
        public int maxHp;
        public float flash;
        public float hitScale;
    }

    public static final class Ball {
        public BallType type;
        public int row;
        public float x;
        public float y;
        public float vx;
        public float vy;
        public float glow;
        public float hitPulse;
        public float scale;
        public int durability;
    }

    public static final class Fx {
        public static final int TYPE_DAMAGE_POPUP = 1;
        public static final int TYPE_SCORE_POPUP = 2;
        public static final int TYPE_PARTICLE = 3;
        public static final int TYPE_EXPLOSION = 4;
        public static final int TYPE_WARNING = 5;
        public static final int TYPE_GIANT_SWEEP = 6;
        public static final int TYPE_SPARK = 7;
        public int type;
        public float duration;
        public float time;
        public float rowLike;
        public float colLike;
        public int value;
    }
}
