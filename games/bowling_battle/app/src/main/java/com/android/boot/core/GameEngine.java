package com.android.boot.core;

import android.graphics.RectF;

import com.android.boot.entity.BallProjectile;
import com.android.boot.entity.BallType;
import com.android.boot.entity.EnemyCell;
import com.android.boot.entity.EnemyType;
import com.android.boot.entity.ExplosionPulse;
import com.android.boot.entity.FloatingText;

import java.util.Random;

public class GameEngine {
    public static final int ROWS = 5;
    public static final int COLS = 9;
    private static final int PREVIEW_COUNT = 3;
    private static final float ENEMY_SPEED = 0.38f;
    private static final float NORMAL_SPEED = 7.4f;
    private static final float GIANT_SPEED = 9.5f;
    private static final float HIT_EPSILON = 0.32f;
    private final Random random = new Random();
    private final EnemyCell[][] board = new EnemyCell[ROWS][COLS];
    private final BallProjectile ball = new BallProjectile();
    private final ExplosionPulse explosionPulse = new ExplosionPulse();
    private final FloatingText comboText = new FloatingText();
    private final int[] previewBalls = new int[PREVIEW_COUNT];
    private final int[] spawnCursor = new int[8];
    private final float[] spawnTimer = new float[8];
    private final LevelDefinition[] levels = LevelRepository.createLevels();
    private final RectF boardRect = new RectF();
    private GameState gameState = GameState.MENU;
    private LevelDefinition level;
    private int levelIndex;
    private int waveIndex;
    private int life;
    private int score;
    private int combo;
    private int clearedEnemies;
    private int stars;
    private boolean levelCleared;
    private float conveyorTimer;
    private float conveyorCooldown;
    private float conveyorOffset;
    private float glowPulse;
    private float starAnim;
    private float giantFlash;
    private float bounceArrowTimer;
    private int hudScore = Integer.MIN_VALUE;
    private int hudCombo = Integer.MIN_VALUE;
    private int hudWave = Integer.MIN_VALUE;
    private int hudLife = Integer.MIN_VALUE;

    public interface Listener {
        void onHudChanged(int score, int combo, int wave, int life);
        void onGameFinished(boolean cleared, int stars, String summary);
    }

    private Listener listener;

