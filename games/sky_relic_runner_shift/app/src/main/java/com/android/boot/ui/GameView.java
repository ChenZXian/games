package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.SoundManager;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.core.Timer;
import com.android.boot.entity.Obstacle;
import com.android.boot.entity.Orb;
import com.android.boot.entity.Player;
import com.android.boot.fx.Particle;

import java.util.Locale;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface GameEventListener {
        void onStateChanged(GameState state);
        void onHudUpdate(String score, String best, String distance, String energy, String speed, String shield);
        void onGameOver(String finalScore, String finalBest);
    }

    private final Timer timer = new Timer();
    private final Paint paint = new Paint();
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final float[] starsX = new float[80];
    private final float[] starsY = new float[80];
    private final float[] starsSpeed = new float[80];

    private Thread thread;
    private boolean running;
    private GameEngine engine;
    private GameEventListener listener;
    private SoundManager soundManager;
    private GameState lastState = GameState.MENU;
    private int lastScore;
    private float width;
    private float height;
    private LinearGradient background;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
        setFocusable(true);
        engine = new GameEngine(context.getSharedPreferences("sky_relic_runner_shift", Context.MODE_PRIVATE));
        soundManager = new SoundManager(context);
        for (int i = 0; i < starsX.length; i++) {
            starsX[i] = random.nextFloat();
            starsY[i] = random.nextFloat();
            starsSpeed[i] = 0.2f + random.nextFloat() * 0.8f;
        }
    }

    public void setGameEventListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void startGame() {
        engine.resetRun();
        lastScore = 0;
        setState(GameState.PLAYING);
    }

    public void pauseGame() {
        if (engine.getState() == GameState.PLAYING) {
            setState(GameState.PAUSED);
        }
    }

    public void resumeGame() {
        if (engine.getState() == GameState.PAUSED) {
            setState(GameState.PLAYING);
        }
    }

    public void resumeIfVisible() {
        if (engine.getState() == GameState.PAUSED) {
            setState(GameState.PLAYING);
        }
    }

    public void restartGame() {
        engine.resetRun();
        lastScore = 0;
        setState(GameState.PLAYING);
    }

    public void backToMenu() {
        lastScore = 0;
        setState(GameState.MENU);
    }

    public void onJumpPress() {
        engine.jumpPress();
    }

    public void onJumpRelease() {
        engine.jumpRelease();
        if (engine.getState() == GameState.PLAYING) {
            soundManager.playFlip();
        }
    }

    public void onDash() {
        if (engine.dash()) {
            soundManager.playFlip();
        }
    }

    public void release() {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
        soundManager.release();
    }

    private void setState(GameState state) {
        engine.setState(state);
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.width = width;
        this.height = height;
        engine.resize(width, height);
        background = new LinearGradient(0f, 0f, 0f, height,
                getResources().getColor(com.android.boot.R.color.cst_bg_start),
                getResources().getColor(com.android.boot.R.color.cst_bg_end),
                Shader.TileMode.CLAMP);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        timer.reset(System.currentTimeMillis());
        while (running) {
            float dt = timer.step(System.currentTimeMillis());
            if (dt > 0.05f) {
                dt = 0.05f;
            }
            updateStars(dt);
            engine.update(dt);
            notifyHud();
            handleStateChange();
            drawFrame();
        }
    }

    private void updateStars(float dt) {
        float speed = 0.2f + engine.getSpeed() * 0.0006f;
        for (int i = 0; i < starsX.length; i++) {
            starsX[i] -= speed * starsSpeed[i] * dt;
            if (starsX[i] < 0f) {
                starsX[i] += 1f;
                starsY[i] = random.nextFloat();
                starsSpeed[i] = 0.2f + random.nextFloat() * 0.8f;
            }
        }
    }

    private void notifyHud() {
        if (listener == null) {
            return;
        }
        int score = engine.getScore();
        int best = engine.getBestScore();
        int energy = engine.getEnergy();
        int bestDistance = engine.getBestDistance();
        String scoreLabel = "Score " + score;
        String bestLabel = "Best " + best;
        String distanceLabel = "Distance " + Math.round(engine.getDistance());
        String energyLabel = "Energy " + energy;
        String speedLabel = String.format(Locale.US, "Speed %.1f", engine.getSpeed() / 520f);
        String shieldLabel = engine.hasShield() ? "Shield On" : "Shield Off";
        listener.onHudUpdate(scoreLabel, bestLabel, distanceLabel, energyLabel, speedLabel, shieldLabel);
        if (score > lastScore) {
            soundManager.playCollect();
            lastScore = score;
        }
    }

    private void handleStateChange() {
        GameState current = engine.getState();
        if (current != lastState) {
            if (current == GameState.GAME_OVER) {
                soundManager.playHit();
                soundManager.playGameOver();
                if (listener != null) {
                    listener.onGameOver("Score " + engine.getScore(), "Best " + engine.getBestScore());
                }
            }
            if (listener != null) {
                listener.onStateChanged(current);
            }
            lastState = current;
        }
    }

    private void drawFrame() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(background);
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(null);

        paint.setColor(getResources().getColor(com.android.boot.R.color.cst_text_secondary));
        for (int i = 0; i < starsX.length; i++) {
            float x = starsX[i] * width;
            float y = starsY[i] * height;
            float size = 1.5f + starsSpeed[i] * 1.4f;
            canvas.drawCircle(x, y, size, paint);
        }

        drawPlatforms(canvas);
        drawObstacles(canvas);
        drawOrbs(canvas);
        drawParticles(canvas);
        drawPlayer(canvas);

        holder.unlockCanvasAndPost(canvas);
    }

    private void drawPlatforms(Canvas canvas) {
        paint.setColor(getResources().getColor(com.android.boot.R.color.cst_platform));
        for (Obstacle obstacle : engine.getObstacles()) {
            if (!obstacle.active || obstacle.type != Obstacle.Type.PLATFORM) {
                continue;
            }
            rect.set(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height);
            canvas.drawRoundRect(rect, 10f, 10f, paint);
        }
    }

    private void drawObstacles(Canvas canvas) {
        paint.setColor(getResources().getColor(com.android.boot.R.color.cst_hazard));
        for (Obstacle obstacle : engine.getObstacles()) {
            if (!obstacle.active || !obstacle.isHazard()) {
                continue;
            }
            rect.set(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height);
            canvas.drawRoundRect(rect, 8f, 8f, paint);
        }
    }

    private void drawOrbs(Canvas canvas) {
        for (Orb orb : engine.getOrbs()) {
            if (!orb.active) {
                continue;
            }
            if (orb.type == Orb.Type.SHARD) {
                paint.setColor(getResources().getColor(com.android.boot.R.color.cst_orb_shard));
            } else if (orb.type == Orb.Type.CORE) {
                paint.setColor(getResources().getColor(com.android.boot.R.color.cst_orb_core));
            } else {
                paint.setColor(getResources().getColor(com.android.boot.R.color.cst_orb_rune));
            }
            canvas.drawCircle(orb.x, orb.y, orb.radius, paint);
        }
    }

    private void drawParticles(Canvas canvas) {
        paint.setColor(getResources().getColor(com.android.boot.R.color.cst_orb_shard));
        for (Particle particle : engine.getParticles()) {
            if (!particle.active) {
                continue;
            }
            canvas.drawCircle(particle.x, particle.y, particle.size, paint);
        }
    }

    private void drawPlayer(Canvas canvas) {
        Player player = engine.getPlayer();
        if (player == null) {
            return;
        }
        if (player.isDashing()) {
            paint.setColor(getResources().getColor(com.android.boot.R.color.cst_orb_core));
            rect.set(player.x - 6f, player.y - 6f, player.x + player.width + 6f, player.y + player.height + 6f);
            canvas.drawRoundRect(rect, 18f, 18f, paint);
        }
        paint.setColor(getResources().getColor(com.android.boot.R.color.cst_player));
        rect.set(player.x, player.y, player.x + player.width, player.y + player.height);
        canvas.drawRoundRect(rect, 14f, 14f, paint);
        if (engine.hasShield()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(getResources().getColor(com.android.boot.R.color.cst_orb_rune));
            rect.set(player.x - 10f, player.y - 10f, player.x + player.width + 10f, player.y + player.height + 10f);
            canvas.drawRoundRect(rect, 18f, 18f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
