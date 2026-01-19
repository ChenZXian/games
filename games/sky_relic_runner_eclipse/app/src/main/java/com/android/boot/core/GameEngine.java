package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import com.android.boot.R;
import com.android.boot.audio.SoundManager;
import com.android.boot.entity.Collectible;
import com.android.boot.entity.Hazard;
import com.android.boot.entity.Platform;
import com.android.boot.entity.Player;
import com.android.boot.fx.FloatText;
import com.android.boot.fx.Particle;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private final Context context;
    private final SoundManager soundManager;
    private final SharedPreferences preferences;
    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint dimPaint = new Paint();
    private final RectF rectTemp = new RectF();
    private final RectF jumpButton = new RectF();
    private final RectF dashButton = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF startButton = new RectF();
    private final RectF resumeButton = new RectF();
    private final RectF restartButton = new RectF();
    private final RectF menuButton = new RectF();
    private final RectF muteButton = new RectF();

    private int width;
    private int height;
    private float density;

    private GameState state = GameState.MENU;
    private Player player;
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Collectible> collectibles = new ArrayList<>();
    private final List<FloatText> floatTexts = new ArrayList<>();
    private Particle[] particles;
    private RandomUtil random;

    private float worldSpeed;
    private float distance;
    private int score;
    private float energy;
    private float runeTimer;
    private float spawnX;
    private float groundY;
    private float magnetRadius;

    private boolean jumpHeld;
    private boolean detectedTouch;

    private int bestScore;
    private float bestDistance;

    public GameEngine(Context context, SoundManager soundManager) {
        this.context = context;
        this.soundManager = soundManager;
        preferences = context.getSharedPreferences("sky_relic_runner_eclipse", Context.MODE_PRIVATE);
        bestScore = preferences.getInt("best_score", 0);
        bestDistance = preferences.getFloat("best_distance", 0f);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        dimPaint.setARGB(190, 8, 10, 20);
    }

    public GameState getState() {
        return state;
    }

    public RectF getJumpButton() {
        return jumpButton;
    }

    public RectF getDashButton() {
        return dashButton;
    }

    public RectF getPauseButton() {
        return pauseButton;
    }

    public RectF getStartButton() {
        return startButton;
    }

    public RectF getResumeButton() {
        return resumeButton;
    }

    public RectF getRestartButton() {
        return restartButton;
    }

    public RectF getMenuButton() {
        return menuButton;
    }

    public RectF getMuteButton() {
        return muteButton;
    }

    public void setSize(int width, int height, float density) {
        this.width = width;
        this.height = height;
        this.density = density;
        float buttonSize = 86f * density;
        jumpButton.set(24f * density, height - buttonSize - 24f * density, 24f * density + buttonSize, height - 24f * density);
        dashButton.set(width - buttonSize - 24f * density, height - buttonSize - 24f * density, width - 24f * density, height - 24f * density);
        pauseButton.set(width - 76f * density, 24f * density, width - 24f * density, 76f * density);
        startButton.set(width * 0.5f - 120f * density, height * 0.55f - 40f * density, width * 0.5f + 120f * density, height * 0.55f + 40f * density);
        resumeButton.set(width * 0.5f - 140f * density, height * 0.5f - 70f * density, width * 0.5f + 140f * density, height * 0.5f);
        restartButton.set(width * 0.5f - 140f * density, height * 0.5f + 10f * density, width * 0.5f + 140f * density, height * 0.5f + 80f * density);
        menuButton.set(width * 0.5f - 140f * density, height * 0.5f + 90f * density, width * 0.5f + 140f * density, height * 0.5f + 160f * density);
        muteButton.set(width * 0.5f - 90f * density, height * 0.55f + 80f * density, width * 0.5f + 90f * density, height * 0.55f + 135f * density);
        initRun();
    }

    private void initRun() {
        random = new RandomUtil(System.nanoTime());
        float playerSize = 36f * density;
        groundY = height * 0.78f;
        player = new Player(width * 0.28f, groundY - playerSize, playerSize, playerSize);
        worldSpeed = 320f * density;
        distance = 0f;
        score = 0;
        energy = 60f;
        runeTimer = 0f;
        spawnX = width + 200f * density;
        magnetRadius = 140f * density;
        platforms.clear();
        hazards.clear();
        collectibles.clear();
        floatTexts.clear();
        buildInitialPlatforms();
        initParticles();
    }

    private void buildInitialPlatforms() {
        platforms.add(new Platform(0f, groundY, width * 1.5f, 50f * density));
        for (int i = 0; i < 5; i++) {
            float platformWidth = random.nextFloat(180f * density, 320f * density);
            float platformHeight = 36f * density;
            float y = groundY - random.nextFloat(140f * density, 260f * density);
            platforms.add(new Platform(width * 0.6f + i * 260f * density, y, platformWidth, platformHeight));
        }
    }

    private void initParticles() {
        particles = new Particle[60];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(random.nextFloat(0f, width), random.nextFloat(0f, height), random.nextFloat(10f * density, 40f * density), random.nextFloat(1f * density, 3f * density), random.nextFloat(0.2f, 0.8f));
        }
    }

    public void update(float dt) {
        if (state != GameState.PLAYING) {
            return;
        }
        worldSpeed += dt * 6f * density;
        float speed = worldSpeed;
        if (player.dashTime > 0f) {
            speed *= 1.45f;
            player.dashTime -= dt;
        }
        if (player.dashCooldown > 0f) {
            player.dashCooldown -= dt;
        }
        if (runeTimer > 0f) {
            runeTimer -= dt;
            speed *= 0.85f;
        }

        distance += speed * dt * 0.01f;

        updateParticles(dt, speed * 0.3f);

        player.velocityY += 1800f * density * dt;
        if (jumpHeld && player.jumpHoldTime < 0.2f) {
            player.velocityY -= 1200f * density * dt;
            player.jumpHoldTime += dt;
        }
        player.y += player.velocityY * dt;
        player.grounded = false;

        updatePlatforms(dt, speed);
        handlePlatformCollisions();
        updateHazards(dt, speed);
        updateCollectibles(dt, speed);
        updateFloatTexts(dt, speed);

        if (player.y > height + player.height) {
            endRun();
        }
        spawnEntities(dt);
    }

    private void updateParticles(float dt, float speed) {
        for (Particle particle : particles) {
            particle.x -= (speed + particle.speed) * dt;
            if (particle.x < -10f * density) {
                particle.x = width + random.nextFloat(0f, width * 0.3f);
                particle.y = random.nextFloat(0f, height);
            }
        }
    }

    private void updatePlatforms(float dt, float speed) {
        for (int i = platforms.size() - 1; i >= 0; i--) {
            Platform platform = platforms.get(i);
            platform.x -= speed * dt;
            if (platform.collapsing && platform.collapseTimer > 0f) {
                platform.collapseTimer -= dt;
                if (platform.collapseTimer <= 0f) {
                    platform.active = false;
                }
            }
            if (platform.x + platform.width < -200f * density) {
                platforms.remove(i);
            }
        }
    }

    private void updateHazards(float dt, float speed) {
        for (int i = hazards.size() - 1; i >= 0; i--) {
            Hazard hazard = hazards.get(i);
            hazard.x -= speed * dt;
            hazard.offset += dt;
            if (hazard.amplitude > 0f) {
                hazard.y += (float) Math.sin(hazard.offset * 3f) * hazard.amplitude * dt;
            }
            if (hazard.x + hazard.width < -200f * density) {
                hazards.remove(i);
            } else if (checkAabb(player.x, player.y, player.width, player.height, hazard.x, hazard.y, hazard.width, hazard.height)) {
                if (player.dashTime <= 0f) {
                    soundManager.playHit();
                    endRun();
                    return;
                }
            }
        }
    }

    private void updateCollectibles(float dt, float speed) {
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible collectible = collectibles.get(i);
            collectible.x -= speed * dt;
            if (collectible.x + collectible.radius < -100f * density) {
                collectibles.remove(i);
            } else if (collectible.active && shouldCollect(collectible)) {
                collectible.active = false;
                if (collectible.type == Collectible.TYPE_SHARD) {
                    int addScore = runeTimer > 0f ? 20 : 10;
                    score += addScore;
                    floatTexts.add(new FloatText(collectible.x, collectible.y, "+" + addScore));
                    soundManager.playCollect();
                } else if (collectible.type == Collectible.TYPE_CORE) {
                    energy = Math.min(100f, energy + 25f);
                    floatTexts.add(new FloatText(collectible.x, collectible.y, "+Energy"));
                    soundManager.playCollect();
                } else {
                    runeTimer = 6f;
                    floatTexts.add(new FloatText(collectible.x, collectible.y, "Rune"));
                    soundManager.playCollect();
                }
                collectibles.remove(i);
            }
        }
    }

    private boolean shouldCollect(Collectible collectible) {
        float playerCenterX = player.x + player.width * 0.5f;
        float playerCenterY = player.y + player.height * 0.5f;
        float radius = player.width * 0.55f;
        if (runeTimer > 0f) {
            radius = Math.max(radius, magnetRadius);
        }
        return checkCircle(playerCenterX, playerCenterY, radius, collectible.x, collectible.y, collectible.radius);
    }

    private void updateFloatTexts(float dt, float speed) {
        for (int i = floatTexts.size() - 1; i >= 0; i--) {
            FloatText floatText = floatTexts.get(i);
            floatText.x -= speed * dt;
            floatText.y -= 30f * density * dt;
            floatText.alpha -= dt * 0.8f;
            if (floatText.alpha <= 0f) {
                floatTexts.remove(i);
            }
        }
    }

    private void handlePlatformCollisions() {
        for (Platform platform : platforms) {
            if (!platform.active) {
                continue;
            }
            if (player.velocityY >= 0f && checkAabb(player.x, player.y, player.width, player.height, platform.x, platform.y, platform.width, platform.height)) {
                float nextY = platform.y - player.height;
                if (player.y <= nextY + 20f * density) {
                    player.y = nextY;
                    player.velocityY = 0f;
                    player.grounded = true;
                    player.jumpHoldTime = 0f;
                    if (platform.collapsing && platform.collapseTimer <= 0f) {
                        platform.collapseTimer = 0.6f;
                    }
                }
            }
        }
    }

    private void spawnEntities(float dt) {
        spawnX -= worldSpeed * dt;
        if (spawnX < width * 0.65f) {
            float gap = random.nextFloat(180f * density, 260f * density);
            spawnX = width + gap;
            float platformWidth = random.nextFloat(200f * density, 360f * density);
            float platformHeight = 36f * density;
            float y = groundY - random.nextFloat(140f * density, 320f * density);
            Platform platform = new Platform(spawnX, y, platformWidth, platformHeight);
            platform.collapsing = random.chance(0.2f + distance * 0.002f);
            platforms.add(platform);

            if (random.chance(0.45f + distance * 0.0015f)) {
                Hazard hazard = new Hazard(spawnX + platformWidth * 0.4f, y - 28f * density, 40f * density, 40f * density);
                hazard.amplitude = random.chance(0.4f) ? 12f * density : 0f;
                hazards.add(hazard);
            }
            if (random.chance(0.6f)) {
                int type = random.chance(0.1f) ? Collectible.TYPE_RUNE : (random.chance(0.35f) ? Collectible.TYPE_CORE : Collectible.TYPE_SHARD);
                collectibles.add(new Collectible(spawnX + platformWidth * 0.5f, y - 40f * density, 16f * density, type));
            }
        }
    }

    public void render(Canvas canvas) {
        if (width == 0 || height == 0) {
            return;
        }
        drawBackground(canvas);
        if (state == GameState.MENU) {
            drawMenu(canvas);
        } else {
            drawWorld(canvas);
            drawHud(canvas);
            if (state == GameState.PAUSED) {
                drawPauseOverlay(canvas);
            } else if (state == GameState.GAME_OVER) {
                drawGameOver(canvas);
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(context, R.color.cst_bg_main));
        paint.setColor(ContextCompat.getColor(context, R.color.cst_bg_far));
        rectTemp.set(0f, height * 0.1f, width, height * 0.5f);
        canvas.drawRect(rectTemp, paint);
        paint.setColor(ContextCompat.getColor(context, R.color.cst_particle));
        for (Particle particle : particles) {
            paint.setAlpha((int) (particle.alpha * 255));
            canvas.drawCircle(particle.x, particle.y, particle.size, paint);
        }
        paint.setAlpha(255);
    }

    private void drawWorld(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(context, R.color.cst_platform));
        for (Platform platform : platforms) {
            if (!platform.active) {
                continue;
            }
            rectTemp.set(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height);
            canvas.drawRoundRect(rectTemp, 12f * density, 12f * density, paint);
        }
        paint.setColor(ContextCompat.getColor(context, R.color.cst_hazard));
        for (Hazard hazard : hazards) {
            rectTemp.set(hazard.x, hazard.y, hazard.x + hazard.width, hazard.y + hazard.height);
            canvas.drawRoundRect(rectTemp, 8f * density, 8f * density, paint);
        }
        for (Collectible collectible : collectibles) {
            if (!collectible.active) {
                continue;
            }
            if (collectible.type == Collectible.TYPE_SHARD) {
                paint.setColor(ContextCompat.getColor(context, R.color.cst_accent));
            } else if (collectible.type == Collectible.TYPE_CORE) {
                paint.setColor(ContextCompat.getColor(context, R.color.cst_accent_warm));
            } else {
                paint.setColor(ContextCompat.getColor(context, R.color.cst_runner));
            }
            canvas.drawCircle(collectible.x, collectible.y, collectible.radius, paint);
        }
        paint.setColor(ContextCompat.getColor(context, R.color.cst_runner));
        rectTemp.set(player.x, player.y, player.x + player.width, player.y + player.height);
        canvas.drawRoundRect(rectTemp, 14f * density, 14f * density, paint);

        for (FloatText floatText : floatTexts) {
            textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text));
            textPaint.setTextSize(18f * density);
            textPaint.setAlpha((int) (floatText.alpha * 255));
            canvas.drawText(floatText.text, floatText.x, floatText.y, textPaint);
        }
        textPaint.setAlpha(255);
    }

    private void drawHud(Canvas canvas) {
        float panelLeft = 20f * density;
        float panelTop = 20f * density;
        float panelRight = width - 20f * density;
        float panelBottom = 90f * density;
        paint.setColor(ContextCompat.getColor(context, R.color.cst_panel));
        rectTemp.set(panelLeft, panelTop, panelRight, panelBottom);
        canvas.drawRoundRect(rectTemp, 18f * density, 18f * density, paint);

        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text));
        textPaint.setTextSize(22f * density);
        canvas.drawText(context.getString(R.string.label_distance) + " " + String.format("%.0f", distance), panelLeft + 20f * density, panelTop + 40f * density, textPaint);
        canvas.drawText(context.getString(R.string.label_score) + " " + score, panelLeft + 20f * density, panelTop + 72f * density, textPaint);

        canvas.drawText(context.getString(R.string.label_energy) + " " + (int) energy, panelRight - 230f * density, panelTop + 40f * density, textPaint);
        canvas.drawText(context.getString(R.string.label_best) + " " + bestScore, panelRight - 230f * density, panelTop + 72f * density, textPaint);

        drawButton(canvas, jumpButton, context.getString(R.string.label_jump), detectedTouch && jumpHeld);
        drawButton(canvas, dashButton, context.getString(R.string.label_dash), player.dashTime > 0f);
        drawButton(canvas, pauseButton, context.getString(R.string.label_pause), false);
    }

    private void drawButton(Canvas canvas, RectF bounds, String label, boolean pressed) {
        int baseColor = pressed ? ContextCompat.getColor(context, R.color.cst_accent) : ContextCompat.getColor(context, R.color.cst_panel_light);
        paint.setColor(baseColor);
        canvas.drawRoundRect(bounds, 20f * density, 20f * density, paint);
        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text));
        textPaint.setTextSize(18f * density);
        float textWidth = textPaint.measureText(label);
        canvas.drawText(label, bounds.centerX() - textWidth * 0.5f, bounds.centerY() + 6f * density, textPaint);
    }

    private void drawMenu(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(context, R.color.cst_panel));
        rectTemp.set(width * 0.2f, height * 0.2f, width * 0.8f, height * 0.8f);
        canvas.drawRoundRect(rectTemp, 26f * density, 26f * density, paint);
        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text));
        textPaint.setTextSize(32f * density);
        String title = context.getString(R.string.app_name);
        canvas.drawText(title, width * 0.5f - textPaint.measureText(title) * 0.5f, height * 0.32f, textPaint);
        drawButton(canvas, startButton, context.getString(R.string.btn_start), false);
        drawButton(canvas, muteButton, soundManager.isMuted() ? context.getString(R.string.btn_mute) + " On" : context.getString(R.string.btn_mute) + " Off", false);
    }

    private void drawPauseOverlay(Canvas canvas) {
        canvas.drawRect(0f, 0f, width, height, dimPaint);
        paint.setColor(ContextCompat.getColor(context, R.color.cst_panel));
        rectTemp.set(width * 0.25f, height * 0.25f, width * 0.75f, height * 0.75f);
        canvas.drawRoundRect(rectTemp, 24f * density, 24f * density, paint);
        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text));
        textPaint.setTextSize(30f * density);
        String title = context.getString(R.string.btn_resume);
        canvas.drawText(title, width * 0.5f - textPaint.measureText(title) * 0.5f, height * 0.34f, textPaint);
        drawButton(canvas, resumeButton, context.getString(R.string.btn_resume), false);
        drawButton(canvas, restartButton, context.getString(R.string.btn_restart), false);
        drawButton(canvas, menuButton, context.getString(R.string.btn_menu), false);
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawRect(0f, 0f, width, height, dimPaint);
        paint.setColor(ContextCompat.getColor(context, R.color.cst_panel));
        rectTemp.set(width * 0.2f, height * 0.2f, width * 0.8f, height * 0.8f);
        canvas.drawRoundRect(rectTemp, 24f * density, 24f * density, paint);
        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text));
        textPaint.setTextSize(32f * density);
        String label = context.getString(R.string.label_game_over);
        canvas.drawText(label, width * 0.5f - textPaint.measureText(label) * 0.5f, height * 0.32f, textPaint);
        textPaint.setTextSize(22f * density);
        String stats = context.getString(R.string.label_distance) + " " + String.format("%.0f", distance);
        canvas.drawText(stats, width * 0.5f - textPaint.measureText(stats) * 0.5f, height * 0.4f, textPaint);
        String scoreText = context.getString(R.string.label_score) + " " + score;
        canvas.drawText(scoreText, width * 0.5f - textPaint.measureText(scoreText) * 0.5f, height * 0.46f, textPaint);
        drawButton(canvas, restartButton, context.getString(R.string.btn_restart), false);
        drawButton(canvas, menuButton, context.getString(R.string.btn_menu), false);
    }

    public void handleUiTap(float x, float y) {
        if (state == GameState.MENU) {
            if (startButton.contains(x, y)) {
                state = GameState.PLAYING;
                initRun();
            } else if (muteButton.contains(x, y)) {
                soundManager.toggleMute();
            }
            return;
        }
        if (state == GameState.PLAYING) {
            if (pauseButton.contains(x, y)) {
                state = GameState.PAUSED;
            }
            return;
        }
        if (state == GameState.PAUSED) {
            if (resumeButton.contains(x, y)) {
                state = GameState.PLAYING;
            } else if (restartButton.contains(x, y)) {
                initRun();
                state = GameState.PLAYING;
            } else if (menuButton.contains(x, y)) {
                state = GameState.MENU;
            }
            return;
        }
        if (state == GameState.GAME_OVER) {
            if (restartButton.contains(x, y)) {
                initRun();
                state = GameState.PLAYING;
            } else if (menuButton.contains(x, y)) {
                state = GameState.MENU;
            }
        }
    }

    public void onJumpPressed() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (player.grounded) {
            player.velocityY = -780f * density;
            player.grounded = false;
            jumpHeld = true;
            player.jumpHoldTime = 0f;
            soundManager.playJump();
        }
    }

    public void onJumpReleased() {
        jumpHeld = false;
    }

    public void onDashPressed() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (player.dashCooldown > 0f || energy < 30f) {
            return;
        }
        energy -= 30f;
        player.dashTime = 0.25f;
        player.dashCooldown = 1.2f;
        soundManager.playDash();
    }

    private void endRun() {
        state = GameState.GAME_OVER;
        soundManager.playGameOver();
        if (score > bestScore) {
            bestScore = score;
            preferences.edit().putInt("best_score", bestScore).apply();
        }
        if (distance > bestDistance) {
            bestDistance = distance;
            preferences.edit().putFloat("best_distance", bestDistance).apply();
        }
    }

    public void setDetectedTouch(boolean detectedTouch) {
        this.detectedTouch = detectedTouch;
    }

    private boolean checkAabb(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private boolean checkCircle(float ax, float ay, float ar, float bx, float by, float br) {
        float dx = ax - bx;
        float dy = ay - by;
        float dist = dx * dx + dy * dy;
        float rad = ar + br;
        return dist <= rad * rad;
    }
}
