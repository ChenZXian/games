package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.TonePlayer;
import com.android.boot.core.CollisionHelper;
import com.android.boot.core.GameLoopThread;
import com.android.boot.core.GameState;
import com.android.boot.core.InputController;
import com.android.boot.core.LevelDefinition;
import com.android.boot.core.LevelManager;
import com.android.boot.core.LevelProgress;
import com.android.boot.core.SaveRepository;
import com.android.boot.entity.BadgePickup;
import com.android.boot.entity.CollapseTile;
import com.android.boot.entity.ElectricTile;
import com.android.boot.entity.ExitGate;
import com.android.boot.entity.GameEntity;
import com.android.boot.entity.KeyPickup;
import com.android.boot.entity.LaserGate;
import com.android.boot.entity.PatrolDrone;
import com.android.boot.entity.Player;
import com.android.boot.entity.RotatingGear;
import com.android.boot.entity.CheckpointPad;
import com.android.boot.entity.ShieldZone;
import com.android.boot.fx.FloatingTextSystem;
import com.android.boot.fx.ParticleSystem;

import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final Paint paintBg = new Paint();
    private final Paint paintPanel = new Paint();
    private final Paint paintAccent = new Paint();
    private final Paint paintDanger = new Paint();
    private final Paint paintSuccess = new Paint();
    private final Paint paintWarning = new Paint();
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final InputController inputController = new InputController();
    private final LevelManager levelManager;
    private final SaveRepository saveRepository;
    private final TonePlayer tonePlayer = new TonePlayer();

    private final List<GameEntity> hazards = new ArrayList<>();
    private final List<BadgePickup> badges = new ArrayList<>();
    private final List<CheckpointPad> checkpoints = new ArrayList<>();

    private final Player player;
    private ExitGate exitGate;
    private KeyPickup keyPickup;
    private ShieldZone shieldZone;
    private final ParticleSystem particleSystem;
    private final FloatingTextSystem floatingTextSystem;

    private GameLoopThread gameLoopThread;
    private GameState state = GameState.MENU;
    private LevelProgress levelProgress;
    private int levelIndex;
    private LevelDefinition level;
    private float timer;
    private int score;
    private int badgesCollected;
    private float respawnX;
    private float respawnY;
    private float shake;

    private final RectF worldRect = new RectF();

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        levelManager = new LevelManager(context);
        saveRepository = new SaveRepository(context);
        levelProgress = saveRepository.loadProgress();
        tonePlayer.setMuted(saveRepository.isMuted());

        paintBg.setColor(ContextCompat.getColor(context, R.color.cst_bg_alt));
        paintPanel.setColor(ContextCompat.getColor(context, R.color.cst_panel_bg));
        paintAccent.setColor(ContextCompat.getColor(context, R.color.cst_accent));
        paintDanger.setColor(ContextCompat.getColor(context, R.color.cst_danger));
        paintSuccess.setColor(ContextCompat.getColor(context, R.color.cst_success));
        paintWarning.setColor(ContextCompat.getColor(context, R.color.cst_warning));
        paintText.setColor(ContextCompat.getColor(context, R.color.cst_text_primary));
        paintText.setTextSize(getResources().getDimension(R.dimen.cst_text_m));

        player = new Player(paintAccent);
        particleSystem = new ParticleSystem(paintAccent);
        floatingTextSystem = new FloatingTextSystem(paintText);
        loadLevel(0);
    }

    private void loadLevel(int index) {
        levelIndex = Math.max(0, Math.min(index, levelManager.size() - 1));
        level = levelManager.get(levelIndex);
        timer = level.timerLimit;
        score = 0;
        badgesCollected = 0;
        hazards.clear();
        badges.clear();
        checkpoints.clear();
        player.health = player.maxHealth;
        player.energy = player.maxEnergy;
        player.hasKey = false;
        player.setPosition(level.start.x, level.start.y);
        respawnX = level.start.x;
        respawnY = level.start.y;
        exitGate = new ExitGate(level.exit.x, level.exit.y, paintAccent);
        exitGate.unlocked = !level.keyRequired;
        keyPickup = level.keyPosition == null ? null : new KeyPickup(level.keyPosition.x, level.keyPosition.y, paintWarning);
        shieldZone = null;
        for (LevelDefinition.HazardDef h : level.hazards) {
            switch (h.type) {
                case "laser": hazards.add(new LaserGate(h.x, h.y, h.w, h.h, h.p1, paintDanger)); break;
                case "electric": hazards.add(new ElectricTile(h.x, h.y, h.w, h.h, h.p1, h.p2, paintDanger)); break;
                case "gear": hazards.add(new RotatingGear(h.x, h.y, h.w, h.p2, paintDanger)); break;
                case "patrol": hazards.add(new PatrolDrone(h.x, h.y, h.w, h.h, h.p1, 20f + h.p2 * 6f, paintDanger)); break;
                case "collapse": hazards.add(new CollapseTile(h.x, h.y, h.w, h.h, h.p1, h.p2, paintDanger)); break;
                case "shield": shieldZone = new ShieldZone(h.x, h.y, h.w, h.h, paintSuccess); break;
            }
        }
        for (android.graphics.PointF b : level.badgePositions) badges.add(new BadgePickup(b.x, b.y, paintWarning));
        for (android.graphics.PointF p : level.checkpoints) checkpoints.add(new CheckpointPad(p.x, p.y, paintSuccess));
    }

    public void step(float dt) {
        if (!getHolder().getSurface().isValid()) {
            return;
        }
        update(dt);
        Canvas c = getHolder().lockCanvas();
        if (c != null) {
            render(c);
            getHolder().unlockCanvasAndPost(c);
        }
    }

    private void update(float dt) {
        if (state == GameState.PLAYING) {
            timer -= dt;
            if (timer <= 0 || player.health <= 0) {
                state = GameState.GAME_OVER;
            }
            player.update(dt);
            player.move(inputController.moveX, inputController.moveY, dt, level.stageW, level.stageH);
            if (inputController.dashPressed && player.tryDash()) {
                tonePlayer.button();
            }
            boolean shielded = shieldZone != null && CollisionHelper.overlaps(player.bounds, shieldZone.bounds);
            for (GameEntity e : hazards) {
                e.update(dt);
                if (e.active && CollisionHelper.overlaps(player.bounds, e.bounds)) {
                    boolean harmful = true;
                    if (e instanceof LaserGate) harmful = ((LaserGate) e).isDangerOn();
                    if (e instanceof ElectricTile) harmful = ((ElectricTile) e).isOn();
                    if (e instanceof CollapseTile) harmful = !((CollapseTile) e).isSolid();
                    if (harmful && !shielded) {
                        player.damage(20);
                        particleSystem.burst(player.bounds.centerX(), player.bounds.centerY(), 8, 34f);
                        tonePlayer.damage();
                        shake = 0.2f;
                    }
                }
            }
            for (BadgePickup b : badges) {
                if (b.active && CollisionHelper.overlaps(player.bounds, b.bounds)) {
                    b.active = false;
                    badgesCollected++;
                    score += 150;
                    floatingTextSystem.push("+150", b.bounds.centerX(), b.bounds.centerY());
                    particleSystem.burst(b.bounds.centerX(), b.bounds.centerY(), 10, 26f);
                    tonePlayer.pickup();
                }
            }
            for (CheckpointPad cp : checkpoints) {
                if (!cp.triggered && CollisionHelper.overlaps(player.bounds, cp.bounds)) {
                    cp.triggered = true;
                    respawnX = cp.bounds.centerX();
                    respawnY = cp.bounds.centerY();
                }
            }
            if (keyPickup != null && keyPickup.active && CollisionHelper.overlaps(player.bounds, keyPickup.bounds)) {
                keyPickup.active = false;
                player.hasKey = true;
                exitGate.unlocked = true;
                score += 100;
            }
            if (player.health <= 0 && !checkpoints.isEmpty()) {
                player.setPosition(respawnX, respawnY);
                player.health = 50f;
            }
            if (CollisionHelper.overlaps(player.bounds, exitGate.bounds) && exitGate.unlocked) {
                levelCleared();
            }
            particleSystem.update(dt);
            floatingTextSystem.update(dt);
            if (shake > 0) shake -= dt;
        }
        if (inputController.pausePressed && state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (inputController.pausePressed && state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
        inputController.resetFrameFlags();
    }

    private void levelCleared() {
        int stars = 1;
        if (timer > level.timerLimit * 0.55f && badgesCollected >= 2 && player.health >= 70) stars = 3;
        else if (timer > level.timerLimit * 0.3f) stars = 2;
        score += Math.max(0, (int) (timer * 10f)) + badgesCollected * 200 + (int) player.health;
        int idx = level.id - 1;
        if (idx >= 0 && idx < levelProgress.bestStars.length) {
            levelProgress.bestStars[idx] = Math.max(levelProgress.bestStars[idx], stars);
        }
        levelProgress.unlockedLevel = Math.max(levelProgress.unlockedLevel, Math.min(6, level.id + 1));
        saveRepository.saveProgress(levelProgress);
        tonePlayer.clear();
        state = GameState.GAME_OVER;
    }

    private void render(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), paintBg);
        float scale = Math.min(getWidth() / level.stageW, getHeight() / level.stageH);
        float ox = (getWidth() - level.stageW * scale) * 0.5f;
        float oy = (getHeight() - level.stageH * scale) * 0.5f;
        worldRect.set(ox, oy, ox + level.stageW * scale, oy + level.stageH * scale);
        float sx = shake > 0 ? (float) Math.sin(System.nanoTime() * 0.00000005f) * 4f : 0f;
        float sy = shake > 0 ? (float) Math.cos(System.nanoTime() * 0.00000006f) * 4f : 0f;
        canvas.save();
        canvas.translate(ox + sx, oy + sy);
        canvas.scale(scale, scale);

        canvas.drawRect(0, 0, level.stageW, level.stageH, paintPanel);
        for (int i = 0; i < 11; i++) {
            float y = i * (level.stageH / 10f);
            paintPanel.setAlpha(40);
            canvas.drawLine(0, y, level.stageW, y, paintPanel);
        }
        paintPanel.setAlpha(255);
        for (GameEntity h : hazards) h.render(canvas);
        if (shieldZone != null) shieldZone.render(canvas);
        for (BadgePickup b : badges) b.render(canvas);
        for (CheckpointPad cp : checkpoints) cp.render(canvas);
        if (keyPickup != null) keyPickup.render(canvas);
        exitGate.render(canvas);
        player.render(canvas);
        particleSystem.render(canvas);
        floatingTextSystem.render(canvas);

        canvas.restore();
        drawHud(canvas);
        drawOverlays(canvas);
    }

    private void drawHud(Canvas canvas) {
        float pad = getResources().getDimension(R.dimen.cst_pad_10);
        float barW = getWidth() * 0.28f;
        float y = pad * 1.7f;
        paintText.setTextSize(getResources().getDimension(R.dimen.cst_text_s));
        canvas.drawText(level.name, pad, y, paintText);
        float healthRatio = player.health / player.maxHealth;
        float energyRatio = player.energy / player.maxEnergy;
        y += 22;
        paintDanger.setAlpha(110);
        canvas.drawRect(pad, y, pad + barW, y + 10, paintDanger);
        paintDanger.setAlpha(255);
        canvas.drawRect(pad, y, pad + barW * healthRatio, y + 10, paintDanger);
        y += 18;
        paintSuccess.setAlpha(100);
        canvas.drawRect(pad, y, pad + barW, y + 10, paintSuccess);
        paintSuccess.setAlpha(255);
        canvas.drawRect(pad, y, pad + barW * energyRatio, y + 10, paintSuccess);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(getResources().getString(R.string.time) + " " + (int) timer, getWidth() - pad, pad * 2.2f, paintText);
        canvas.drawText(getResources().getString(R.string.score) + " " + score, getWidth() - pad, pad * 4.2f, paintText);
        paintText.setTextAlign(Paint.Align.LEFT);
    }

    private void drawOverlays(Canvas canvas) {
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(getResources().getDimension(R.dimen.cst_text_l));
        if (state == GameState.MENU) {
            canvas.drawText(getResources().getString(R.string.app_name), getWidth() * 0.5f, getHeight() * 0.3f, paintText);
            paintText.setTextSize(getResources().getDimension(R.dimen.cst_text_m));
            canvas.drawText(getResources().getString(R.string.tagline), getWidth() * 0.5f, getHeight() * 0.36f, paintText);
            canvas.drawText(getResources().getString(R.string.tap_to_start), getWidth() * 0.5f, getHeight() * 0.56f, paintText);
        } else if (state == GameState.PAUSED) {
            canvas.drawText(getResources().getString(R.string.paused), getWidth() * 0.5f, getHeight() * 0.5f, paintText);
        } else if (state == GameState.GAME_OVER) {
            String title = timer > 0 && player.health > 0 ? getResources().getString(R.string.cleared) : getResources().getString(R.string.failed);
            canvas.drawText(title, getWidth() * 0.5f, getHeight() * 0.44f, paintText);
            paintText.setTextSize(getResources().getDimension(R.dimen.cst_text_m));
            canvas.drawText(getResources().getString(R.string.badges) + " " + badgesCollected, getWidth() * 0.5f, getHeight() * 0.50f, paintText);
            canvas.drawText(getResources().getString(R.string.score) + " " + score, getWidth() * 0.5f, getHeight() * 0.56f, paintText);
        }
        paintText.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float actionX = getWidth() - getWidth() * 0.17f;
        float actionY = getHeight() - getHeight() * 0.16f;
        float actionR = getWidth() * 0.1f;
        float pauseX = getWidth() - getWidth() * 0.08f;
        float pauseY = getWidth() * 0.08f;
        float pauseR = getWidth() * 0.05f;
        inputController.setJoystick(getWidth() * 0.16f, getHeight() - getHeight() * 0.16f, getWidth() * 0.11f);
        inputController.onTouch(event, actionX, actionY, actionR, pauseX, pauseY, pauseR);
        if ((event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) && state == GameState.MENU) {
            state = GameState.PLAYING;
        }
        if ((event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) && state == GameState.GAME_OVER) {
            if (timer > 0 && player.health > 0 && levelIndex + 1 < levelProgress.unlockedLevel && levelIndex < 5) {
                loadLevel(levelIndex + 1);
            } else {
                loadLevel(levelIndex);
            }
            state = GameState.PLAYING;
        }
        return true;
    }

    public void onHostResume() {
        if (gameLoopThread == null) {
            gameLoopThread = new GameLoopThread(this);
            gameLoopThread.setRunning(true);
            gameLoopThread.start();
        }
    }

    public void onHostPause() {
        if (gameLoopThread != null) {
            gameLoopThread.setRunning(false);
            try {
                gameLoopThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            gameLoopThread = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        onHostResume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        onHostPause();
    }
}
