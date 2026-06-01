package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.GameState;
import com.android.boot.core.InputState;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Player;
import com.android.boot.fx.ChiBurst;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class GameView extends View {
    private static final String LOG_TAG = "BlazeDragonReborn";
    private static final long FRAME_NS = 16666667L;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF leftPad = new RectF();
    private final RectF rightPad = new RectF();
    private final RectF jumpPad = new RectF();
    private final RectF dodgePad = new RectF();
    private final RectF attackPad = new RectF();
    private final RectF chiPad = new RectF();
    private final RectF pausePad = new RectF();
    private final RectF menuPrimary = new RectF();
    private final RectF menuSecondary = new RectF();
    private final RectF objectiveCard = new RectF();
    private final RectF chapterCardA = new RectF();
    private final RectF chapterCardB = new RectF();
    private final RectF chapterCardC = new RectF();
    private final RectF chapterCardD = new RectF();
    private final RectF chapterCardE = new RectF();
    private final RectF chapterCardF = new RectF();
    private final RectF chapterBack = new RectF();
    private final RectF menuUpgrade = new RectF();
    private final RectF menuCodex = new RectF();
    private final RectF menuSettings = new RectF();
    private final RectF panelPrimary = new RectF();
    private final RectF panelSecondary = new RectF();
    private final RectF panelTertiary = new RectF();
    private final RectF panelBack = new RectF();
    private boolean jumpHeld;
    private boolean dodgeHeld;
    private boolean attackHeld;
    private boolean chiHeld;
    private boolean hostActive;
    private boolean loopRunning;
    private long lastFrameNs;
    private final InputState input = new InputState();
    private final Player player = new Player();
    private final ChiBurst chiBurst = new ChiBurst();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) {
                return;
            }
            long now = System.nanoTime();
            float dt = lastFrameNs == 0L ? 0.016f : (now - lastFrameNs) / 1000000000f;
            lastFrameNs = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            try {
                stepFrame(dt);
            } catch (Throwable throwable) {
                loopRunning = false;
                fatalErrorTitle = throwable.getClass().getSimpleName();
                fatalErrorDetail = throwable.getMessage() == null || throwable.getMessage().trim().isEmpty() ? "Runtime failure during startup." : throwable.getMessage().trim();
                Log.e(LOG_TAG, "Frame update failure", throwable);
            }
            invalidate();
            if (loopRunning) {
                postOnAnimationDelayed(this, FRAME_NS / 1000000L);
            }
        }
    };
    private GameState state = GameState.MENU;
    private Bitmap heroIdle;
    private Bitmap heroWalkA;
    private Bitmap heroWalkB;
    private Bitmap heroActionA;
    private Bitmap heroActionB;
    private Bitmap heroHurt;
    private Bitmap heroJump;
    private Bitmap gruntWalkA;
    private Bitmap gruntWalkB;
    private Bitmap gruntIdle;
    private Bitmap gruntHurt;
    private Bitmap flyerA;
    private Bitmap flyerB;
    private Bitmap bossIdle;
    private Bitmap bossWalkA;
    private Bitmap bossWalkB;
    private Bitmap background;
    private Bitmap cloudLayer;
    private float floorY;
    private float stageWidth = 3200f;
    private float cameraX;
    private int waveIndex;
    private int score;
    private boolean showHowTo;
    private float titlePulse;
    private float bannerTimer;
    private float bossAlertTimer;
    private float shakeTime;
    private float shakePower;
    private boolean trainingMode;
    private int chapterIndex = 1;
    private int gold = 120;
    private int crestShards = 2;
    private int techniqueScrolls = 1;
    private int attackLevel = 1;
    private int chiLevel = 1;
    private int vitalityLevel = 1;
    private boolean musicOn = true;
    private boolean sfxOn = true;
    private MediaPlayer menuBgm;
    private MediaPlayer playBgm;
    private int bgmMode = -1;
    private int clearGoldReward;
    private int clearShardReward;
    private int clearScrollReward;
    private String resultSummary = "Route clear";
    private String codexTitle = "Enemy Codex";
    private String objectiveText = "Sweep the lane and break the gate captain.";
    private String bannerTitle = "Chapter One";
    private String bannerBody = "Ash Gate Approach";
    private volatile String fatalErrorTitle;
    private volatile String fatalErrorDetail;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setFocusable(true);
        setKeepScreenOn(true);
        loadAssets();
    }

    public void onHostResume() {
        hostActive = true;
        fatalErrorTitle = null;
        fatalErrorDetail = null;
        updateAudioState();
        startLoop();
        invalidate();
    }

    public void onHostPause() {
        hostActive = false;
        pauseAllAudio();
        stopLoop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (hostActive) {
            startLoop();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        pauseAllAudio();
        super.onDetachedFromWindow();
    }

    private void startLoop() {
        if (loopRunning || !isAttachedToWindow()) {
            return;
        }
        lastFrameNs = 0L;
        loopRunning = true;
        removeCallbacks(frameRunnable);
        postOnAnimation(frameRunnable);
    }

    private void stopLoop() {
        loopRunning = false;
        lastFrameNs = 0L;
        removeCallbacks(frameRunnable);
    }

    private void stepFrame(float dt) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        titlePulse += dt;
        bannerTimer = Math.max(0f, bannerTimer - dt);
        bossAlertTimer = Math.max(0f, bossAlertTimer - dt);
        shakeTime = Math.max(0f, shakeTime - dt);
        if (getHeight() > 0) {
            floorY = getHeight() - Math.max(58f, getHeight() * 0.11f);
        }
        if (state == GameState.PLAYING || state == GameState.TRAINING) {
            updateGame(dt);
        }
        chiBurst.update(dt);
        updateAudioState();
    }

    private void loadAssets() {
        heroIdle = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_idle.png");
        heroWalkA = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_walk1.png");
        heroWalkB = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_walk2.png");
        heroActionA = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_action1.png");
        heroActionB = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_action2.png");
        heroHurt = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_hurt.png");
        heroJump = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Player/Poses/player_jump.png");
        gruntIdle = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Soldier/Poses/soldier_idle.png");
        gruntWalkA = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Soldier/Poses/soldier_walk1.png");
        gruntWalkB = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Soldier/Poses/soldier_walk2.png");
        gruntHurt = loadBitmap("game_art/kenney_platformer_characters/assets/PNG/Soldier/Poses/soldier_hurt.png");
        flyerA = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Enemies/flyFly1.png");
        flyerB = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Enemies/flyFly2.png");
        bossIdle = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Enemies/blockerBody.png");
        bossWalkA = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Enemies/blockerMad.png");
        bossWalkB = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Enemies/blockerSad.png");
        background = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/bg_castle.png");
        cloudLayer = loadBitmap("game_art/kenney_new_platformer_pack/assets/Sprites/Backgrounds/Double/background_clouds.png");
    }

    private Bitmap loadBitmap(String path) {
        try {
            InputStream stream = getContext().getAssets().open(path);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            stream.close();
            return bitmap;
        } catch (IOException ignored) {
            return null;
        }
    }

    private MediaPlayer buildLoopPlayer(String assetPath) {
        try {
            AssetFileDescriptor descriptor = getContext().getAssets().openFd(assetPath);
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build());
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            player.setLooping(true);
            player.setVolume(0.72f, 0.72f);
            player.prepare();
            return player;
        } catch (IOException ignored) {
            return null;
        }
    }

    private void ensureAudioPlayers() {
        if (menuBgm == null) {
            menuBgm = buildLoopPlayer("audio/bgm_menu.wav");
        }
        if (playBgm == null) {
            playBgm = buildLoopPlayer("audio/bgm_play.wav");
        }
    }

    private void pauseAllAudio() {
        try {
            if (menuBgm != null && menuBgm.isPlaying()) {
                menuBgm.pause();
            }
        } catch (RuntimeException ignored) {
            menuBgm = null;
        }
        try {
            if (playBgm != null && playBgm.isPlaying()) {
                playBgm.pause();
            }
        } catch (RuntimeException ignored) {
            playBgm = null;
        }
    }

    private void updateAudioState() {
        try {
            ensureAudioPlayers();
            int targetMode = 0;
            if (musicOn) {
                if (state == GameState.PLAYING || state == GameState.TRAINING || state == GameState.PAUSED) {
                    targetMode = 2;
                } else {
                    targetMode = 1;
                }
            }
            if (bgmMode == targetMode) {
                return;
            }
            bgmMode = targetMode;
            pauseAllAudio();
            if (targetMode == 1 && menuBgm != null) {
                menuBgm.seekTo(0);
                menuBgm.start();
            } else if (targetMode == 2 && playBgm != null) {
                playBgm.seekTo(0);
                playBgm.start();
            }
        } catch (RuntimeException ignored) {
            musicOn = false;
            bgmMode = 0;
            pauseAllAudio();
        }
    }

    private void startRun() {
        startRun(false, chapterIndex);
    }

    private void startRun(boolean training, int chapter) {
        trainingMode = training;
        chapterIndex = chapter;
        waveIndex = 0;
        score = 0;
        cameraX = 0f;
        shakeTime = 0f;
        shakePower = 0f;
        player.reset(floorY);
        syncPlayerGrowth();
        player.hp = player.maxHp;
        player.chi = Math.min(player.maxChi, 34f + chiLevel * 6f);
        enemies.clear();
        stageWidth = training ? 2200f : 2600f + (chapter - 1) * 240f;
        bannerTitle = training ? "Training Room" : "Chapter " + chapter;
        bannerBody = training ? "Dragon form rehearsal" : (chapter == 1 ? "Ash Gate Approach" : (chapter == 2 ? "Moon Terrace Push" : "Volcanic Seal Break"));
        objectiveText = training ? "Practice launchers, dodges, and chi routing without failure pressure." : "Open with fast chains and carry chi into the gate captain.";
        bannerTimer = 2.4f;
        bossAlertTimer = 0f;
        spawnNextWave();
        state = training ? GameState.TRAINING : GameState.PLAYING;
    }

    private int getAttackPower(int base) {
        return base + (attackLevel - 1) * 4;
    }

    private int getMaxHpValue() {
        return 220 + (vitalityLevel - 1) * 28;
    }

    private float getMaxChiValue() {
        return 100f + (chiLevel - 1) * 16f;
    }

    private void syncPlayerGrowth() {
        player.maxHp = getMaxHpValue();
        player.maxChi = getMaxChiValue();
        if (player.hp > player.maxHp) {
            player.hp = player.maxHp;
        }
        if (player.chi > player.maxChi) {
            player.chi = player.maxChi;
        }
    }

    private void awardClearRewards() {
        clearGoldReward = 30 + chapterIndex * 18 + score / 40;
        clearShardReward = chapterIndex >= 3 ? 2 : 1;
        clearScrollReward = 1 + (player.comboCount >= 12 ? 1 : 0);
        gold += clearGoldReward;
        crestShards += clearShardReward;
        techniqueScrolls += clearScrollReward;
        resultSummary = "Route pressure broken with " + getStyleRank() + " style.";
    }

    private void spawnNextWave() {
        enemies.clear();
        if (trainingMode) {
            bannerTitle = "Training";
            bannerBody = waveIndex == 0 ? "Launcher route drill" : "Chi follow-up drill";
            objectiveText = "Cycle dodge, launcher, air chase, and chi finish until the route feels smooth.";
            enemies.add(new Enemy(Enemy.TYPE_GRUNT, 980f, floorY));
            enemies.add(new Enemy(Enemy.TYPE_FLYER, 1160f, floorY - 96f));
            if (waveIndex > 0) {
                enemies.add(new Enemy(Enemy.TYPE_ASSASSIN, 1320f, floorY));
            }
            bannerTimer = 1.6f;
            return;
        }
        if (waveIndex == 0) {
            bannerTitle = "Wave One";
            bannerBody = chapterIndex == 1 ? "Scouts on the ridge" : (chapterIndex == 2 ? "Moon guard vanguard" : (chapterIndex == 3 ? "Ash furnace outriders" : (chapterIndex == 4 ? "Bridge wardens" : (chapterIndex == 5 ? "Shrine veil raiders" : "Volcanic honor guard"))));
            objectiveText = "Keep the chain live while the front scouts collapse.";
            enemies.add(new Enemy(Enemy.TYPE_GRUNT, 860f, floorY));
            enemies.add(new Enemy(Enemy.TYPE_GRUNT, 980f, floorY));
            enemies.add(new Enemy(Enemy.TYPE_FLYER, 1100f, floorY - 90f));
            enemies.add(new Enemy(chapterIndex >= 4 ? Enemy.TYPE_ASSASSIN : Enemy.TYPE_GRUNT, 1180f, floorY));
            if (chapterIndex >= 2) {
                enemies.add(new Enemy(Enemy.TYPE_FLYER, 1260f, floorY - 76f));
            }
            if (chapterIndex >= 5) {
                enemies.add(new Enemy(Enemy.TYPE_RANGER, 1360f, floorY));
            }
        } else if (waveIndex == 1) {
            bannerTitle = "Wave Two";
            bannerBody = chapterIndex == 1 ? "Lancers at the cliff turn" : (chapterIndex == 2 ? "Terrace pincer squad" : (chapterIndex == 3 ? "Seal road cutters" : (chapterIndex == 4 ? "Courtyard breaker line" : (chapterIndex == 5 ? "Moon shrine interceptors" : "Gatefire crusher pack"))));
            objectiveText = "Launch the line apart before the sky hunters pinch the lane.";
            enemies.add(new Enemy(chapterIndex >= 3 ? Enemy.TYPE_BRUTE : Enemy.TYPE_GRUNT, 1480f, floorY));
            enemies.add(new Enemy(chapterIndex >= 2 ? Enemy.TYPE_ASSASSIN : Enemy.TYPE_GRUNT, 1600f, floorY));
            enemies.add(new Enemy(Enemy.TYPE_FLYER, 1680f, floorY - 96f));
            enemies.add(new Enemy(chapterIndex >= 4 ? Enemy.TYPE_RANGER : Enemy.TYPE_GRUNT, 1780f, floorY));
            enemies.add(new Enemy(chapterIndex >= 5 ? Enemy.TYPE_ASSASSIN : Enemy.TYPE_FLYER, 1880f, chapterIndex >= 5 ? floorY : floorY - 72f));
            if (chapterIndex >= 3) {
                enemies.add(new Enemy(Enemy.TYPE_GRUNT, 1980f, floorY));
            }
            if (chapterIndex >= 6) {
                enemies.add(new Enemy(Enemy.TYPE_BRUTE, 2100f, floorY));
            }
        } else {
            bannerTitle = "Boss Pressure";
            bannerBody = chapterIndex == 1 ? "Gate captain enters" : (chapterIndex == 2 ? "Moon judge descends" : (chapterIndex == 3 ? "Seal breaker erupts" : (chapterIndex == 4 ? "Bridge tyrant arrives" : (chapterIndex == 5 ? "Shrine executioner descends" : "Volcanic dragon vassal rises"))));
            objectiveText = "Dodge the captain, relaunch the escorts, and cash out with chi.";
            bossAlertTimer = 2.2f;
            enemies.add(new Enemy(chapterIndex >= 4 ? Enemy.TYPE_BRUTE : Enemy.TYPE_GRUNT, 2220f, floorY));
            enemies.add(new Enemy(chapterIndex >= 2 ? Enemy.TYPE_ASSASSIN : Enemy.TYPE_FLYER, 2320f, chapterIndex >= 2 ? floorY : floorY - 88f));
            enemies.add(new Enemy(Enemy.TYPE_BOSS, 2480f, floorY));
            if (chapterIndex >= 2) {
                enemies.add(new Enemy(chapterIndex >= 5 ? Enemy.TYPE_RANGER : Enemy.TYPE_GRUNT, 2380f, floorY));
            }
            if (chapterIndex >= 6) {
                enemies.add(new Enemy(Enemy.TYPE_FLYER, 2580f, floorY - 94f));
            }
        }
        bannerTimer = 1.8f;
    }

    private void updateGame(float dt) {
        if (input.dodge) {
            triggerDodge();
        }
        player.update(dt, input.left, input.right, input.jump, floorY);
        if (player.queuedAttack && player.attackTimer <= 0.06f) {
            player.queuedAttack = false;
            triggerAttack();
        }
        if (player.x < 90f) {
            player.x = 90f;
        }
        if (player.x > stageWidth - 80f) {
            player.x = stageWidth - 80f;
        }
        if (input.attack) {
            triggerAttack();
        }
        if (input.chi) {
            triggerChiWave();
        }
        updateEnemies(dt);
        updateCamera();
        if (player.hp <= 0) {
            if (trainingMode) {
                syncPlayerGrowth();
                player.hp = player.maxHp;
                player.chi = player.maxChi;
                player.x = 180f;
                player.y = floorY;
                player.vx = 0f;
                player.vy = 0f;
                shake(0.08f, 6f);
            } else {
                state = GameState.GAME_OVER;
            }
        }
        if (enemies.isEmpty()) {
            if (trainingMode) {
                waveIndex = (waveIndex + 1) % 2;
                spawnNextWave();
            } else if (waveIndex >= 2) {
                awardClearRewards();
                state = GameState.RESULT;
            } else {
                waveIndex++;
                spawnNextWave();
            }
        }
        input.clearOneShot();
    }

    private void triggerDodge() {
        if (player.dodgeCooldown > 0f || player.attackTimer > 0.16f) {
            return;
        }
        float dir;
        if (input.left && !input.right) {
            dir = -1f;
            player.facingRight = false;
        } else if (input.right && !input.left) {
            dir = 1f;
            player.facingRight = true;
        } else {
            dir = player.facingRight ? 1f : -1f;
        }
        player.vx = dir * 520f;
        player.invul = Math.max(player.invul, 0.34f);
        player.dodgeTimer = 0.18f;
        player.dodgeCooldown = 0.7f;
        player.flashLife = 0.09f;
        shake(0.08f, 6f);
    }

    private void triggerAttack() {
        if (player.attackTimer > 0.08f) {
            player.queuedAttack = true;
            return;
        }
        if (!player.onGround) {
            triggerAirAttack();
            return;
        }
        if (player.comboWindow > 0f) {
            player.comboStep = Math.min(4, player.comboStep + 1);
        } else {
            player.comboStep = 1;
        }
        float dir = player.facingRight ? 1f : -1f;
        float range;
        int damage;
        float knock;
        float lift;
        if (player.comboStep == 1) {
            player.attackTimer = 0.14f;
            range = 122f;
            damage = getAttackPower(18);
            knock = 180f;
            lift = 0f;
        } else if (player.comboStep == 2) {
            player.attackTimer = 0.18f;
            range = 150f;
            damage = getAttackPower(26);
            knock = 220f;
            lift = 0f;
        } else if (player.comboStep == 3) {
            player.attackTimer = 0.22f;
            range = 168f;
            damage = getAttackPower(28);
            knock = 150f;
            lift = 520f;
        } else {
            player.attackTimer = 0.26f;
            range = 206f;
            damage = getAttackPower(38);
            knock = 420f;
            lift = 220f;
        }
        player.comboWindow = 0.36f;
        player.flashLife = 0.15f;
        player.attackGlow = 0.14f;
        player.vx += dir * 38f;
        player.chi = Math.min(player.maxChi, player.chi + 9f + player.comboStep * 3f);
        applyHitRange(range, damage, dir * knock, lift);
        shake(0.09f, player.comboStep >= 3 ? 12f : 7f);
    }

    private void triggerAirAttack() {
        player.comboStep = Math.max(2, Math.min(4, player.comboStep + 1));
        player.attackTimer = 0.18f;
        player.comboWindow = 0.24f;
        player.flashLife = 0.12f;
        player.attackGlow = 0.16f;
        player.vy = Math.min(player.vy, 80f);
        float dir = player.facingRight ? 1f : -1f;
        applyHitRange(154f, getAttackPower(30), dir * 240f, 260f);
        player.chi = Math.min(player.maxChi, player.chi + 12f);
        shake(0.08f, 8f);
    }

    private void triggerChiWave() {
        if (player.chi < 45f || player.chiCooldown > 0f || player.dodgeTimer > 0.02f) {
            return;
        }
        player.chi -= 45f;
        player.chiCooldown = 1.35f;
        player.attackTimer = 0.26f;
        player.comboWindow = 0.2f;
        player.attackGlow = 0.22f;
        float dir = player.facingRight ? 1f : -1f;
        chiBurst.trigger(player.x + dir * 110f, player.y - 54f, 128f);
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            float dx = enemy.x - player.x;
            if ((player.facingRight && dx > -34f && dx < 420f) || (!player.facingRight && dx < 34f && dx > -420f)) {
                float waveLift = enemy.type == Enemy.TYPE_BOSS ? 0f : 360f;
                enemy.takeHit(getAttackPower(enemy.type == Enemy.TYPE_BOSS ? 42 : 48), dir * 540f, waveLift);
                player.comboCount += enemy.type == Enemy.TYPE_BOSS ? 2 : 3;
                player.comboLife = 2f;
                score += enemy.type == Enemy.TYPE_BOSS ? 60 : 40;
            }
        }
        shake(0.18f, 16f);
        clearDeadEnemies();
    }

    private void applyHitRange(float range, int damage, float knock, float lift) {
        boolean hit = false;
        float dir = player.facingRight ? 1f : -1f;
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            float dx = enemy.x - player.x;
            float verticalAllowance = enemy.airborne ? 120f : 86f;
            if (((dir > 0f && dx >= -36f && dx <= range) || (dir < 0f && dx <= 36f && dx >= -range))
                    && Math.abs(enemy.y - player.y) < verticalAllowance) {
                float actualLift = enemy.type == Enemy.TYPE_BOSS ? Math.min(120f, lift * 0.2f) : lift;
                enemy.takeHit(damage, knock, actualLift);
                player.comboCount++;
                player.comboLife = 1.75f;
                score += 18 + damage / 2;
                hit = true;
            }
        }
        if (hit) {
            bannerTimer = 0f;
        }
        clearDeadEnemies();
    }

    private void updateEnemies(float dt) {
        for (Enemy enemy : enemies) {
            enemy.update(dt, player, floorY);
            if (enemy.canHit(player)) {
                int damage = enemy.type == Enemy.TYPE_BOSS ? 22 : (enemy.type == Enemy.TYPE_FLYER ? 10 : (enemy.type == Enemy.TYPE_BRUTE ? 18 : (enemy.type == Enemy.TYPE_RANGER ? 12 : (enemy.type == Enemy.TYPE_ASSASSIN ? 16 : 14))));
                float knock = enemy.x < player.x ? 220f : -220f;
                if (enemy.type == Enemy.TYPE_RANGER) {
                    knock *= 0.7f;
                }
                if (enemy.type == Enemy.TYPE_BRUTE) {
                    knock *= 1.25f;
                }
                if (player.takeDamage(damage, knock)) {
                    enemy.resetAttackCooldown();
                    shake(0.12f, enemy.type == Enemy.TYPE_BOSS ? 14f : 8f);
                }
            }
        }
        clearDeadEnemies();
    }

    private void clearDeadEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.dead) {
                iterator.remove();
                player.chi = Math.min(player.maxChi, player.chi + (enemy.type == Enemy.TYPE_BOSS ? 24f : 14f));
                score += enemy.type == Enemy.TYPE_BOSS ? 260 : 90;
                shake(0.1f, enemy.type == Enemy.TYPE_BOSS ? 18f : 10f);
            }
        }
    }

    private void updateCamera() {
        cameraX = player.x - getWidth() * 0.42f;
        if (cameraX < 0f) {
            cameraX = 0f;
        }
        float maxCamera = Math.max(0f, stageWidth - getWidth());
        if (cameraX > maxCamera) {
            cameraX = maxCamera;
        }
    }

    private void shake(float time, float power) {
        shakeTime = Math.max(shakeTime, time);
        shakePower = Math.max(shakePower, power);
    }

    private String getStyleRank() {
        if (player.comboCount >= 18) {
            return "Dragon";
        }
        if (player.comboCount >= 12) {
            return "Blaze";
        }
        if (player.comboCount >= 7) {
            return "Flux";
        }
        if (player.comboCount >= 3) {
            return "Drive";
        }
        return "Calm";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (fatalErrorTitle != null) {
            drawFailureOverlay(canvas);
            return;
        }
        drawGame(canvas);
        if (state == GameState.MENU) {
            drawMenu(canvas);
        } else if (state == GameState.CHAPTER_SELECT) {
            drawChapterSelect(canvas);
        } else if (state == GameState.LOADOUT) {
            drawLoadout(canvas);
        } else if (state == GameState.CODEX) {
            drawCodex(canvas);
        } else if (state == GameState.PAUSED) {
            drawPause(canvas);
        } else if (state == GameState.RESULT) {
            drawResult(canvas);
        } else if (state == GameState.GAME_OVER) {
            drawOverlay(canvas, "Defeat", "Reset the chain, re-enter the lane, and bring the dragon back online.");
        }
    }

    private void drawFailureOverlay(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_bg_main));
        rect.set(getWidth() * 0.16f, getHeight() * 0.18f, getWidth() * 0.84f, getHeight() * 0.82f);
        paint.setColor(Color.argb(244, 14, 20, 38));
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(42f);
        canvas.drawText("Startup Error", rect.left + 28f, rect.top + 60f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(26f);
        canvas.drawText(fatalErrorTitle == null ? "Unknown failure" : fatalErrorTitle, rect.left + 28f, rect.top + 110f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(20f);
        canvas.drawText(fatalErrorDetail == null ? "Check logcat for the full stack trace." : fatalErrorDetail, rect.left + 28f, rect.top + 156f, paint);
        canvas.drawText("Reopen the game after the failing draw or update path is corrected.", rect.left + 28f, rect.top + 192f, paint);
    }

    private void drawGame(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_bg_main));
        float shakeX = 0f;
        float shakeY = 0f;
        if (shakeTime > 0f) {
            float t = titlePulse * 42f;
            shakeX = (float) Math.sin(t) * shakePower;
            shakeY = (float) Math.cos(t * 0.75f) * shakePower * 0.45f;
        }
        canvas.save();
        canvas.translate(shakeX, shakeY);
        drawBackground(canvas);
        drawGround(canvas);
        for (Enemy enemy : enemies) {
            drawEnemy(canvas, enemy);
        }
        drawPlayer(canvas);
        drawChiBurst(canvas);
        canvas.restore();
        drawHud(canvas);
        drawControls(canvas);
        drawCombatBanner(canvas);
    }

    private void drawBackground(Canvas canvas) {
        paint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), Color.rgb(10, 16, 30), Color.rgb(18, 22, 44), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);
        if (background != null) {
            rect.set(0f, 0f, getWidth(), getHeight() * 0.74f);
            paint.setAlpha(208);
            canvas.drawBitmap(background, null, rect, paint);
            paint.setAlpha(255);
        }
        if (cloudLayer != null) {
            float cloudWidth = getWidth() * 0.84f;
            for (int i = -1; i < 4; i++) {
                float left = (i * cloudWidth) - (cameraX * 0.15f % cloudWidth);
                rect.set(left, 18f, left + cloudWidth, 180f);
                paint.setAlpha(184);
                canvas.drawBitmap(cloudLayer, null, rect, paint);
            }
            paint.setAlpha(255);
        }
        paint.setColor(Color.argb(96, 110, 246, 255));
        canvas.drawRect(0f, getHeight() * 0.62f, getWidth(), getHeight() * 0.66f, paint);
    }

    private void drawGround(Canvas canvas) {
        paint.setColor(Color.argb(255, 14, 22, 38));
        canvas.drawRect(0f, floorY + 12f, getWidth(), getHeight(), paint);
        paint.setColor(Color.argb(255, 34, 52, 80));
        for (int i = 0; i < 15; i++) {
            float left = (i * 228f) - (cameraX % 228f);
            canvas.drawRoundRect(left, floorY + 4f, left + 190f, floorY + 18f, 8f, 8f, paint);
        }
        paint.setColor(Color.argb(110, 255, 209, 102));
        for (int i = 0; i < 10; i++) {
            float left = (i * 320f) - (cameraX % 320f);
            canvas.drawRect(left + 60f, floorY - 4f, left + 68f, floorY + 18f, paint);
        }
    }

    private void drawPlayer(Canvas canvas) {
        Bitmap frame = heroIdle;
        if (!player.onGround) {
            frame = heroJump != null ? heroJump : heroIdle;
        } else if (player.invul > 0f && heroHurt != null && player.dodgeTimer <= 0f) {
            frame = heroHurt;
        } else if (player.attackTimer > 0.18f && heroActionB != null) {
            frame = heroActionB;
        } else if (player.attackTimer > 0f && heroActionA != null) {
            frame = heroActionA;
        } else if (Math.abs(player.vx) > 30f && heroWalkA != null && heroWalkB != null) {
            frame = ((int) (titlePulse * 10f) % 2 == 0) ? heroWalkA : heroWalkB;
        }
        if (player.attackGlow > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10f);
            paint.setColor(Color.argb((int) (160f * (player.attackGlow / 0.22f)), 110, 246, 255));
            canvas.drawCircle(player.x - cameraX, player.y - 48f, 48f + player.attackGlow * 120f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        drawFacingBitmap(canvas, frame, player.x - cameraX, player.y - 102f, 114f, 114f, player.facingRight, player.flashLife > 0f);
    }

    private void drawEnemy(Canvas canvas, Enemy enemy) {
        Bitmap frame;
        if (enemy.type == Enemy.TYPE_BOSS) {
            frame = ((int) (titlePulse * 6f) % 2 == 0) ? bossWalkA : bossWalkB;
            if (frame == null) {
                frame = bossIdle;
            }
            drawFacingBitmap(canvas, frame, enemy.x - cameraX, enemy.y - 132f, 142f, 142f, enemy.facingRight, enemy.hitFlash > 0f);
            drawEnemyHp(canvas, enemy, 104f);
            return;
        }
        if (enemy.type == Enemy.TYPE_FLYER) {
            frame = ((int) (titlePulse * 12f) % 2 == 0) ? flyerA : flyerB;
            drawFacingBitmap(canvas, frame, enemy.x - cameraX, enemy.y - 72f, 84f, 72f, enemy.facingRight, enemy.hitFlash > 0f);
            drawEnemyHp(canvas, enemy, 44f);
            return;
        }
        if (enemy.hitFlash > 0f && gruntHurt != null) {
            frame = gruntHurt;
        } else if (Math.abs(enemy.vx) > 18f && gruntWalkA != null && gruntWalkB != null) {
            frame = ((int) (titlePulse * 9f) % 2 == 0) ? gruntWalkA : gruntWalkB;
        } else {
            frame = gruntIdle;
        }
        drawFacingBitmap(canvas, frame, enemy.x - cameraX, enemy.y - 98f, 108f, 108f, enemy.facingRight, enemy.hitFlash > 0f);
        drawEnemyHp(canvas, enemy, 58f);
    }

    private void drawEnemyHp(Canvas canvas, Enemy enemy, float width) {
        float screenX = enemy.x - cameraX;
        float top = enemy.y - enemy.radius * 2.6f;
        paint.setColor(Color.argb(180, 15, 24, 42));
        canvas.drawRoundRect(screenX - width * 0.5f, top, screenX + width * 0.5f, top + 10f, 6f, 6f, paint);
        float ratio = Math.max(0f, enemy.hp / (float) enemy.maxHp);
        int color = enemy.type == Enemy.TYPE_BOSS ? ContextCompat.getColor(getContext(), R.color.cst_warning) : ContextCompat.getColor(getContext(), R.color.cst_danger);
        paint.setColor(color);
        canvas.drawRoundRect(screenX - width * 0.5f, top, screenX - width * 0.5f + width * ratio, top + 10f, 6f, 6f, paint);
    }

    private void drawFacingBitmap(Canvas canvas, Bitmap bitmap, float centerX, float topY, float width, float height, boolean facingRight, boolean flash) {
        if (bitmap == null) {
            paint.setColor(flash ? ContextCompat.getColor(getContext(), R.color.cst_warning) : ContextCompat.getColor(getContext(), R.color.cst_accent));
            canvas.drawRoundRect(centerX - width * 0.5f, topY, centerX + width * 0.5f, topY + height, 12f, 12f, paint);
            return;
        }
        rect.set(-width * 0.5f, 0f, width * 0.5f, height);
        canvas.save();
        canvas.translate(centerX, topY);
        if (!facingRight) {
            canvas.scale(-1f, 1f);
        }
        paint.setAlpha(flash ? 170 : 255);
        canvas.drawBitmap(bitmap, null, rect, paint);
        paint.setAlpha(255);
        canvas.restore();
    }

    private void drawChiBurst(Canvas canvas) {
        if (!chiBurst.active) {
            return;
        }
        float screenX = chiBurst.x - cameraX;
        float lifeRatio = Math.max(0f, chiBurst.life) / 0.42f;
        int alpha = (int) (210f * lifeRatio);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha / 3, 110, 246, 255));
        canvas.drawCircle(screenX, chiBurst.y, chiBurst.radius * 0.82f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(18f);
        paint.setColor(Color.argb(alpha, 110, 246, 255));
        canvas.drawCircle(screenX, chiBurst.y, chiBurst.radius, paint);
        paint.setStrokeWidth(8f);
        paint.setColor(Color.argb(alpha, 255, 209, 102));
        canvas.drawArc(screenX - chiBurst.radius * 0.9f, chiBurst.y - chiBurst.radius * 0.42f, screenX + chiBurst.radius * 0.9f, chiBurst.y + chiBurst.radius * 0.42f, -38f, 248f, false, paint);
        paint.setStrokeWidth(4f);
        for (int i = 0; i < 4; i++) {
            float orbit = chiBurst.radius * (0.45f + i * 0.16f);
            float angle = titlePulse * 180f + i * 70f;
            float x = screenX + (float) Math.cos(Math.toRadians(angle)) * orbit;
            float y = chiBurst.y + (float) Math.sin(Math.toRadians(angle * 0.8f)) * orbit * 0.28f;
            paint.setColor(Color.argb(alpha, i % 2 == 0 ? 110 : 255, i % 2 == 0 ? 246 : 209, 255));
            canvas.drawCircle(x, y, 8f + i * 2f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHud(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float base = Math.min(w, h);
        float hudPad = Math.max(12f, base * 0.016f);
        float leftWidth = Math.min(w * 0.34f, base * 0.62f);
        float hudHeight = Math.max(82f, h * 0.12f);
        float rightWidth = Math.min(w * 0.23f, base * 0.44f);
        float badgeWidth = Math.max(58f, leftWidth * 0.18f);
        float textSmall = Math.max(14f, base * 0.019f);
        float textMedium = Math.max(16f, base * 0.022f);
        float textLarge = Math.max(28f, base * 0.04f);
        paint.setColor(Color.argb(208, 7, 14, 28));
        canvas.drawRoundRect(hudPad, hudPad, hudPad + leftWidth, hudPad + hudHeight, 18f, 18f, paint);
        canvas.drawRoundRect(w - rightWidth - hudPad, hudPad, w - hudPad, hudPad + hudHeight + 6f, 18f, 18f, paint);
        paint.setColor(Color.argb(164, 41, 58, 102));
        canvas.drawRoundRect(hudPad + 8f, hudPad + 6f, hudPad + 8f + badgeWidth, hudPad + hudHeight - 6f, 18f, 18f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(textSmall);
        canvas.drawText("HP", hudPad + 18f, hudPad + 30f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_track));
        float meterLeft = hudPad + badgeWidth + 20f;
        float meterRight = hudPad + leftWidth - 16f;
        float hpTop = hudPad + 14f;
        canvas.drawRoundRect(meterLeft, hpTop, meterRight, hpTop + 14f, 8f, 8f, paint);
        float hpRatio = Math.max(0f, player.hp / (float) player.maxHp);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_fill));
        canvas.drawRoundRect(meterLeft, hpTop, meterLeft + (meterRight - meterLeft) * hpRatio, hpTop + 14f, 8f, 8f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(textMedium);
        canvas.drawText(player.hp + " / " + player.maxHp, meterLeft, hudPad + 52f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_track));
        float chiTop = hudPad + 38f;
        canvas.drawRoundRect(meterLeft, chiTop, meterRight, chiTop + 14f, 8f, 8f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
        canvas.drawRoundRect(meterLeft, chiTop, meterLeft + (meterRight - meterLeft) * (player.chi / player.maxChi), chiTop + 14f, 8f, 8f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(textSmall);
        canvas.drawText("CHI " + (int) player.chi, meterLeft, hudPad + hudHeight - 10f, paint);
        paint.setColor(Color.argb(178, 18, 30, 56));
        canvas.drawRoundRect(w - rightWidth - hudPad + 12f, hudPad + 10f, w - hudPad - 12f, hudPad + 36f, 14f, 14f, paint);
        canvas.drawRoundRect(w - rightWidth - hudPad + 12f, hudPad + 42f, w - hudPad - 12f, hudPad + 68f, 14f, 14f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(textSmall);
        canvas.drawText("WAVE " + (waveIndex + 1) + " / 3", w - rightWidth - hudPad + 24f, hudPad + 30f, paint);
        canvas.drawText("SCORE " + score, w - rightWidth - hudPad + 24f, hudPad + 62f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(textLarge);
        canvas.drawText(String.format(Locale.US, "%02d", player.comboCount), w - Math.max(118f, rightWidth * 0.5f), hudPad + hudHeight + 54f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(textMedium);
        canvas.drawText(getStyleRank(), w - Math.max(118f, rightWidth * 0.5f), hudPad + hudHeight + 78f, paint);
        float pauseSize = Math.max(56f, base * 0.08f);
        pausePad.set(w - pauseSize - hudPad, hudPad + hudHeight + 10f, w - hudPad, hudPad + hudHeight + 10f + pauseSize);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(pausePad, 12f, 12f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(textMedium + 2f);
        canvas.drawText("II", pausePad.centerX() - 10f, pausePad.centerY() + 8f, paint);
        objectiveCard.set(w * 0.22f, hudPad, w * 0.78f, hudPad + Math.max(42f, base * 0.06f));
        paint.setColor(Color.argb(160, 9, 18, 36));
        canvas.drawRoundRect(objectiveCard, 16f, 16f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(textSmall);
        canvas.drawText(objectiveText, objectiveCard.left + 16f, objectiveCard.centerY() + 5f, paint);
    }

    private void drawControls(Canvas canvas) {
        layoutGameplayTouchZones();
        float h = getHeight();
        float base = Math.min(getWidth(), getHeight());
        float labelText = Math.max(18f, base * 0.025f);
        paint.setColor(Color.argb(188, 24, 36, 66));
        canvas.drawRoundRect(leftPad, 18f, 18f, paint);
        canvas.drawRoundRect(rightPad, 18f, 18f, paint);
        paint.setColor(Color.argb(212, 28, 44, 82));
        canvas.drawRoundRect(jumpPad, 18f, 18f, paint);
        canvas.drawRoundRect(dodgePad, 18f, 18f, paint);
        canvas.drawRoundRect(attackPad, 18f, 18f, paint);
        paint.setColor(Color.argb(224, 56, 96, 158));
        canvas.drawRoundRect(chiPad, 18f, 18f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(labelText);
        canvas.drawText("L", leftPad.centerX() - labelText * 0.28f, leftPad.centerY() + labelText * 0.32f, paint);
        canvas.drawText("R", rightPad.centerX() - labelText * 0.28f, rightPad.centerY() + labelText * 0.32f, paint);
        canvas.drawText("J", jumpPad.centerX() - labelText * 0.28f, jumpPad.centerY() + labelText * 0.32f, paint);
        canvas.drawText("D", dodgePad.centerX() - labelText * 0.28f, dodgePad.centerY() + labelText * 0.32f, paint);
        canvas.drawText("A", attackPad.centerX() - labelText * 0.28f, attackPad.centerY() + labelText * 0.32f, paint);
        canvas.drawText("Q", chiPad.centerX() - labelText * 0.28f, chiPad.centerY() + labelText * 0.32f, paint);
    }

    private void drawCombatBanner(Canvas canvas) {
        if (bannerTimer > 0f) {
            float alphaRatio = Math.min(1f, bannerTimer / 0.6f);
            paint.setColor(Color.argb((int) (180f * alphaRatio), 8, 14, 28));
            rect.set(getWidth() * 0.28f, getHeight() * 0.18f, getWidth() * 0.72f, getHeight() * 0.34f);
            canvas.drawRoundRect(rect, 22f, 22f, paint);
            paint.setColor(Color.argb((int) (255f * alphaRatio), 255, 209, 102));
            paint.setTextSize(20f);
            canvas.drawText(bannerTitle, rect.left + 24f, rect.top + 44f, paint);
            paint.setColor(Color.argb((int) (255f * alphaRatio), 242, 246, 255));
            paint.setTextSize(32f);
            canvas.drawText(bannerBody, rect.left + 24f, rect.top + 84f, paint);
        }
        if (bossAlertTimer > 0f) {
            float alphaRatio = Math.min(1f, bossAlertTimer / 0.5f);
            paint.setColor(Color.argb((int) (220f * alphaRatio), 120, 14, 28));
            rect.set(getWidth() * 0.18f, getHeight() * 0.42f, getWidth() * 0.82f, getHeight() * 0.52f);
            canvas.drawRoundRect(rect, 14f, 14f, paint);
            paint.setColor(Color.argb((int) (255f * alphaRatio), 255, 241, 225));
            paint.setTextSize(34f);
            canvas.drawText("BOSS PRESSURE", rect.left + 26f, rect.centerY() + 12f, paint);
        }
    }

    private void layoutGameplayTouchZones() {
        float w = getWidth();
        float h = getHeight();
        float base = Math.min(w, h);
        float bottomInset = Math.max(18f, h * 0.028f);
        float sideInset = Math.max(18f, w * 0.03f);
        float moveWidth = Math.max(92f, base * 0.14f);
        float moveHeight = Math.max(88f, base * 0.125f);
        float moveGap = Math.max(12f, base * 0.022f);
        float actionSize = Math.max(86f, base * 0.12f);
        float actionGap = Math.max(14f, base * 0.022f);
        float moveTop = h - bottomInset - moveHeight;
        leftPad.set(sideInset, moveTop, sideInset + moveWidth, moveTop + moveHeight);
        rightPad.set(leftPad.right + moveGap, moveTop, leftPad.right + moveGap + moveWidth, moveTop + moveHeight);
        float clusterRight = w - sideInset;
        float clusterBottom = h - bottomInset;
        chiPad.set(clusterRight - actionSize, clusterBottom - actionSize, clusterRight, clusterBottom);
        attackPad.set(chiPad.left - actionGap - actionSize, clusterBottom - actionSize, chiPad.left - actionGap, clusterBottom);
        dodgePad.set(clusterRight - actionSize, chiPad.top - actionGap - actionSize, clusterRight, chiPad.top - actionGap);
        jumpPad.set(attackPad.left, dodgePad.top, attackPad.right, dodgePad.bottom);
    }

    private void clearGameplayTouchState() {
        input.left = false;
        input.right = false;
        jumpHeld = false;
        dodgeHeld = false;
        attackHeld = false;
        chiHeld = false;
    }

    private void updateGameplayTouchState(MotionEvent event) {
        layoutGameplayTouchZones();
        boolean nextLeft = false;
        boolean nextRight = false;
        boolean nextJump = false;
        boolean nextDodge = false;
        boolean nextAttack = false;
        boolean nextChi = false;
        int skipIndex = -1;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            skipIndex = event.getActionIndex();
        }
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (i == skipIndex) {
                continue;
            }
            float px = event.getX(i);
            float py = event.getY(i);
            if (leftPad.contains(px, py)) {
                nextLeft = true;
            }
            if (rightPad.contains(px, py)) {
                nextRight = true;
            }
            if (jumpPad.contains(px, py)) {
                nextJump = true;
            }
            if (dodgePad.contains(px, py)) {
                nextDodge = true;
            }
            if (attackPad.contains(px, py)) {
                nextAttack = true;
            }
            if (chiPad.contains(px, py)) {
                nextChi = true;
            }
        }
        input.left = nextLeft;
        input.right = nextRight;
        if (nextJump && !jumpHeld) {
            input.jump = true;
        }
        if (nextDodge && !dodgeHeld) {
            input.dodge = true;
        }
        if (nextAttack && !attackHeld) {
            input.attack = true;
        }
        if (nextChi && !chiHeld) {
            input.chi = true;
        }
        jumpHeld = nextJump;
        dodgeHeld = nextDodge;
        attackHeld = nextAttack;
        chiHeld = nextChi;
    }

    private void drawMenu(Canvas canvas) {
        paint.setColor(Color.argb(232, 7, 10, 20));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setColor(Color.argb(236, 12, 22, 44));
        rect.set(getWidth() * 0.12f, getHeight() * 0.12f, getWidth() * 0.88f, getHeight() * 0.86f);
        canvas.drawRoundRect(rect, 26f, 26f, paint);
        paint.setColor(Color.argb(64, 110, 246, 255));
        canvas.drawCircle(rect.right - 120f, rect.top + 110f, 86f + (float) Math.sin(titlePulse * 2f) * 10f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(50f);
        canvas.drawText(getContext().getString(R.string.app_name), rect.left + 34f, rect.top + 76f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(18f);
        canvas.drawText("Fantasy combo brawler", rect.left + 34f, rect.top + 110f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(22f);
        canvas.drawText("Carry light strings into launchers, air follow-ups, and dragon-chi finishers.", rect.left + 34f, rect.top + 152f, paint);
        menuPrimary.set(rect.left + 34f, rect.top + 190f, rect.left + 360f, rect.top + 258f);
        menuSecondary.set(rect.left + 34f, rect.top + 276f, rect.left + 360f, rect.top + 344f);
        menuUpgrade.set(rect.left + 34f, rect.top + 366f, rect.left + 192f, rect.top + 420f);
        menuCodex.set(rect.left + 204f, rect.top + 366f, rect.left + 362f, rect.top + 420f);
        menuSettings.set(rect.left + 34f, rect.top + 434f, rect.left + 362f, rect.top + 488f);
        drawActionButton(canvas, menuPrimary, getContext().getString(R.string.btn_start), true);
        drawActionButton(canvas, menuSecondary, getContext().getString(R.string.btn_how_to_play), false);
        drawActionButton(canvas, menuUpgrade, "Upgrades", false);
        drawActionButton(canvas, menuCodex, "Codex", false);
        drawActionButton(canvas, menuSettings, musicOn ? "Settings: Music On" : "Settings: Music Off", false);
        paint.setColor(Color.argb(196, 12, 22, 40));
        rect.set(rect.left + 420f, rect.top + 184f, rect.right - 34f, rect.bottom - 34f);
        canvas.drawRoundRect(rect, 22f, 22f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(24f);
        canvas.drawText("Lane Rhythm", rect.left + 26f, rect.top + 44f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(20f);
        canvas.drawText("Light chain to launcher, jump to continue, dodge to stay in range.", rect.left + 26f, rect.top + 84f, paint);
        canvas.drawText("Spend chi for a bright wave that clears the whole forward corridor.", rect.left + 26f, rect.top + 118f, paint);
        canvas.drawText("Six chapter routes, post-clear upgrades, and a compact enemy codex are live.", rect.left + 26f, rect.top + 152f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(18f);
        canvas.drawText("Gold " + gold + "   Scrolls " + techniqueScrolls + "   Crests " + crestShards, rect.left + 26f, rect.top + 198f, paint);
        if (showHowTo) {
            paint.setColor(Color.argb(224, 18, 30, 56));
            rect.set(rect.left + 16f, rect.top + 232f, rect.right - 16f, rect.bottom - 18f);
            canvas.drawRoundRect(rect, 18f, 18f, paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
            paint.setTextSize(21f);
            canvas.drawText("L and R keep pressure on the lane.", rect.left + 20f, rect.top + 42f, paint);
            canvas.drawText("J jumps over hits and keeps air routes alive.", rect.left + 20f, rect.top + 76f, paint);
            canvas.drawText("D slips through retaliation and resets your angle.", rect.left + 20f, rect.top + 110f, paint);
            canvas.drawText("A chains into a 4-step route with a launcher on hit three.", rect.left + 20f, rect.top + 144f, paint);
            canvas.drawText("Q releases dragon chi across the whole forward lane.", rect.left + 20f, rect.top + 178f, paint);
        }
    }

    private void drawChapterSelect(Canvas canvas) {
        paint.setColor(Color.argb(236, 7, 10, 20));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        rect.set(getWidth() * 0.12f, getHeight() * 0.12f, getWidth() * 0.88f, getHeight() * 0.86f);
        paint.setColor(Color.argb(240, 12, 22, 44));
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(42f);
        canvas.drawText("Chapter Select", rect.left + 34f, rect.top + 68f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(20f);
        canvas.drawText("Pick a route pressure tier. Six chapters escalate density, hazards, and boss escort pressure.", rect.left + 34f, rect.top + 104f, paint);
        float cardTop = rect.top + 146f;
        float cardWidth = (rect.width() - 96f) / 3f;
        float cardHeight = (rect.height() - 244f) / 2f;
        chapterCardA.set(rect.left + 24f, cardTop, rect.left + 24f + cardWidth, cardTop + cardHeight);
        chapterCardB.set(chapterCardA.right + 24f, cardTop, chapterCardA.right + 24f + cardWidth, cardTop + cardHeight);
        chapterCardC.set(chapterCardB.right + 24f, cardTop, chapterCardB.right + 24f + cardWidth, cardTop + cardHeight);
        chapterCardD.set(rect.left + 24f, chapterCardA.bottom + 20f, rect.left + 24f + cardWidth, chapterCardA.bottom + 20f + cardHeight);
        chapterCardE.set(chapterCardD.right + 24f, chapterCardA.bottom + 20f, chapterCardD.right + 24f + cardWidth, chapterCardA.bottom + 20f + cardHeight);
        chapterCardF.set(chapterCardE.right + 24f, chapterCardA.bottom + 20f, chapterCardE.right + 24f + cardWidth, chapterCardA.bottom + 20f + cardHeight);
        drawChapterCard(canvas, chapterCardA, "Chapter 1", "Ash Gate", "Best for learning launcher flow and first chi confirms.");
        drawChapterCard(canvas, chapterCardB, "Chapter 2", "Moon Terrace", "More flyers and tighter pressure windows.");
        drawChapterCard(canvas, chapterCardC, "Chapter 3", "Volcanic Seal", "Higher brute pressure and denser escorts.");
        drawChapterCard(canvas, chapterCardD, "Chapter 4", "Bridge Ruin", "Assassins fold in from the flanks.");
        drawChapterCard(canvas, chapterCardE, "Chapter 5", "Shrine Veil", "Rangers and shrine raiders pin the lane.");
        drawChapterCard(canvas, chapterCardF, "Chapter 6", "Dragon Gate", "Full route density before the final vassal.");
        chapterBack.set(rect.left + 24f, rect.top + 18f, rect.left + 132f, rect.top + 64f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(chapterBack, 14f, 14f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(18f);
        canvas.drawText("Back", chapterBack.left + 28f, chapterBack.centerY() + 7f, paint);
    }

    private void drawChapterCard(Canvas canvas, RectF card, String title, String subtitle, String body) {
        paint.setColor(Color.argb(204, 16, 28, 54));
        canvas.drawRoundRect(card, 22f, 22f, paint);
        paint.setColor(Color.argb(72, 110, 246, 255));
        canvas.drawCircle(card.centerX(), card.top + 92f, 54f + (float) Math.sin(titlePulse * 2f + card.centerX() * 0.01f) * 6f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(28f);
        canvas.drawText(title, card.left + 24f, card.top + 52f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(20f);
        canvas.drawText(subtitle, card.left + 24f, card.top + 88f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(17f);
        canvas.drawText(body, card.left + 24f, card.top + 188f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_primary_bg_end));
        canvas.drawRoundRect(card.left + 24f, card.bottom - 78f, card.right - 24f, card.bottom - 24f, 16f, 16f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
        paint.setTextSize(22f);
        canvas.drawText("Enter Route", card.left + 42f, card.bottom - 42f, paint);
    }

    private void drawLoadout(Canvas canvas) {
        paint.setColor(Color.argb(236, 7, 10, 20));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        rect.set(getWidth() * 0.14f, getHeight() * 0.14f, getWidth() * 0.86f, getHeight() * 0.86f);
        paint.setColor(Color.argb(240, 12, 22, 44));
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(40f);
        canvas.drawText("Hero Loadout", rect.left + 30f, rect.top + 64f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(18f);
        canvas.drawText("Gold " + gold + "   Scrolls " + techniqueScrolls + "   Crests " + crestShards, rect.left + 30f, rect.top + 100f, paint);
        panelPrimary.set(rect.left + 24f, rect.top + 140f, rect.left + rect.width() / 3f - 8f, rect.bottom - 40f);
        panelSecondary.set(panelPrimary.right + 16f, rect.top + 140f, rect.left + rect.width() * 2f / 3f - 8f, rect.bottom - 40f);
        panelTertiary.set(panelSecondary.right + 16f, rect.top + 140f, rect.right - 24f, rect.bottom - 40f);
        drawUpgradePanel(canvas, panelPrimary, "Combo Extension", attackLevel, "Spend gold to raise combo damage and finish range.");
        drawUpgradePanel(canvas, panelSecondary, "Chi Mastery", chiLevel, "Spend scrolls to grow chi capacity and burst uptime.");
        drawUpgradePanel(canvas, panelTertiary, "Survivability", vitalityLevel, "Spend crests to raise vitality and route stability.");
        panelBack.set(rect.left + 24f, rect.top + 18f, rect.left + 132f, rect.top + 64f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(panelBack, 14f, 14f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(18f);
        canvas.drawText("Back", panelBack.left + 28f, panelBack.centerY() + 7f, paint);
    }

    private void drawUpgradePanel(Canvas canvas, RectF box, String title, int level, String body) {
        paint.setColor(Color.argb(208, 18, 30, 56));
        canvas.drawRoundRect(box, 20f, 20f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(24f);
        canvas.drawText(title, box.left + 20f, box.top + 42f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(20f);
        canvas.drawText("Level " + level, box.left + 20f, box.top + 76f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(18f);
        canvas.drawText(body, box.left + 20f, box.top + 122f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_primary_bg_end));
        canvas.drawRoundRect(box.left + 20f, box.bottom - 72f, box.right - 20f, box.bottom - 22f, 14f, 14f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
        paint.setTextSize(20f);
        canvas.drawText("Upgrade", box.left + 34f, box.bottom - 40f, paint);
    }

    private void drawCodex(Canvas canvas) {
        paint.setColor(Color.argb(236, 7, 10, 20));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        rect.set(getWidth() * 0.14f, getHeight() * 0.14f, getWidth() * 0.86f, getHeight() * 0.86f);
        paint.setColor(Color.argb(240, 12, 22, 44));
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(40f);
        canvas.drawText(codexTitle, rect.left + 30f, rect.top + 64f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(20f);
        canvas.drawText("Scout, brute, ranger, assassin, flyer, and gate captain notes for route reading.", rect.left + 30f, rect.top + 100f, paint);
        paint.setColor(Color.argb(208, 18, 30, 56));
        canvas.drawRoundRect(rect.left + 24f, rect.top + 136f, rect.right - 24f, rect.bottom - 36f, 20f, 20f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(22f);
        canvas.drawText("Enemy Families", rect.left + 44f, rect.top + 176f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(19f);
        canvas.drawText("Scout: fast contact fighter that feeds your opener route.", rect.left + 44f, rect.top + 216f, paint);
        canvas.drawText("Brute: slower armor body that wants to break your spacing.", rect.left + 44f, rect.top + 252f, paint);
        canvas.drawText("Ranger: long reach pressure that forces dodges or fast launchers.", rect.left + 44f, rect.top + 288f, paint);
        canvas.drawText("Assassin: quick re-entry threat that punishes dropped combo timing.", rect.left + 44f, rect.top + 324f, paint);
        canvas.drawText("Flyer: air lane flanker that extends pincer pressure from above.", rect.left + 44f, rect.top + 360f, paint);
        canvas.drawText("Gate Captain: boss anchor with escort overlap and heavy punish windows.", rect.left + 44f, rect.top + 396f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(22f);
        canvas.drawText("Technique Notes", rect.left + 44f, rect.top + 448f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(19f);
        canvas.drawText("Launcher on combo hit three, jump for air chase, then cash out with chi.", rect.left + 44f, rect.top + 488f, paint);
        canvas.drawText("Dodge cancel repositions your route when escorts close from both sides.", rect.left + 44f, rect.top + 524f, paint);
        panelBack.set(rect.left + 24f, rect.top + 18f, rect.left + 132f, rect.top + 64f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(panelBack, 14f, 14f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(18f);
        canvas.drawText("Back", panelBack.left + 28f, panelBack.centerY() + 7f, paint);
    }

    private void drawResult(Canvas canvas) {
        drawOverlay(canvas, "Chapter Clear", resultSummary);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(20f);
        canvas.drawText("Gold +" + clearGoldReward + "   Scrolls +" + clearScrollReward + "   Crests +" + clearShardReward, rect.left + 28f, rect.top + 182f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(18f);
        canvas.drawText("Upgrades unlocked in the loadout screen before the next route.", rect.left + 28f, rect.top + 214f, paint);
    }

    private void drawPause(Canvas canvas) {
        paint.setColor(Color.argb(224, 7, 10, 20));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        rect.set(getWidth() * 0.24f, 120f, getWidth() * 0.76f, 468f);
        paint.setColor(Color.argb(240, 16, 24, 46));
        canvas.drawRoundRect(rect, 20f, 20f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(38f);
        canvas.drawText("Paused", rect.left + 26f, rect.top + 54f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(20f);
        canvas.drawText("Resume to keep the route alive or restart the chapter push.", rect.left + 26f, rect.top + 88f, paint);
        canvas.drawText("Controls: L/R move, J jump, D dodge, A combo, Q chi.", rect.left + 26f, rect.top + 120f, paint);
        canvas.drawText("Audio: Music " + (musicOn ? "On" : "Off") + "   SFX " + (sfxOn ? "On" : "Off"), rect.left + 26f, rect.top + 152f, paint);
        canvas.drawText("Objective: " + objectiveText, rect.left + 26f, rect.top + 184f, paint);
        menuPrimary.set(rect.left + 26f, rect.top + 224f, rect.right - 26f, rect.top + 286f);
        menuSecondary.set(rect.left + 26f, rect.top + 304f, rect.right - 26f, rect.top + 366f);
        drawActionButton(canvas, menuPrimary, getContext().getString(R.string.btn_resume), true);
        drawActionButton(canvas, menuSecondary, (musicOn ? "Mute Music" : "Unmute Music"), false);
        panelTertiary.set(rect.left + 26f, rect.top + 384f, rect.right - 26f, rect.top + 446f);
        drawActionButton(canvas, panelTertiary, getContext().getString(R.string.btn_restart), false);
    }

    private void drawOverlay(Canvas canvas, String title, String body) {
        paint.setColor(Color.argb(228, 7, 10, 20));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        rect.set(getWidth() * 0.22f, 150f, getWidth() * 0.78f, 458f);
        paint.setColor(Color.argb(240, 16, 24, 46));
        canvas.drawRoundRect(rect, 20f, 20f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(44f);
        canvas.drawText(title, rect.left + 28f, rect.top + 68f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(22f);
        canvas.drawText(body, rect.left + 28f, rect.top + 108f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        paint.setTextSize(18f);
        canvas.drawText("Score " + score + "   Style " + getStyleRank() + "   Combo " + player.comboCount, rect.left + 28f, rect.top + 148f, paint);
        menuPrimary.set(rect.left + 28f, rect.top + 186f, rect.right - 28f, rect.top + 248f);
        menuSecondary.set(rect.left + 28f, rect.top + 260f, rect.right - 28f, rect.top + 322f);
        drawActionButton(canvas, menuPrimary, getContext().getString(R.string.btn_restart), true);
        drawActionButton(canvas, menuSecondary, getContext().getString(R.string.btn_menu), false);
    }

    private void drawActionButton(Canvas canvas, RectF button, String text, boolean primary) {
        paint.setColor(primary ? ContextCompat.getColor(getContext(), R.color.cst_btn_primary_bg_end) : ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(button, 16f, 16f, paint);
        paint.setColor(primary ? ContextCompat.getColor(getContext(), R.color.cst_text_on_primary) : ContextCompat.getColor(getContext(), R.color.cst_text_on_secondary));
        paint.setTextSize(24f);
        canvas.drawText(text, button.left + 24f, button.centerY() + 8f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getActionMasked();
        if (state == GameState.MENU) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (menuPrimary.contains(x, y)) {
                    state = GameState.CHAPTER_SELECT;
                } else if (menuSecondary.contains(x, y)) {
                    startRun(true, 1);
                } else if (menuUpgrade.contains(x, y)) {
                    state = GameState.LOADOUT;
                } else if (menuCodex.contains(x, y)) {
                    state = GameState.CODEX;
                } else if (menuSettings.contains(x, y)) {
                    musicOn = !musicOn;
                }
            }
            return true;
        }
        if (state == GameState.CHAPTER_SELECT) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (chapterBack.contains(x, y)) {
                    state = GameState.MENU;
                } else if (chapterCardA.contains(x, y)) {
                    startRun(false, 1);
                } else if (chapterCardB.contains(x, y)) {
                    startRun(false, 2);
                } else if (chapterCardC.contains(x, y)) {
                    startRun(false, 3);
                } else if (chapterCardD.contains(x, y)) {
                    startRun(false, 4);
                } else if (chapterCardE.contains(x, y)) {
                    startRun(false, 5);
                } else if (chapterCardF.contains(x, y)) {
                    startRun(false, 6);
                }
            }
            return true;
        }
        if (state == GameState.LOADOUT) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (panelBack.contains(x, y)) {
                    state = GameState.MENU;
                } else if (panelPrimary.contains(x, y) && gold >= 60 + attackLevel * 20) {
                    gold -= 60 + attackLevel * 20;
                    attackLevel++;
                    syncPlayerGrowth();
                } else if (panelSecondary.contains(x, y) && techniqueScrolls > 0) {
                    techniqueScrolls--;
                    chiLevel++;
                    syncPlayerGrowth();
                } else if (panelTertiary.contains(x, y) && crestShards > 0) {
                    crestShards--;
                    vitalityLevel++;
                    syncPlayerGrowth();
                }
            }
            return true;
        }
        if (state == GameState.CODEX) {
            if (action == MotionEvent.ACTION_DOWN && panelBack.contains(x, y)) {
                state = GameState.MENU;
            }
            return true;
        }
        if (state == GameState.PAUSED) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (menuPrimary.contains(x, y)) {
                    state = trainingMode ? GameState.TRAINING : GameState.PLAYING;
                } else if (menuSecondary.contains(x, y)) {
                    musicOn = !musicOn;
                } else if (panelTertiary.contains(x, y)) {
                    startRun(trainingMode, chapterIndex);
                }
            }
            return true;
        }
        if (state == GameState.GAME_OVER || state == GameState.RESULT || state == GameState.STAGE_CLEAR) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (menuPrimary.contains(x, y)) {
                    startRun(trainingMode, chapterIndex);
                } else if (menuSecondary.contains(x, y)) {
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.PLAYING || state == GameState.TRAINING) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                if (pausePad.contains(x, y)) {
                    state = GameState.PAUSED;
                    clearGameplayTouchState();
                    return true;
                }
                updateGameplayTouchState(event);
            } else if (action == MotionEvent.ACTION_CANCEL) {
                clearGameplayTouchState();
            }
        }
        return true;
    }
}