    public GameEngine() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = new EnemyCell();
            }
        }
        refillPreview();
    }

    public void setListener(Listener value) {
        listener = value;
        dispatchHud();
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState value) {
        gameState = value;
    }

    public LevelDefinition[] getLevels() {
        return levels;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public boolean isLevelCleared() {
        return levelCleared;
    }

    public int getStars() {
        return stars;
    }

    public int getCurrentBallType() {
        return previewBalls[0];
    }

    public int getPreviewBallType(int index) {
        return previewBalls[index + 1];
    }

    public EnemyCell[][] getBoard() {
        return board;
    }

    public BallProjectile getBall() {
        return ball;
    }

    public ExplosionPulse getExplosionPulse() {
        return explosionPulse;
    }

    public FloatingText getComboText() {
        return comboText;
    }

    public float getConveyorRatio() {
        return conveyorCooldown <= 0f ? 1f : conveyorTimer / conveyorCooldown;
    }

    public float getConveyorOffset() {
        return conveyorOffset;
    }

    public float getGlowPulse() {
        return glowPulse;
    }

    public float getStarAnim() {
        return starAnim;
    }

    public float getGiantFlash() {
        return giantFlash;
    }

    public float getBounceArrowTimer() {
        return bounceArrowTimer;
    }

    public void setBoardRect(float left, float top, float right, float bottom) {
        boardRect.set(left, top, right, bottom);
    }

    public void startLevel(int index) {
        levelIndex = index;
        level = levels[index];
        waveIndex = 0;
        levelCleared = false;
        score = 0;
        combo = 0;
        clearedEnemies = 0;
        stars = 0;
        conveyorCooldown = level.conveyorCooldown;
        conveyorTimer = conveyorCooldown;
        conveyorOffset = 0f;
        glowPulse = 0f;
        starAnim = 0f;
        giantFlash = 0f;
        bounceArrowTimer = 0f;
        ball.active = false;
        explosionPulse.active = false;
        comboText.active = false;
        life = level.startLife;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col].clear();
            }
        }
        for (int i = 0; i < level.waves.length; i++) {
            spawnCursor[i] = 0;
            spawnTimer[i] = 0f;
        }
        refillPreview();
        gameState = GameState.PLAYING;
        dispatchHud();
    }

    public void update(float delta) {
        if (gameState != GameState.PLAYING || level == null) {
            return;
        }
        if (delta > 0.033f) {
            delta = 0.033f;
        }
        glowPulse += delta * 4f;
        conveyorOffset += delta * 150f;
        if (conveyorOffset > 120f) {
            conveyorOffset -= 120f;
        }
        conveyorTimer += delta;
        if (explosionPulse.active) {
            explosionPulse.life -= delta;
            if (explosionPulse.life <= 0f) {
                explosionPulse.active = false;
            }
        }
        if (comboText.active) {
            comboText.life -= delta;
            comboText.y += comboText.velocityY * delta;
            if (comboText.life <= 0f) {
                comboText.active = false;
            }
        }
        if (starAnim > 0f) {
            starAnim -= delta;
        }
        if (giantFlash > 0f) {
            giantFlash -= delta;
        }
        if (bounceArrowTimer > 0f) {
            bounceArrowTimer -= delta;
        }
        advanceWaves(delta);
        moveEnemies(delta);
        moveBall(delta);
        coolHitEffects(delta);
        if (life <= 0) {
            gameState = GameState.GAME_OVER;
            if (listener != null) {
                listener.onGameFinished(false, 0, "The lanes were overrun. Score " + score);
            }
        } else if (!levelCleared && wavesDone() && !hasEnemies() && !ball.active) {
            levelCleared = true;
            stars = computeStars();
            starAnim = 1.6f;
            gameState = GameState.GAME_OVER;
            if (listener != null) {
                listener.onGameFinished(true, stars, level.name + " clear with " + stars + " stars");
            }
        }
        dispatchHud();
    }

    private void advanceWaves(float delta) {
        for (int i = 0; i < level.waves.length; i++) {
            LevelDefinition.Wave wave = level.waves[i];
            if (spawnCursor[i] >= wave.spawns.length) {
                continue;
            }
            spawnTimer[i] += delta;
            LevelDefinition.Spawn spawn = wave.spawns[spawnCursor[i]];
            float target = wave.spawnInterval * spawn.delaySteps;
            if (spawnTimer[i] >= target) {
                spawnEnemy(spawn.row, spawn.type);
                spawnCursor[i]++;
                if (i >= waveIndex && spawnCursor[i] > 0) {
                    waveIndex = i;
                }
            }
        }
    }

    private void spawnEnemy(int row, EnemyType type) {
        EnemyCell free = null;
        for (int col = COLS - 1; col >= 0; col--) {
            EnemyCell cell = board[row][col];
            if (!cell.active) {
                free = cell;
                cell.set(type, COLS - 0.2f);
                return;
            }
        }
        if (free == null) {
            life--;
        }
    }

    private void moveEnemies(float delta) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                EnemyCell cell = board[row][col];
                if (!cell.active) {
                    continue;
                }
                cell.x -= ENEMY_SPEED * delta;
                if (cell.x <= 0.1f) {
                    cell.clear();
                    life--;
                    combo = 0;
                    continue;
                }
                int targetCol = clamp((int) Math.floor(cell.x), 0, COLS - 1);
                if (targetCol != col && !board[row][targetCol].active) {
                    board[row][targetCol].set(cell.type, cell.x);
                    board[row][targetCol].hp = cell.hp;
                    board[row][targetCol].hitFlash = cell.hitFlash;
                    cell.clear();
                }
            }
        }
    }

    public boolean canDrop() {
        return gameState == GameState.PLAYING && conveyorTimer >= conveyorCooldown && !ball.active;
    }

    public boolean dropBall(int row) {
        if (!canDrop() || row < 0 || row >= ROWS) {
            return false;
        }
        BallType type = BallType.values()[previewBalls[0]];
        ball.reset(type, row, 0.2f, row + 0.5f);
        if (type == BallType.GIANT) {
            ball.verticalDir = 0;
        }
        shiftPreview();
        conveyorTimer = 0f;
        bounceArrowTimer = 0.25f;
        return true;
    }

    private void moveBall(float delta) {
        if (!ball.active) {
            return;
        }
        if (ball.type == BallType.GIANT) {
            ball.x += GIANT_SPEED * delta;
            ball.heavyPulse = 0.12f;
            giantFlash = 0.12f;
            clearRowHits(ball.row);
            if (ball.x > COLS + 0.6f) {
                ball.active = false;
            }
            return;
        }
        ball.x += NORMAL_SPEED * delta;
        if (ball.type == BallType.NORMAL) {
            float ySpeed = 2.8f * ball.verticalDir;
            ball.y += ySpeed * delta;
            if (ball.y < 0.5f) {
                ball.y = 0.5f;
                ball.verticalDir = 1;
                ball.bouncesLeft--;
                bounceArrowTimer = 0.2f;
            } else if (ball.y > ROWS - 0.5f) {
                ball.y = ROWS - 0.5f;
                ball.verticalDir = -1;
                ball.bouncesLeft--;
                bounceArrowTimer = 0.2f;
            }
        }
        int impactRow = clamp(Math.round(ball.y - 0.5f), 0, ROWS - 1);
        ball.row = impactRow;
        int impactCol = findImpactCol(impactRow, ball.x);
        if (impactCol >= 0) {
            if (ball.type == BallType.BOMB) {
                triggerBomb(impactRow, impactCol);
                ball.active = false;
            } else {
                damageSingle(impactRow, impactCol);
                ball.collisionCount++;
                ball.hitPulse = 0.12f;
                ball.squash = 0.16f;
                ball.verticalDir *= -1;
                ball.bouncesLeft--;
                bounceArrowTimer = 0.2f;
                if (ball.bouncesLeft <= 0 || ball.collisionCount >= 6) {
                    ball.active = false;
                }
            }
        }
        if (ball.x > COLS + 0.6f || ball.bouncesLeft <= 0) {
            ball.active = false;
        }
    }

    private int findImpactCol(int row, float xPos) {
        for (int col = 0; col < COLS; col++) {
            EnemyCell cell = board[row][col];
            if (cell.active && Math.abs(cell.x - xPos) < HIT_EPSILON) {
                return col;
            }
        }
        return -1;
    }

    private void damageSingle(int row, int col) {
        EnemyCell enemy = board[row][col];
        if (!enemy.active) {
            return;
        }
        enemy.hp -= 1;
        enemy.hitFlash = 0.14f;
        if (enemy.hp <= 0) {
            killEnemy(row, col, 1);
        } else {
            combo = 0;
        }
    }

    private void triggerBomb(int centerRow, int centerCol) {
        explosionPulse.trigger(centerRow, centerCol);
        for (int row = Math.max(0, centerRow - 1); row <= Math.min(ROWS - 1, centerRow + 1); row++) {
            for (int col = Math.max(0, centerCol - 1); col <= Math.min(COLS - 1, centerCol + 1); col++) {
                if (board[row][col].active) {
                    killEnemy(row, col, 2);
                }
            }
        }
    }

    private void clearRowHits(int row) {
        for (int col = 0; col < COLS; col++) {
            if (board[row][col].active && board[row][col].x <= ball.x + 0.3f) {
                killEnemy(row, col, 3);
            }
        }
    }

    private void killEnemy(int row, int col, int bonus) {
        EnemyCell enemy = board[row][col];
        if (!enemy.active) {
            return;
        }
        combo++;
        clearedEnemies++;
        score += 100 * bonus + combo * 12;
        comboText.show("Combo x" + combo, boardRect.centerX(), boardRect.top + 40f);
        enemy.clear();
    }

    private void coolHitEffects(float delta) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                EnemyCell cell = board[row][col];
                if (cell.hitFlash > 0f) {
                    cell.hitFlash -= delta;
                }
            }
        }
        if (ball.hitPulse > 0f) {
            ball.hitPulse -= delta;
        }
        if (ball.heavyPulse > 0f) {
            ball.heavyPulse -= delta;
        }
        if (ball.squash > 0f) {
            ball.squash -= delta;
        }
    }

    private boolean hasEnemies() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col].active) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean wavesDone() {
        for (int i = 0; i < level.waves.length; i++) {
            if (spawnCursor[i] < level.waves[i].spawns.length) {
                return false;
            }
        }
        return true;
    }

    private int computeStars() {
        int result = 1;
        if (life >= Math.max(2, level.startLife - 1)) {
            result++;
        }
        if (score >= level.targetScore) {
            result++;
        }
        return Math.min(3, result);
    }

    private void refillPreview() {
        for (int i = 0; i < PREVIEW_COUNT; i++) {
            previewBalls[i] = rollBall();
        }
    }

    private void shiftPreview() {
        previewBalls[0] = previewBalls[1];
        previewBalls[1] = previewBalls[2];
        previewBalls[2] = rollBall();
    }

    private int rollBall() {
        int roll = random.nextInt(100);
        if (roll < 80) {
            return BallType.NORMAL.ordinal();
        }
        if (roll < 90) {
            return BallType.BOMB.ordinal();
        }
        return BallType.GIANT.ordinal();
    }

    private void dispatchHud() {
        int currentWave = waveIndex + 1;
        if (score == hudScore && combo == hudCombo && currentWave == hudWave && life == hudLife) {
            return;
        }
        hudScore = score;
        hudCombo = combo;
        hudWave = currentWave;
        hudLife = life;
        if (listener != null) {
            listener.onHudChanged(score, combo, currentWave, life);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
