package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.android.boot.audio.SoundManager;
import com.android.boot.entity.Collectible;
import com.android.boot.entity.Hazard;
import com.android.boot.entity.Platform;
import com.android.boot.entity.Player;
import com.android.boot.fx.Particle;

public class GameEngine {
  private static final String PREFS = "sky_relic_runner_nova";
  private static final String KEY_BEST_SCORE = "best_score";
  private static final String KEY_BEST_DISTANCE = "best_distance";

  private final Context context;
  private final SoundManager soundManager;
  private final RandomUtil random;
  private final Player player;
  private final Platform[] platforms;
  private final Hazard[] hazards;
  private final Collectible[] collectibles;
  private final Particle[] particles;
  private final Paint bgPaint;
  private final Paint bgStarsPaint;
  private final Paint platformPaint;
  private final Paint parallaxPaint;
  private final Paint hazardPaint;
  private final Paint shardPaint;
  private final Paint corePaint;
  private final Paint runePaint;
  private final Paint playerPaint;
  private final Paint dashPaint;
  private final Paint particlePaint;
  private final RectF tempRect;
  private final int platformColor;

  private GameState state = GameState.MENU;
  private int viewWidth;
  private int viewHeight;
  private float groundY;
  private float cameraX;
  private float nextSpawnX;
  private float distance;
  private int score;
  private int bestScore;
  private float bestDistance;
  private float energy;
  private float dashCooldown;
  private float dashTimer;
  private float runeTimer;
  private boolean jumpHeld;
  private boolean jumpActive;
  private float jumpHoldTime;
  private float speedScale = 1f;
  private float baseSpeed = 220f;
  private float gravity = 1800f;
  private float jumpVelocity = 720f;
  private float jumpHoldBoost = 1200f;
  private float maxJumpHold = 0.18f;
  private float maxEnergy = 100f;
  private float energyRegen = 12f;
  private float dashCost = 30f;
  private float dashDuration = 0.22f;
  private float dashCooldownTime = 1.2f;
  private float dashSpeedBoost = 260f;

  public GameEngine(Context context, SoundManager soundManager) {
    this.context = context;
    this.soundManager = soundManager;
    random = new RandomUtil(1947L);
    player = new Player(54f, 54f);
    platforms = new Platform[64];
    hazards = new Hazard[48];
    collectibles = new Collectible[64];
    particles = new Particle[96];
    for (int i = 0; i < platforms.length; i++) {
      platforms[i] = new Platform();
    }
    for (int i = 0; i < hazards.length; i++) {
      hazards[i] = new Hazard();
    }
    for (int i = 0; i < collectibles.length; i++) {
      collectibles[i] = new Collectible();
    }
    for (int i = 0; i < particles.length; i++) {
      particles[i] = new Particle();
    }
    bgPaint = new Paint();
    bgStarsPaint = new Paint();
    platformPaint = new Paint();
    parallaxPaint = new Paint();
    hazardPaint = new Paint();
    shardPaint = new Paint();
    corePaint = new Paint();
    runePaint = new Paint();
    playerPaint = new Paint();
    dashPaint = new Paint();
    particlePaint = new Paint();
    tempRect = new RectF();
    platformColor = Color.parseColor("#1E2A5A");
    platformPaint.setColor(platformColor);
    hazardPaint.setColor(Color.parseColor("#FF6B6B"));
    shardPaint.setColor(Color.parseColor("#79B8FF"));
    corePaint.setColor(Color.parseColor("#4FE6D5"));
    runePaint.setColor(Color.parseColor("#B48BFF"));
    playerPaint.setColor(Color.parseColor("#F7F2E8"));
    dashPaint.setColor(Color.parseColor("#58D6FF"));
    particlePaint.setColor(Color.parseColor("#9DD6FF"));
    loadBest();
  }

  public void setViewport(int width, int height) {
    viewWidth = width;
    viewHeight = height;
    groundY = height * 0.78f;
    LinearGradient gradient = new LinearGradient(0, 0, 0, height, Color.parseColor("#2E3A7A"), Color.parseColor("#0B1026"), Shader.TileMode.CLAMP);
    bgPaint.setShader(gradient);
    bgStarsPaint.setColor(Color.parseColor("#9DB7FF"));
    bgStarsPaint.setAlpha(170);
    if (state == GameState.PLAYING) {
      ensurePlayerOnGround();
    }
  }

  public GameState getState() {
    return state;
  }

  public void setJumpHeld(boolean held) {
    jumpHeld = held;
  }

