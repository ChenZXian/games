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
    private static final float ENEMY_SPEED = 0.48f;
    private static final float DROP_READY_TIME = 1.35f;
    private static final float BALL_SPEED = 7.2f;
    private static final float GIANT_SPEED = 9.4f;
    private static final int MAX_BOUNCES = 5;
    private static final String PREFS = "bowling_battle_prefs";
    private static final String KEY_UNLOCKED = "unlocked_levels";
    private static final String KEY_STARS = "stars_";
    private final SharedPreferences preferences;
    private final LevelDef[] levels;
    private final Random random = new Random(41L);
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Ball> balls = new ArrayList<>();
    private final ArrayList<Fx> fxList = new ArrayList<>();
    private final GameSnapshot snapshot = new GameSnapshot();
    private ToneFx audio;
    private String state = STATE_MENU;
    private int selectedLevel;
    private int unlockedLevels;
    private int score;
    private int combo;
    private int bestCombo;
    private int remainingLife;
    private float elapsed;
    private int spawnIndex;
    private float conveyorTimer = DROP_READY_TIME;
    private float conveyorRoll;
    private boolean conveyorReady = true;
    private BallType currentBall = BallType.NORMAL;
    private final BallType[] nextBalls = new BallType[] {BallType.NORMAL, BallType.NORMAL, BallType.NORMAL};
    private float comboTimer;
    private float resultStarTimer;
    private boolean clearSuccess;

    public GameEngine(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        unlockedLevels = Math.max(1, preferences.getInt(KEY_UNLOCKED, 1));
        levels = createLevels();
        refillConveyor();
        updateSnapshot();
    }

    public void setAudio(ToneFx audio) {
        this.audio = audio;
        snapshot.muted = audio != null && audio.isMuted();
    }

    public void update(float dt) {
        float clamped = Math.min(0.033f, Math.max(0.0f, dt));
        conveyorRoll += clamped * 3.2f;
        if (STATE_PLAYING.equals(state)) {
            elapsed += clamped;
            updateSpawns();
            updateConveyor(clamped);
            updateEnemies(clamped);
            updateBalls(clamped);
            updateFx(clamped);
            updateCombo(clamped);
            resolveLevelEnd();
        } else {
            updateFx(clamped);
            if (STATE_LEVEL_CLEAR.equals(state)) {
                resultStarTimer = Math.min(1f, resultStarTimer + clamped * 1.2f);
            }
        }
        updateSnapshot();
    }

    public void startLevel(int levelIndex) {
        selectedLevel = Math.max(0, Math.min(levels.length - 1, levelIndex));
        enemies.clear();
        balls.clear();
        fxList.clear();
        score = 0;
        combo = 0;
        bestCombo = 0;
        elapsed = 0f;
        spawnIndex = 0;
        conveyorTimer = DROP_READY_TIME;
        conveyorReady = true;
        remainingLife = levels[selectedLevel].life;
        clearSuccess = false;
        resultStarTimer = 0f;
        refillConveyor();
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
        updateSnapshot();
    }

    public void pause() {
        if (STATE_PLAYING.equals(state)) {
            state = STATE_PAUSED;
        }
        updateSnapshot();
    }

    public void resume() {
        if (STATE_PAUSED.equals(state)) {
            state = STATE_PLAYING;
        }
        updateSnapshot();
    }

    public void toggleMuted() {
        if (audio != null) {
            audio.setMuted(!audio.isMuted());
            snapshot.muted = audio.isMuted();
        }
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

    public void onRowTapped(float yPx, float width, float height) {
        if (!STATE_PLAYING.equals(state) || !conveyorReady) {
            return;
        }
        float boardTop = height * 0.23f;
        float boardBottom = height * 0.92f;
        if (yPx < boardTop || yPx > boardBottom) {
            return;
        }
        float rowHeight = (boardBottom - boardTop) / ROWS;
        int row = Math.max(0, Math.min(ROWS - 1, (int) ((yPx - boardTop) / rowHeight)));
        dropBall(row, width, height);
    }

    private void dropBall(int row, float width, float height) {
        Ball ball = new Ball();
        ball.type = currentBall;
        ball.x = -0.35f;
        ball.y = row + 0.5f;
        ball.row = row;
        ball.verticalDir = row >= ROWS / 2 ? -1 : 1;
        ball.bouncesRemaining = MAX_BOUNCES;
        ball.scale = 1f;
        ball.glow = 0f;
        balls.add(ball);
        if (audio != null) {
            audio.playDrop();
        }
        currentBall = nextBalls[0];
        nextBalls[0] = nextBalls[1];
        nextBalls[1] = nextBalls[2];
        nextBalls[2] = rollBallType();
        conveyorReady = false;
        conveyorTimer = 0f;
        createFx(Fx.TYPE_POPUP, 0.65f, row + 0.12f, 0.2f, ball.type.ordinal() + 1);
    }

    private void updateConveyor(float dt) {
        if (!conveyorReady) {
            conveyorTimer += dt;
            if (conveyorTimer >= DROP_READY_TIME) {
                conveyorTimer = DROP_READY_TIME;
                conveyorReady = true;
            }
        }
    }

    private void updateSpawns() {
        LevelDef level = levels[selectedLevel];
        while (spawnIndex < level.spawnTimes.length && elapsed >= level.spawnTimes[spawnIndex]) {
            Enemy enemy = new Enemy();
            enemy.type = level.spawnTypes[spawnIndex];
            enemy.row = level.spawnRows[spawnIndex];
            enemy.hp = enemy.type.hp;
            enemy.maxHp = enemy.type.hp;
            enemy.x = COLS - 0.15f + (spawnIndex % 2) * 0.35f;
            enemies.add(enemy);
            spawnIndex++;
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.flash = Math.max(0f, enemy.flash - dt * 3f);
            enemy.x -= ENEMY_SPEED * dt;
            if (enemy.x <= -0.45f) {
                enemies.remove(i);
                remainingLife--;
                combo = 0;
                comboTimer = 0f;
                createFx(Fx.TYPE_WARNING, 0.8f, enemy.row + 0.45f, 0.35f, 0);
                if (remainingLife <= 0) {
                    failLevel();
                    return;
                }
            }
        }
    }

    private void updateBalls(float dt) {
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball ball = balls.get(i);
            ball.glow += dt * 4.5f;
            ball.scale = 1f + Math.max(0f, ball.hitPulse) * 0.18f;
            ball.hitPulse = Math.max(0f, ball.hitPulse - dt * 4f);
            float speed = ball.type == BallType.GIANT ? GIANT_SPEED : BALL_SPEED;
            ball.x += speed * dt;
            if (ball.type == BallType.NORMAL) {
                ball.y += ball.verticalDir * dt * 1.6f;
                if (ball.y <= 0.25f) {
                    ball.y = 0.25f;
                    ball.verticalDir = 1;
                    createFx(Fx.TYPE_ARROW, 0.25f, ball.y, ball.x, 1);
                } else if (ball.y >= ROWS - 0.25f) {
                    ball.y = ROWS - 0.25f;
                    ball.verticalDir = -1;
                    createFx(Fx.TYPE_ARROW, 0.25f, ball.y, ball.x, -1);
                }
            }
            boolean collided = false;
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                if (Math.abs(enemy.row + 0.5f - ball.y) < 0.46f && Math.abs(enemy.x - ball.x) < 0.42f) {
                    collided = true;
                    hitEnemy(ball, enemy, j);
                    if (ball.type == BallType.NORMAL) {
                        ball.verticalDir *= -1;
                        ball.bouncesRemaining--;
                        ball.hitPulse = 1f;
                        createFx(Fx.TYPE_ARROW, 0.35f, ball.y, ball.x, ball.verticalDir);
                    }
                    break;
                }
            }
            if (ball.type == BallType.GIANT) {
                clearRowHits(ball);
            }
            if (ball.x > COLS + 0.7f || (ball.type == BallType.NORMAL && ball.bouncesRemaining <= 0 && collided)) {
                balls.remove(i);
            } else if (ball.type == BallType.BOMB && collided) {
                balls.remove(i);
            }
        }
    }

    private void clearRowHits(Ball ball) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.row == ball.row && enemy.x <= ball.x + 0.2f && enemy.x >= ball.x - 0.55f) {
                killEnemy(i, enemy, true);
            }
        }
        createFx(Fx.TYPE_GIANT, 0.18f, ball.row + 0.5f, ball.x, 0);
    }

    private void hitEnemy(Ball ball, Enemy enemy, int enemyIndex) {
        enemy.flash = 1f;
        if (ball.type == BallType.NORMAL) {
            enemy.hp -= 1;
            createFx(Fx.TYPE_HIT, 0.22f, enemy.row + 0.5f, enemy.x, 0);
            if (audio != null) {
                audio.playHit();
            }
            if (enemy.hp <= 0) {
                killEnemy(enemyIndex, enemy, false);
            }
        } else if (ball.type == BallType.BOMB) {
            explodeAt(enemy.row, Math.round(enemy.x));
            if (audio != null) {
                audio.playBlast();
            }
        } else if (ball.type == BallType.GIANT) {
            killEnemy(enemyIndex, enemy, true);
            if (audio != null) {
                audio.playBlast();
            }
        }
    }

    private void explodeAt(int centerRow, int centerCol) {
        createFx(Fx.TYPE_EXPLOSION, 0.55f, centerRow + 0.5f, centerCol + 0.5f, 0);
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            int col = Math.round(enemy.x);
            if (Math.abs(enemy.row - centerRow) <= 1 && Math.abs(col - centerCol) <= 1) {
                killEnemy(i, enemy, true);
            }
        }
    }

    private void killEnemy(int index, Enemy enemy, boolean instant) {
        enemies.remove(index);
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        comboTimer = 1.4f;
        int gain = 120 * enemy.maxHp + combo * 10;
        score += gain;
        createFx(Fx.TYPE_POPUP, 0.7f, enemy.row + 0.1f, enemy.x, gain);
        createFx(Fx.TYPE_HIT, 0.3f, enemy.row + 0.5f, enemy.x, instant ? 1 : 0);
        if (audio != null && !instant) {
            audio.playHit();
        }
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

    private void updateCombo(float dt) {
        if (comboTimer > 0f) {
            comboTimer -= dt;
            if (comboTimer <= 0f) {
                combo = 0;
            }
        }
    }

    private void resolveLevelEnd() {
        if (spawnIndex >= levels[selectedLevel].spawnTimes.length && enemies.isEmpty() && balls.isEmpty()) {
            clearSuccess = true;
            state = STATE_LEVEL_CLEAR;
            int stars = computeStars();
            resultStarTimer = 0f;
            if (audio != null) {
                audio.playSuccess();
            }
            saveProgress(stars);
        }
    }

    private void failLevel() {
        clearSuccess = false;
        state = STATE_GAME_OVER;
        resultStarTimer = 0f;
        if (audio != null) {
            audio.playFail();
        }
    }

    private void saveProgress(int stars) {
        int levelNumber = selectedLevel + 1;
        int storedStars = preferences.getInt(KEY_STARS + levelNumber, 0);
        SharedPreferences.Editor editor = preferences.edit();
        if (stars > storedStars) {
            editor.putInt(KEY_STARS + levelNumber, stars);
        }
        if (selectedLevel + 2 > unlockedLevels && selectedLevel + 1 < levels.length) {
            unlockedLevels = selectedLevel + 2;
            editor.putInt(KEY_UNLOCKED, unlockedLevels);
        }
        editor.apply();
    }

    private int computeStars() {
        int stars = clearSuccess ? 1 : 0;
        if (remainingLife >= Math.max(2, levels[selectedLevel].life - 1)) {
            stars++;
        }
        if (score >= levels[selectedLevel].targetScore || bestCombo >= 5) {
            stars++;
        }
        return Math.min(3, stars);
    }

    private void refillConveyor() {
        currentBall = rollBallType();
        nextBalls[0] = rollBallType();
        nextBalls[1] = rollBallType();
        nextBalls[2] = rollBallType();
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

    private void createFx(int type, float duration, float rowLike, float colLike, int value) {
        Fx fx = new Fx();
        fx.type = type;
        fx.duration = duration;
        fx.rowLike = rowLike;
        fx.colLike = colLike;
        fx.value = value;
        fxList.add(fx);
    }

    private void updateSnapshot() {
        snapshot.state = state;
        snapshot.levelIndex = selectedLevel + 1;
        snapshot.levelName = levels[selectedLevel].name;
        snapshot.score = score;
        snapshot.combo = combo;
        snapshot.waveIndex = getWaveIndex();
        snapshot.remainingLife = Math.max(remainingLife, 0);
        snapshot.unlockedLevels = Math.min(levels.length, Math.max(unlockedLevels, 1));
        snapshot.muted = audio != null && audio.isMuted();
        snapshot.clearSuccess = clearSuccess;
        snapshot.resultStars = STATE_LEVEL_CLEAR.equals(state) ? computeStars() : 0;
        snapshot.resultTitle = STATE_LEVEL_CLEAR.equals(state) ? "Level Clear" : "Game Over";
        snapshot.resultScore = "Score " + score + "  Combo " + bestCombo;
    }

    private int getWaveIndex() {
        LevelDef level = levels[selectedLevel];
        int wave = 1;
        for (int start : level.waveStarts) {
            if (spawnIndex > start) {
                wave++;
            }
        }
        return wave;
    }

    private LevelDef[] createLevels() {
        return new LevelDef[] {
                new LevelDef("First Roll",
                        new float[] {0.8f, 1.9f, 3.2f, 4.1f, 5.5f, 6.2f, 7.1f, 8.4f, 9.4f},
                        new int[] {2, 2, 1, 3, 2, 1, 3, 0, 4},
                        new EnemyType[] {EnemyType.LIGHT, EnemyType.LIGHT, EnemyType.LIGHT, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.LIGHT},
                        new int[] {2, 5}, 4, 1100),
                new LevelDef("Packed Lanes",
                        new float[] {0.7f, 1.1f, 1.5f, 3.0f, 3.4f, 3.8f, 5.1f, 5.5f, 5.9f, 7.6f, 8.0f, 8.4f},
                        new int[] {1, 2, 3, 1, 2, 3, 0, 0, 1, 3, 4, 4},
                        new EnemyType[] {EnemyType.LIGHT, EnemyType.LIGHT, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.LIGHT},
                        new int[] {2, 5, 8}, 4, 1700),
                new LevelDef("Heavy Line",
                        new float[] {0.9f, 1.6f, 2.3f, 3.2f, 4.0f, 4.7f, 5.2f, 6.3f, 7.0f, 7.8f, 8.6f, 9.3f},
                        new int[] {2, 2, 2, 1, 3, 2, 2, 0, 4, 2, 2, 2},
                        new EnemyType[] {EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.HEAVY, EnemyType.MEDIUM},
                        new int[] {3, 6, 9}, 5, 2300),
                new LevelDef("Mixed Chaos",
                        new float[] {0.6f, 1.1f, 1.7f, 2.3f, 3.0f, 3.5f, 4.1f, 4.8f, 5.3f, 6.0f, 6.5f, 7.1f, 7.7f, 8.4f, 9.0f, 9.7f},
                        new int[] {0, 2, 4, 1, 3, 0, 4, 2, 1, 3, 0, 4, 2, 1, 3, 2},
                        new EnemyType[] {EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.LIGHT, EnemyType.HEAVY, EnemyType.MEDIUM},
                        new int[] {3, 7, 11}, 5, 3200),
                new LevelDef("Final Conveyor",
                        new float[] {0.5f, 0.9f, 1.2f, 1.7f, 2.1f, 2.6f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f, 5.6f, 6.1f, 6.7f, 7.1f, 7.7f, 8.2f, 8.7f, 9.2f, 9.7f},
                        new int[] {0, 2, 4, 1, 3, 2, 0, 4, 1, 3, 2, 0, 4, 1, 3, 2, 0, 4, 1, 3},
                        new EnemyType[] {EnemyType.MEDIUM, EnemyType.LIGHT, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.LIGHT, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY, EnemyType.MEDIUM, EnemyType.HEAVY},
                        new int[] {4, 9, 14}, 6, 4300)
        };
    }

    public GameSnapshot getSnapshot() {
        return snapshot;
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

    public float getConveyorProgress() {
        return conveyorReady ? 1f : conveyorTimer / DROP_READY_TIME;
    }

    public float getConveyorRoll() {
        return conveyorRoll;
    }

    public int getLevelCount() {
        return levels.length;
    }

    public String getLevelName(int index) {
        return levels[index].name;
    }

    public int getLevelStars(int index) {
        return preferences.getInt(KEY_STARS + (index + 1), 0);
    }

    public boolean isLevelUnlocked(int index) {
        return index < unlockedLevels;
    }

    public static final class Enemy {
        public EnemyType type;
        public int row;
        public int hp;
        public int maxHp;
        public float x;
        public float flash;
    }

    public static final class Ball {
        public BallType type;
        public int row;
        public float x;
        public float y;
        public int verticalDir;
        public int bouncesRemaining;
        public float scale;
        public float glow;
        public float hitPulse;
    }

    public static final class Fx {
        public static final int TYPE_HIT = 1;
        public static final int TYPE_EXPLOSION = 2;
        public static final int TYPE_POPUP = 3;
        public static final int TYPE_GIANT = 4;
        public static final int TYPE_ARROW = 5;
        public static final int TYPE_WARNING = 6;
        public int type;
        public float duration;
        public float time;
        public float rowLike;
        public float colLike;
        public int value;
    }
}
