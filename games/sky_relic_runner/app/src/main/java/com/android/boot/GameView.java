package com.android.boot;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  public enum GameState { MENU, PLAYING, PAUSED, GAME_OVER }

  public interface Listener {
    void onHudUpdate(String distance, String score, String energy, String combo);
    void onStateChange(GameState state, String finalText, String bestText, boolean controlsEnabled);
  }

  private static class Player {
    float x;
    float y;
    float vx;
    float vy;
    float width;
    float height;
    boolean onGround;
    float runTime;
    boolean invulnerable;
    float invulTime;
    float dashTime;
  }

  private static class Platform {
    float x;
    float y;
    float w;
    float h;
    boolean active;
  }

  private static class Hazard {
    float x;
    float y;
    float w;
    float h;
    int type;
    float phase;
    boolean active;
  }

  private static class Item {
    float x;
    float y;
    int type;
    boolean active;
  }

  private static class Particle {
    float x;
    float y;
    float vx;
    float vy;
    float life;
    float maxLife;
    float size;
    int color;
    boolean active;
  }

  private Listener listener;
  private Thread thread;
  private boolean running;
  private final Object stateLock = new Object();
  private GameState state = GameState.MENU;

  private final Random random = new Random();
  private final Player player = new Player();
  private final Platform[] platforms = new Platform[14];
  private final Hazard[] hazards = new Hazard[16];
  private final Item[] items = new Item[26];
  private final Particle[] particles = new Particle[90];

  private float viewWidth;
  private float viewHeight;
  private float groundY;
  private float distance;
  private int score;
  private int energy;
  private float comboTimer;
  private int comboCount;
  private float runeTimer;
  private float speed;
  private float spawnCursor;
  private float shakeTime;
  private float shakeAmp;
  private float dashCooldown;
  private boolean jumpHeld;

  private Paint paint;
  private Paint glowPaint;
  private LinearGradient bgGradient;
  private float bgShift;

  private final float[] cloudX = new float[8];
  private final float[] cloudY = new float[8];
  private final float[] cloudW = new float[8];

  private SharedPreferences prefs;

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
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    for (int i = 0; i < platforms.length; i++) {
      platforms[i] = new Platform();
    }
    for (int i = 0; i < hazards.length; i++) {
      hazards[i] = new Hazard();
    }
    for (int i = 0; i < items.length; i++) {
      items[i] = new Item();
    }
    for (int i = 0; i < particles.length; i++) {
      particles[i] = new Particle();
    }
    prefs = context.getSharedPreferences("sky_relic_runner", Context.MODE_PRIVATE);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
    notifyState();
  }

  public void startGame() {
    synchronized (stateLock) {
      resetGame();
      state = GameState.PLAYING;
      notifyState();
    }
  }

  public void resumeGame() {
    synchronized (stateLock) {
      if (state == GameState.PAUSED) {
        state = GameState.PLAYING;
        notifyState();
      }
    }
  }

  public void pauseGame() {
    synchronized (stateLock) {
      if (state == GameState.PLAYING) {
        state = GameState.PAUSED;
        notifyState();
      }
    }
  }

  public void restartGame() {
    synchronized (stateLock) {
      resetGame();
      state = GameState.PLAYING;
      notifyState();
    }
  }

  public void setJumpHeld(boolean held) {
    jumpHeld = held;
  }

  public void triggerDash() {
    synchronized (stateLock) {
      if (state != GameState.PLAYING) {
        return;
      }
      if (dashCooldown > 0f) {
        return;
      }
      if (energy < 20) {
        return;
      }
      energy -= 20;
      dashCooldown = 1.2f;
      player.dashTime = 0.32f;
      player.invulnerable = true;
      player.invulTime = 0.32f;
      emitDashBurst();
    }
  }

  public void onActivityResume() {
    resumeLoop();
  }

  public void onActivityPause() {
    pauseGame();
    stopLoop();
  }

  private void resetGame() {
    distance = 0f;
    score = 0;
    energy = 60;
    comboTimer = 0f;
    comboCount = 0;
    runeTimer = 0f;
    speed = 320f;
    spawnCursor = 0f;
    dashCooldown = 0f;
    shakeTime = 0f;
    shakeAmp = 0f;
    jumpHeld = false;
    player.width = dp(34);
    player.height = dp(54);
    player.x = viewWidth * 0.25f;
    player.y = groundY - player.height;
    player.vx = 0f;
    player.vy = 0f;
    player.onGround = true;
    player.runTime = 0f;
    player.invulnerable = false;
    player.invulTime = 0f;
    player.dashTime = 0f;
    for (Platform platform : platforms) {
      platform.active = false;
    }
    for (Hazard hazard : hazards) {
      hazard.active = false;
    }
    for (Item item : items) {
      item.active = false;
    }
    for (Particle particle : particles) {
      particle.active = false;
    }
    initPlatforms();
    initClouds();
    notifyHud();
  }

  private void initClouds() {
    for (int i = 0; i < cloudX.length; i++) {
      cloudX[i] = random.nextFloat() * viewWidth;
      cloudY[i] = dp(20) + random.nextFloat() * viewHeight * 0.4f;
      cloudW[i] = dp(90) + random.nextFloat() * dp(80);
    }
  }

  private void initPlatforms() {
    float startX = -dp(80);
    float y = groundY - dp(10);
    for (int i = 0; i < platforms.length; i++) {
      Platform p = platforms[i];
      p.active = true;
      p.w = dp(140) + random.nextFloat() * dp(80);
      p.h = dp(24);
      p.x = startX;
      p.y = y;
      startX += p.w + dp(40);
      if (i % 3 == 0) {
        y = groundY - dp(20) - random.nextFloat() * dp(80);
      }
    }
    spawnCursor = startX + dp(60);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    resumeLoop();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    viewWidth = width;
    viewHeight = height;
    groundY = height * 0.78f;
    bgGradient = new LinearGradient(0, 0, 0, height, getColor(R.color.cst_bg_top), getColor(R.color.cst_bg_bottom), Shader.TileMode.CLAMP);
    resetGame();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    stopLoop();
  }

  private void resumeLoop() {
    if (thread != null && running) {
      return;
    }
    running = true;
    thread = new Thread(this);
    thread.start();
  }

  private void stopLoop() {
    running = false;
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void run() {
    long last = System.nanoTime();
    double accumulator = 0.0;
    double step = 1.0 / 60.0;
    while (running) {
      long now = System.nanoTime();
      double delta = (now - last) / 1_000_000_000.0;
      if (delta > 0.25) {
        delta = 0.25;
      }
      last = now;
      accumulator += delta;
      while (accumulator >= step) {
        update((float) step);
        accumulator -= step;
      }
      drawFrame();
    }
  }

  private void update(float dt) {
    synchronized (stateLock) {
      if (state == GameState.PAUSED || state == GameState.GAME_OVER) {
        return;
      }
      if (state == GameState.MENU) {
        updateMenu(dt);
        return;
      }
      speed = 320f + distance * 0.02f;
      if (speed > 780f) {
        speed = 780f;
      }
      dashCooldown = Math.max(0f, dashCooldown - dt);
      if (player.dashTime > 0f) {
        player.dashTime -= dt;
      }
      if (player.invulTime > 0f) {
        player.invulTime -= dt;
      } else {
        player.invulnerable = false;
      }
      distance += speed * dt / dp(40);
      comboTimer -= dt;
      if (comboTimer <= 0f) {
        comboCount = 0;
      }
      if (runeTimer > 0f) {
        runeTimer -= dt;
      }
      updatePlayer(dt);
      updatePlatforms(dt);
      updateHazards(dt);
      updateItems(dt);
      updateParticles(dt);
      updateClouds(dt);
      checkSpawns();
      notifyHud();
      if (player.y > viewHeight + dp(120)) {
        triggerGameOver();
      }
    }
  }

  private void updateMenu(float dt) {
    updateClouds(dt);
    bgShift += dt * 10f;
    if (bgShift > viewWidth) {
      bgShift = 0f;
    }
  }

  private void updatePlayer(float dt) {
    float gravity = dp(1400);
    float jumpBoost = dp(2400);
    if (jumpHeld && player.onGround) {
      player.vy = -dp(640);
      player.onGround = false;
    }
    if (jumpHeld && !player.onGround && player.vy < 0f) {
      player.vy -= jumpBoost * dt;
    }
    player.vy += gravity * dt;
    player.y += player.vy * dt;
    player.runTime += dt * 10f;
    player.x = viewWidth * 0.25f;
    if (player.dashTime > 0f) {
      emitDashTrail(dt);
    }
    resolvePlatformCollision();
  }

  private void resolvePlatformCollision() {
    player.onGround = false;
    for (Platform p : platforms) {
      if (!p.active) {
        continue;
      }
      if (player.x + player.width * 0.5f > p.x && player.x - player.width * 0.5f < p.x + p.w) {
        float foot = player.y + player.height;
        if (foot >= p.y && foot <= p.y + p.h + dp(8) && player.vy >= 0f) {
          player.y = p.y - player.height;
          player.vy = 0f;
          player.onGround = true;
        }
      }
    }
  }

  private void updatePlatforms(float dt) {
    float move = speed * dt;
    for (Platform p : platforms) {
      if (!p.active) {
        continue;
      }
      p.x -= move;
      if (p.x + p.w < -dp(120)) {
        p.active = false;
      }
    }
  }

  private void updateHazards(float dt) {
    float move = speed * dt;
    for (Hazard h : hazards) {
      if (!h.active) {
        continue;
      }
      h.x -= move;
      h.phase += dt;
      if (h.type == 2) {
        h.y += (float) Math.sin(h.phase * 3.2f) * dt * dp(30);
      }
      if (h.x + h.w < -dp(140)) {
        h.active = false;
      } else if (!player.invulnerable && intersect(player.x, player.y, player.width, player.height, h.x, h.y, h.w, h.h)) {
        shake(0.16f, dp(12));
        triggerGameOver();
      } else if (player.invulnerable && intersect(player.x, player.y, player.width, player.height, h.x, h.y, h.w, h.h)) {
        shake(0.08f, dp(6));
      }
    }
  }

  private void updateItems(float dt) {
    float move = speed * dt;
    for (Item item : items) {
      if (!item.active) {
        continue;
      }
      item.x -= move;
      if (item.x < -dp(120)) {
        item.active = false;
      } else if (intersect(player.x, player.y, player.width, player.height, item.x - dp(10), item.y - dp(10), dp(20), dp(20))) {
        onCollect(item);
      }
    }
  }

  private void onCollect(Item item) {
    item.active = false;
    comboTimer = 1.5f;
    comboCount += 1;
    int base = 6 + comboCount;
    if (item.type == 0) {
      score += base * (runeTimer > 0f ? 2 : 1);
      energy = Math.min(100, energy + 4);
      emitPickup(item.x, item.y, getColor(R.color.cst_shard));
    } else if (item.type == 1) {
      energy = Math.min(100, energy + 18);
      score += base;
      emitPickup(item.x, item.y, getColor(R.color.cst_energy));
    } else {
      runeTimer = 5f;
      score += base * 3;
      emitPickup(item.x, item.y, getColor(R.color.cst_rune));
    }
    notifyHud();
  }

  private void updateParticles(float dt) {
    for (Particle p : particles) {
      if (!p.active) {
        continue;
      }
      p.life -= dt;
      if (p.life <= 0f) {
        p.active = false;
        continue;
      }
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vy += dp(160) * dt;
    }
  }

  private void updateClouds(float dt) {
    float move = speed * dt * 0.18f;
    for (int i = 0; i < cloudX.length; i++) {
      cloudX[i] -= move;
      if (cloudX[i] + cloudW[i] < -dp(60)) {
        cloudX[i] = viewWidth + random.nextFloat() * dp(120);
        cloudY[i] = dp(20) + random.nextFloat() * viewHeight * 0.4f;
      }
    }
  }

  private void checkSpawns() {
    while (spawnCursor < viewWidth + dp(300)) {
      float segment = dp(120) + random.nextFloat() * dp(140);
      float platformWidth = dp(140) + random.nextFloat() * dp(140);
      float platformHeight = groundY - dp(20) - random.nextFloat() * dp(130);
      Platform p = getPlatform();
      if (p != null) {
        p.active = true;
        p.x = spawnCursor;
        p.w = platformWidth;
        p.h = dp(24);
        p.y = Math.min(groundY - dp(20), platformHeight);
      }
      if (random.nextFloat() < hazardChance()) {
        spawnHazard(spawnCursor + platformWidth * 0.45f, p != null ? p.y - dp(12) : groundY - dp(16));
      }
      if (random.nextFloat() < 0.7f) {
        spawnItem(spawnCursor + platformWidth * 0.6f, (p != null ? p.y : groundY) - dp(36));
      }
      spawnCursor += platformWidth + segment;
    }
  }

  private float hazardChance() {
    float c = 0.3f + distance * 0.0009f;
    if (c > 0.85f) {
      c = 0.85f;
    }
    return c;
  }

  private void spawnHazard(float x, float y) {
    Hazard h = getHazard();
    if (h == null) {
      return;
    }
    h.active = true;
    h.x = x;
    h.w = dp(28) + random.nextFloat() * dp(18);
    h.h = dp(28) + random.nextFloat() * dp(10);
    h.type = distance > 900 ? random.nextInt(3) : random.nextInt(2);
    h.phase = random.nextFloat() * 3f;
    h.y = y - h.h;
  }

  private void spawnItem(float x, float y) {
    Item item = getItem();
    if (item == null) {
      return;
    }
    item.active = true;
    item.x = x;
    item.y = y - random.nextFloat() * dp(30);
    float roll = random.nextFloat();
    if (roll < 0.7f) {
      item.type = 0;
    } else if (roll < 0.9f) {
      item.type = 1;
    } else {
      item.type = 2;
    }
  }

  private void triggerGameOver() {
    if (state == GameState.GAME_OVER) {
      return;
    }
    state = GameState.GAME_OVER;
    int best = prefs.getInt("best_score", 0);
    if (score > best) {
      best = score;
      prefs.edit().putInt("best_score", best).apply();
    }
    if (listener != null) {
      String finalText = "Score " + score + "  Distance " + String.format("%.1f", distance);
      String bestText = "Best " + best;
      listener.onStateChange(state, finalText, bestText, false);
    }
  }

  private void notifyState() {
    if (listener == null) {
      return;
    }
    int best = prefs.getInt("best_score", 0);
    String finalText = state == GameState.GAME_OVER ? "Score " + score + "  Distance " + String.format("%.1f", distance) : "";
    String bestText = state == GameState.GAME_OVER ? "Best " + best : "";
    boolean controls = state == GameState.PLAYING;
    listener.onStateChange(state, finalText, bestText, controls);
  }

  private void notifyHud() {
    if (listener == null) {
      return;
    }
    String d = "Distance " + String.format("%.1f", distance);
    String s = "Score " + score;
    String e = "Energy " + energy;
    String c = comboCount > 1 ? "Combo x" + comboCount : "Combo";
    listener.onHudUpdate(d, s, e, c);
  }

  private Platform getPlatform() {
    for (Platform p : platforms) {
      if (!p.active) {
        return p;
      }
    }
    return null;
  }

  private Hazard getHazard() {
    for (Hazard h : hazards) {
      if (!h.active) {
        return h;
      }
    }
    return null;
  }

  private Item getItem() {
    for (Item item : items) {
      if (!item.active) {
        return item;
      }
    }
    return null;
  }

  private Particle getParticle() {
    for (Particle p : particles) {
      if (!p.active) {
        return p;
      }
    }
    return null;
  }

  private void emitPickup(float x, float y, int color) {
    for (int i = 0; i < 12; i++) {
      Particle p = getParticle();
      if (p == null) {
        return;
      }
      p.active = true;
      p.x = x;
      p.y = y;
      p.vx = (random.nextFloat() - 0.5f) * dp(220);
      p.vy = -random.nextFloat() * dp(200);
      p.life = 0.5f + random.nextFloat() * 0.4f;
      p.maxLife = p.life;
      p.size = dp(4) + random.nextFloat() * dp(4);
      p.color = color;
    }
  }

  private void emitDashBurst() {
    for (int i = 0; i < 20; i++) {
      Particle p = getParticle();
      if (p == null) {
        return;
      }
      p.active = true;
      p.x = player.x - dp(6);
      p.y = player.y + player.height * 0.5f;
      p.vx = -dp(200) - random.nextFloat() * dp(160);
      p.vy = (random.nextFloat() - 0.5f) * dp(140);
      p.life = 0.3f + random.nextFloat() * 0.3f;
      p.maxLife = p.life;
      p.size = dp(6) + random.nextFloat() * dp(6);
      p.color = getColor(R.color.cst_text_accent);
    }
  }

  private void emitDashTrail(float dt) {
    if (random.nextFloat() > 0.3f) {
      return;
    }
    Particle p = getParticle();
    if (p == null) {
      return;
    }
    p.active = true;
    p.x = player.x - dp(4);
    p.y = player.y + player.height * 0.6f;
    p.vx = -dp(140) - random.nextFloat() * dp(80);
    p.vy = (random.nextFloat() - 0.5f) * dp(80);
    p.life = 0.25f + random.nextFloat() * 0.2f;
    p.maxLife = p.life;
    p.size = dp(4) + random.nextFloat() * dp(4);
    p.color = getColor(R.color.cst_text_accent);
  }

  private void shake(float time, float amp) {
    shakeTime = Math.max(shakeTime, time);
    shakeAmp = Math.max(shakeAmp, amp);
  }

  private void drawFrame() {
    SurfaceHolder holder = getHolder();
    Canvas canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
    float shakeX = 0f;
    float shakeY = 0f;
    if (shakeTime > 0f) {
      shakeTime -= 1f / 60f;
      shakeX = (random.nextFloat() - 0.5f) * shakeAmp;
      shakeY = (random.nextFloat() - 0.5f) * shakeAmp;
    }
    canvas.save();
    canvas.translate(shakeX, shakeY);
    drawBackground(canvas);
    drawPlatforms(canvas);
    drawHazards(canvas);
    drawItems(canvas);
    drawPlayer(canvas);
    drawParticles(canvas);
    canvas.restore();
    holder.unlockCanvasAndPost(canvas);
  }

  private void drawBackground(Canvas canvas) {
    paint.setShader(bgGradient);
    canvas.drawRect(0, 0, viewWidth, viewHeight, paint);
    paint.setShader(null);
    paint.setColor(getColor(R.color.cst_bg_bottom));
    canvas.drawRect(0, groundY, viewWidth, viewHeight, paint);
    paint.setColor(Color.WHITE);
    for (int i = 0; i < cloudX.length; i++) {
      float alpha = 120 + i * 8;
      paint.setAlpha((int) alpha);
      canvas.drawRoundRect(new RectF(cloudX[i], cloudY[i], cloudX[i] + cloudW[i], cloudY[i] + dp(20)), dp(10), dp(10), paint);
    }
    paint.setAlpha(255);
  }

  private void drawPlatforms(Canvas canvas) {
    for (Platform p : platforms) {
      if (!p.active) {
        continue;
      }
      paint.setColor(getColor(R.color.cst_panel));
      RectF rect = new RectF(p.x, p.y, p.x + p.w, p.y + p.h);
      canvas.drawRoundRect(rect, dp(6), dp(6), paint);
      paint.setColor(getColor(R.color.cst_panel_edge));
      canvas.drawRoundRect(rect, dp(6), dp(6), paint);
      paint.setColor(getColor(R.color.cst_text_secondary));
      canvas.drawRect(p.x + dp(6), p.y + dp(4), p.x + p.w - dp(6), p.y + dp(8), paint);
    }
  }

  private void drawHazards(Canvas canvas) {
    for (Hazard h : hazards) {
      if (!h.active) {
        continue;
      }
      paint.setColor(getColor(R.color.cst_hazard));
      if (h.type == 0) {
        Path path = new Path();
        path.moveTo(h.x, h.y + h.h);
        path.lineTo(h.x + h.w * 0.5f, h.y);
        path.lineTo(h.x + h.w, h.y + h.h);
        path.close();
        canvas.drawPath(path, paint);
      } else if (h.type == 1) {
        canvas.drawRoundRect(new RectF(h.x, h.y, h.x + h.w, h.y + h.h), dp(4), dp(4), paint);
      } else {
        paint.setColor(getColor(R.color.cst_panel_edge));
        canvas.drawRect(h.x, h.y, h.x + h.w, h.y + h.h, paint);
        paint.setColor(getColor(R.color.cst_hazard));
        canvas.drawRect(h.x + dp(4), h.y + dp(4), h.x + h.w - dp(4), h.y + h.h - dp(4), paint);
      }
    }
  }

  private void drawItems(Canvas canvas) {
    for (Item item : items) {
      if (!item.active) {
        continue;
      }
      if (item.type == 0) {
        paint.setColor(getColor(R.color.cst_shard));
      } else if (item.type == 1) {
        paint.setColor(getColor(R.color.cst_energy));
      } else {
        paint.setColor(getColor(R.color.cst_rune));
      }
      canvas.drawCircle(item.x, item.y, dp(8), paint);
      glowPaint.setColor(paint.getColor());
      glowPaint.setAlpha(90);
      canvas.drawCircle(item.x, item.y, dp(14), glowPaint);
    }
  }

  private void drawPlayer(Canvas canvas) {
    float frame = player.runTime % 1f;
    boolean alt = frame > 0.5f;
    float legOffset = alt ? dp(4) : -dp(4);
    paint.setColor(Color.WHITE);
    RectF body = new RectF(player.x - player.width * 0.5f, player.y, player.x + player.width * 0.5f, player.y + player.height);
    paint.setColor(getColor(R.color.cst_text_primary));
    canvas.drawRoundRect(body, dp(10), dp(10), paint);
    paint.setColor(getColor(R.color.cst_text_accent));
    canvas.drawCircle(player.x, player.y - dp(6), dp(10), paint);
    paint.setColor(getColor(R.color.cst_text_secondary));
    canvas.drawRect(player.x - dp(8), player.y + player.height - dp(10) + legOffset, player.x - dp(2), player.y + player.height + legOffset, paint);
    canvas.drawRect(player.x + dp(2), player.y + player.height - dp(10) - legOffset, player.x + dp(8), player.y + player.height - legOffset, paint);
    if (player.invulnerable) {
      glowPaint.setColor(getColor(R.color.cst_text_accent));
      glowPaint.setAlpha(140);
      canvas.drawRoundRect(body, dp(12), dp(12), glowPaint);
    }
  }

  private void drawParticles(Canvas canvas) {
    for (Particle p : particles) {
      if (!p.active) {
        continue;
      }
      float alpha = 255f * (p.life / p.maxLife);
      paint.setColor(p.color);
      paint.setAlpha((int) alpha);
      canvas.drawCircle(p.x, p.y, p.size, paint);
    }
    paint.setAlpha(255);
  }

  private boolean intersect(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
    return ax + aw > bx && ax < bx + bw && ay + ah > by && ay < by + bh;
  }

  private float dp(float value) {
    return value * getResources().getDisplayMetrics().density;
  }

  private int getColor(int resId) {
    return getResources().getColor(resId);
  }
}
