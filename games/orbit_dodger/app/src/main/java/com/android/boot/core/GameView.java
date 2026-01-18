package com.android.boot.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.R;
import com.android.boot.fx.FloatingText;
import com.android.boot.fx.Particle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    public interface GameListener {
        void onScoreChanged(int score);
        void onLivesChanged(int lives);
        void onEnergyChanged(int energy);
        void onGameOver(int finalScore);
        void onMenuState();
    }

    private final Paint paint = new Paint();
    private final Random random = new Random();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private Thread gameThread;
    private boolean running;
    private GameState state = GameState.MENU;
    private GameListener listener;
    private float playerX;
    private float playerY;
    private float targetX;
    private float targetY;
    private float playerRadius;
    private int score;
    private int lives;
    private float energy;
    private float obstacleTimer;
    private float starTimer;
    private float shakeTime;
    private float shakeIntensity;
    private int width;
    private int height;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onMenuState();
        }
    }

    public boolean isPlaying() {
        return state == GameState.PLAYING;
    }

    public void startGame() {
        state = GameState.PLAYING;
        score = 0;
        lives = 3;
        energy = 100f;
        obstacles.clear();
        stars.clear();
        particles.clear();
        texts.clear();
        obstacleTimer = 0f;
        starTimer = 0f;
        shakeTime = 0f;
        shakeIntensity = 0f;
        if (width > 0 && height > 0) {
            playerRadius = Math.min(width, height) * 0.05f;
            playerX = width * 0.5f;
            playerY = height * 0.7f;
            targetX = playerX;
            targetY = playerY;
        }
        notifyStats();
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.width = width;
        this.height = height;
        playerRadius = Math.min(width, height) * 0.05f;
        playerX = width * 0.5f;
        playerY = height * 0.7f;
        targetX = playerX;
        targetY = playerY;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            update(dt);
            drawFrame();
        }
    }

    private void update(float dt) {
        if (state != GameState.PLAYING) {
            updateEffects(dt);
            return;
        }

        score += Math.max(0, (int) (dt * 10));
        obstacleTimer += dt;
        starTimer += dt;

        float speed = Math.min(width, height) * 1.2f;
        float dx = targetX - playerX;
        float dy = targetY - playerY;
        playerX += dx * Math.min(1f, dt * 6f);
        playerY += dy * Math.min(1f, dt * 6f);
        playerX = clamp(playerX, playerRadius, width - playerRadius);
        playerY = clamp(playerY, playerRadius, height - playerRadius);

        if (obstacleTimer > 0.7f) {
            obstacleTimer = 0f;
            obstacles.add(createObstacle());
        }

        if (starTimer > 1.3f) {
            starTimer = 0f;
            stars.add(createStar());
        }

        Iterator<Obstacle> obstacleIterator = obstacles.iterator();
        while (obstacleIterator.hasNext()) {
            Obstacle obstacle = obstacleIterator.next();
            obstacle.y += obstacle.speed * dt;
            if (obstacle.y - obstacle.radius > height) {
                obstacleIterator.remove();
            } else if (distance(playerX, playerY, obstacle.x, obstacle.y) < playerRadius + obstacle.radius) {
                obstacleIterator.remove();
                handleHit(obstacle.x, obstacle.y);
            }
        }

        Iterator<Star> starIterator = stars.iterator();
        while (starIterator.hasNext()) {
            Star star = starIterator.next();
            star.y += star.speed * dt;
            if (star.y - star.radius > height) {
                starIterator.remove();
            } else if (distance(playerX, playerY, star.x, star.y) < playerRadius + star.radius) {
                starIterator.remove();
                handleCollect(star.x, star.y);
            }
        }

        updateEffects(dt);
        notifyStats();
    }

    private void updateEffects(float dt) {
        if (shakeTime > 0f) {
            shakeTime -= dt;
        }

        Iterator<Particle> particleIterator = particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            particle.update(dt);
            if (particle.isDead()) {
                particleIterator.remove();
            }
        }

        Iterator<FloatingText> textIterator = texts.iterator();
        while (textIterator.hasNext()) {
            FloatingText text = textIterator.next();
            text.update(dt);
            if (text.isDead()) {
                textIterator.remove();
            }
        }
    }

    private void drawFrame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        canvas.drawColor(getResources().getColor(R.color.cst_bg_main));

        float shakeX = 0f;
        float shakeY = 0f;
        if (shakeTime > 0f) {
            shakeX = (random.nextFloat() - 0.5f) * shakeIntensity;
            shakeY = (random.nextFloat() - 0.5f) * shakeIntensity;
        }

        canvas.save();
        canvas.translate(shakeX, shakeY);

        paint.setColor(getResources().getColor(R.color.cst_star));
        for (Star star : stars) {
            canvas.drawCircle(star.x, star.y, star.radius, paint);
        }

        paint.setColor(getResources().getColor(R.color.cst_obstacle));
        for (Obstacle obstacle : obstacles) {
            canvas.drawCircle(obstacle.x, obstacle.y, obstacle.radius, paint);
        }

        paint.setColor(getResources().getColor(R.color.cst_player));
        canvas.drawCircle(playerX, playerY, playerRadius, paint);

        for (Particle particle : particles) {
            particle.draw(canvas);
        }

        for (FloatingText text : texts) {
            text.draw(canvas, paint);
        }

        canvas.restore();

        getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != GameState.PLAYING) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            targetX = event.getX();
            targetY = event.getY();
        }
        return true;
    }

    private void handleCollect(float x, float y) {
        score += 50;
        energy = Math.min(100f, energy + 10f);
        texts.add(new FloatingText(x, y, "+50", getResources().getColor(R.color.cst_star)));
        spawnParticles(x, y, getResources().getColor(R.color.cst_particle));
    }

    private void handleHit(float x, float y) {
        energy -= 25f;
        shakeTime = 0.3f;
        shakeIntensity = Math.min(width, height) * 0.04f;
        texts.add(new FloatingText(x, y, "-25", getResources().getColor(R.color.cst_obstacle)));
        spawnParticles(x, y, getResources().getColor(R.color.cst_obstacle));
        if (energy <= 0f) {
            lives -= 1;
            energy = 100f;
            texts.add(new FloatingText(width * 0.5f, height * 0.4f, "Shield Down", getResources().getColor(R.color.cst_text_primary)));
            if (lives <= 0) {
                state = GameState.GAME_OVER;
                if (listener != null) {
                    listener.onGameOver(score);
                }
            }
        }
    }

    private void spawnParticles(float x, float y, int color) {
        for (int i = 0; i < 14; i++) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float speed = (0.4f + random.nextFloat()) * Math.min(width, height) * 0.6f;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            particles.add(new Particle(x, y, vx, vy, color));
        }
    }

    private void notifyStats() {
        if (listener != null) {
            listener.onScoreChanged(score);
            listener.onLivesChanged(lives);
            listener.onEnergyChanged((int) energy);
        }
    }

    private Obstacle createObstacle() {
        float radius = Math.min(width, height) * (0.035f + random.nextFloat() * 0.02f);
        float x = radius + random.nextFloat() * (width - radius * 2f);
        float y = -radius;
        float speed = Math.min(width, height) * (0.4f + random.nextFloat() * 0.6f);
        return new Obstacle(x, y, radius, speed);
    }

    private Star createStar() {
        float radius = Math.min(width, height) * 0.025f;
        float x = radius + random.nextFloat() * (width - radius * 2f);
        float y = -radius;
        float speed = Math.min(width, height) * 0.35f;
        return new Star(x, y, radius, speed);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static class Obstacle {
        private final float x;
        private float y;
        private final float radius;
        private final float speed;

        private Obstacle(float x, float y, float radius, float speed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
        }
    }

    private static class Star {
        private final float x;
        private float y;
        private final float radius;
        private final float speed;

        private Star(float x, float y, float radius, float speed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
        }
    }
}