  public void startGame() {
    resetGame();
    state = GameState.PLAYING;
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

  public void backToMenu() {
    state = GameState.MENU;
  }

  public void restartGame() {
    resetGame();
    state = GameState.PLAYING;
  }

  public void setMuted(boolean muted) {
    soundManager.setMuted(muted);
  }

  public boolean isMuted() {
    return soundManager.isMuted();
  }

  public void requestDash() {
    if (state != GameState.PLAYING) {
      return;
    }
    if (dashCooldown <= 0f && energy >= dashCost) {
      energy -= dashCost;
      dashTimer = dashDuration;
      dashCooldown = dashCooldownTime;
      player.invulnTime = dashDuration;
      soundManager.playDash();
      spawnBurst(player.x, player.y + player.height * 0.5f);
    }
  }

  public void update(float dt) {
    if (state != GameState.PLAYING) {
      return;
    }
    dashCooldown = Math.max(0f, dashCooldown - dt);
    if (dashTimer > 0f) {
      dashTimer -= dt;
      if (dashTimer <= 0f) {
        dashTimer = 0f;
      }
    }
    if (player.invulnTime > 0f) {
      player.invulnTime = Math.max(0f, player.invulnTime - dt);
    }
    runeTimer = Math.max(0f, runeTimer - dt);
    speedScale = 1f + Math.min(0.65f, distance / 2400f);
    float runSpeed = baseSpeed * speedScale;
    if (dashTimer > 0f) {
      runSpeed += dashSpeedBoost;
    }
    player.x += runSpeed * dt;
    distance = player.x / 10f;
    energy = Math.min(maxEnergy, energy + energyRegen * dt);

    if (jumpHeld && player.onGround && !jumpActive) {
      player.velocityY = -jumpVelocity;
      player.onGround = false;
      jumpActive = true;
      jumpHoldTime = 0f;
      soundManager.playJump();
    }
    if (!jumpHeld) {
      jumpActive = false;
    }
    if (jumpActive && jumpHeld) {
      jumpHoldTime += dt;
      if (jumpHoldTime < maxJumpHold) {
        player.velocityY -= jumpHoldBoost * dt;
      }
    }

    player.velocityY += gravity * dt;
    player.y += player.velocityY * dt;

    player.onGround = false;
    for (Platform platform : platforms) {
      if (!platform.active) {
        continue;
      }
      if (platform.collapsing && platform.collapseTimer > 0f) {
        platform.collapseTimer -= dt;
        if (platform.collapseTimer <= 0f) {
          platform.active = false;
          continue;
        }
      }
      if (rectOverlap(player.x, player.y, player.width, player.height, platform.x, platform.y, platform.width, platform.height)) {
        if (player.velocityY >= 0f && player.y + player.height - platform.y < 40f) {
          player.y = platform.y - player.height;
          player.velocityY = 0f;
          player.onGround = true;
          if (platform.collapsing && platform.collapseTimer <= 0f) {
            platform.collapseTimer = 0.45f;
          }
        }
      }
    }

    for (Hazard hazard : hazards) {
      if (!hazard.active) {
        continue;
      }
      if (hazard.type == Hazard.TYPE_BLOCK) {
        hazard.movePhase += hazard.moveSpeed * dt;
        hazard.y += (float) Math.sin(hazard.movePhase) * hazard.moveRange * dt;
      }
      if (rectOverlap(player.x + 6f, player.y + 6f, player.width - 12f, player.height - 8f, hazard.x, hazard.y, hazard.width, hazard.height)) {
        if (player.invulnTime <= 0f) {
          soundManager.playHit();
          triggerGameOver();
          return;
        }
      }
    }

    for (Collectible collectible : collectibles) {
      if (!collectible.active) {
        continue;
      }
      float dx = player.x + player.width * 0.5f - collectible.x;
      float dy = player.y + player.height * 0.5f - collectible.y;
      float reach = collectible.radius + player.width * 0.4f;
      if (dx * dx + dy * dy <= reach * reach) {
        collectible.active = false;
        if (collectible.type == Collectible.TYPE_SHARD) {
          score += runeTimer > 0f ? 24 : 12;
          soundManager.playCollect();
        } else if (collectible.type == Collectible.TYPE_CORE) {
          energy = Math.min(maxEnergy, energy + 30f);
          score += 20;
          soundManager.playCollect();
        } else {
          runeTimer = 6f;
          soundManager.playRune();
        }
        spawnBurst(collectible.x, collectible.y);
      }
    }

    updateParticles(dt);
    spawnAhead();

    cameraX = player.x - viewWidth * 0.26f;

    if (player.y > viewHeight + 120f) {
      soundManager.playGameOver();
      triggerGameOver();
    }
  }

  public void drawWorld(Canvas canvas) {
    canvas.drawRect(0, 0, viewWidth, viewHeight, bgPaint);
    drawParallax(canvas, 0.15f, Color.parseColor("#1A2447"), 18, 0.42f);
    drawParallax(canvas, 0.3f, Color.parseColor("#243160"), 12, 0.55f);
    for (int i = 0; i < 64; i++) {
      float starX = (i * 97f + cameraX * 0.1f) % viewWidth;
      if (starX < 0) {
        starX += viewWidth;
      }
      float starY = (i * 43f) % (viewHeight * 0.55f);
      canvas.drawCircle(starX, starY, 1.6f, bgStarsPaint);
    }

    for (Platform platform : platforms) {
      if (!platform.active) {
        continue;
      }
      float x = platform.x - cameraX;
      tempRect.set(x, platform.y, x + platform.width, platform.y + platform.height);
      platformPaint.setAlpha(platform.collapsing && platform.collapseTimer > 0f ? 140 : 230);
      canvas.drawRoundRect(tempRect, 16f, 16f, platformPaint);
    }

    for (Hazard hazard : hazards) {
      if (!hazard.active) {
        continue;
      }
      float x = hazard.x - cameraX;
      tempRect.set(x, hazard.y, x + hazard.width, hazard.y + hazard.height);
      canvas.drawRoundRect(tempRect, 8f, 8f, hazardPaint);
    }

    for (Collectible collectible : collectibles) {
      if (!collectible.active) {
        continue;
      }
      float x = collectible.x - cameraX;
      if (collectible.type == Collectible.TYPE_SHARD) {
        canvas.drawCircle(x, collectible.y, collectible.radius, shardPaint);
      } else if (collectible.type == Collectible.TYPE_CORE) {
        canvas.drawCircle(x, collectible.y, collectible.radius, corePaint);
      } else {
        canvas.drawCircle(x, collectible.y, collectible.radius, runePaint);
      }
    }

    for (Particle particle : particles) {
      if (!particle.active) {
        continue;
      }
      float x = particle.x - cameraX;
      canvas.drawCircle(x, particle.y, particle.size, particlePaint);
    }

    float playerX = player.x - cameraX;
    tempRect.set(playerX, player.y, playerX + player.width, player.y + player.height);
    if (player.invulnTime > 0f) {
      canvas.drawRoundRect(tempRect, 18f, 18f, dashPaint);
    } else {
      canvas.drawRoundRect(tempRect, 18f, 18f, playerPaint);
    }
  }

  public int getScore() {
    return score;
  }

  public float getDistance() {
    return distance;
  }

  public float getEnergy() {
    return energy;
  }

  public float getDashCooldown() {
    return dashCooldown;
  }

  public float getRuneTimer() {
    return runeTimer;
  }

  public int getBestScore() {
    return bestScore;
  }

  public float getBestDistance() {
    return bestDistance;
  }

  private void spawnAhead() {
    float spawnLimit = cameraX + viewWidth * 2.2f;
    while (nextSpawnX < spawnLimit) {
      float gap = random.range(120f, 240f) + distance * 0.02f;
      float width = random.range(200f, 360f);
      float height = 28f;
      float yVariance = random.range(-140f, 140f);
      float y = clamp(groundY + yVariance, viewHeight * 0.42f, groundY + 20f);
      boolean collapsing = random.chance(distance > 900f ? 0.22f : 0.1f);
      addPlatform(nextSpawnX + gap, y, width, height, collapsing);
      if (random.chance(0.35f)) {
        addCollectible(nextSpawnX + gap + width * 0.5f, y - 60f, Collectible.TYPE_SHARD);
      }
      if (random.chance(0.18f)) {
        addCollectible(nextSpawnX + gap + width * 0.7f, y - 86f, Collectible.TYPE_CORE);
      }
      if (random.chance(0.05f)) {
        addCollectible(nextSpawnX + gap + width * 0.3f, y - 110f, Collectible.TYPE_RUNE);
      }
      if (random.chance(0.32f)) {
        addHazard(nextSpawnX + gap + width * 0.3f, y - 22f, Hazard.TYPE_SPIKE);
      }
      if (random.chance(distance > 1200f ? 0.2f : 0.08f)) {
        addMovingBlock(nextSpawnX + gap + width * 0.6f, y - 130f);
      }
      nextSpawnX += gap + width;
    }
  }

  private void addPlatform(float x, float y, float width, float height, boolean collapsing) {
    for (Platform platform : platforms) {
      if (!platform.active) {
        platform.active = true;
        platform.x = x;
        platform.y = y;
        platform.width = width;
        platform.height = height;
        platform.collapsing = collapsing;
        platform.collapseTimer = 0f;
        return;
      }
    }
  }

  private void addHazard(float x, float y, int type) {
    for (Hazard hazard : hazards) {
      if (!hazard.active) {
        hazard.active = true;
        hazard.x = x;
        hazard.y = y;
        hazard.width = 32f;
        hazard.height = 24f;
        hazard.type = type;
        hazard.moveRange = 0f;
        hazard.moveSpeed = 0f;
        hazard.movePhase = 0f;
        return;
      }
    }
  }

  private void addMovingBlock(float x, float y) {
    for (Hazard hazard : hazards) {
      if (!hazard.active) {
        hazard.active = true;
        hazard.x = x;
        hazard.y = y;
        hazard.width = 38f;
        hazard.height = 38f;
        hazard.type = Hazard.TYPE_BLOCK;
        hazard.moveRange = 18f + random.range(0f, 12f);
        hazard.moveSpeed = 4f + random.range(0f, 3f);
        hazard.movePhase = random.range(0f, 6.2f);
        return;
      }
    }
  }

  private void addCollectible(float x, float y, int type) {
    for (Collectible collectible : collectibles) {
      if (!collectible.active) {
        collectible.active = true;
        collectible.x = x;
        collectible.y = y;
        collectible.radius = type == Collectible.TYPE_RUNE ? 14f : 10f;
        collectible.type = type;
        return;
      }
    }
  }

  private void spawnBurst(float x, float y) {
    int spawned = 0;
    for (int i = 0; i < particles.length; i++) {
      Particle particle = particles[i];
      if (!particle.active) {
        particle.active = true;
        particle.x = x;
        particle.y = y;
        particle.velocityX = random.range(-160f, 160f);
        particle.velocityY = random.range(-220f, 80f);
        particle.life = 0.5f;
        particle.size = random.range(3f, 6f);
        spawned++;
        if (spawned >= 8) {
          return;
        }
      }
    }
  }

  private void updateParticles(float dt) {
    for (Particle particle : particles) {
      if (!particle.active) {
        continue;
      }
      particle.life -= dt;
      if (particle.life <= 0f) {
        particle.active = false;
        continue;
      }
      particle.velocityY += 480f * dt;
      particle.x += particle.velocityX * dt;
      particle.y += particle.velocityY * dt;
    }
  }

  private void resetGame() {
    for (Platform platform : platforms) {
      platform.active = false;
    }
    for (Hazard hazard : hazards) {
      hazard.active = false;
    }
    for (Collectible collectible : collectibles) {
      collectible.active = false;
    }
    for (Particle particle : particles) {
      particle.active = false;
    }
    player.x = 120f;
    player.y = groundY - player.height;
    player.velocityY = 0f;
    player.onGround = true;
    player.invulnTime = 0f;
    distance = 0f;
    score = 0;
    energy = maxEnergy * 0.7f;
    dashCooldown = 0f;
    dashTimer = 0f;
    runeTimer = 0f;
    jumpHeld = false;
    jumpActive = false;
    nextSpawnX = player.x + 200f;
    addPlatform(player.x - 80f, groundY, 480f, 28f, false);
    cameraX = player.x - viewWidth * 0.26f;
    spawnAhead();
  }

  private void triggerGameOver() {
    state = GameState.GAME_OVER;
    if (score > bestScore) {
      bestScore = score;
    }
    if (distance > bestDistance) {
      bestDistance = distance;
    }
    saveBest();
  }

  private void loadBest() {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    bestScore = prefs.getInt(KEY_BEST_SCORE, 0);
    bestDistance = prefs.getFloat(KEY_BEST_DISTANCE, 0f);
  }

  private void saveBest() {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    prefs.edit().putInt(KEY_BEST_SCORE, bestScore).putFloat(KEY_BEST_DISTANCE, bestDistance).apply();
  }

  private boolean rectOverlap(float x1, float y1, float w1, float h1, float x2, float y2, float w2, float h2) {
    return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
  }

  private void drawParallax(Canvas canvas, float speed, int color, int count, float heightRatio) {
    parallaxPaint.setColor(color);
    float layerHeight = viewHeight * heightRatio;
    for (int i = 0; i < count; i++) {
      float width = 120f + (i % 4) * 70f;
      float x = (i * 260f - cameraX * speed) % (viewWidth + 320f);
      if (x < -240f) {
        x += viewWidth + 320f;
      }
      tempRect.set(x, layerHeight - 80f, x + width, layerHeight + 40f);
      canvas.drawRoundRect(tempRect, 24f, 24f, parallaxPaint);
    }
    platformPaint.setColor(platformColor);
  }

  private float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  private void ensurePlayerOnGround() {
    player.y = groundY - player.height;
    player.velocityY = 0f;
    player.onGround = true;
  }
}
