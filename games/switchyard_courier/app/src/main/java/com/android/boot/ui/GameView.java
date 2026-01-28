package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.R;
import com.android.boot.core.GameState;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  public interface GameListener {
    void onHudUpdate(int score, int combo, int timeLeft, String parcel, boolean brakeReady, GameState state);
    void onStateChanged(GameState state, int score);
  }

  private static final int TYPE_EMPTY = 0;
  private static final int TYPE_STRAIGHT = 1;
  private static final int TYPE_CURVE = 2;
  private static final int TYPE_TJUNCTION = 3;
  private static final int TYPE_CROSS = 4;
  private static final int TYPE_DEAD_END = 5;
  private static final int TYPE_BARRIER = 6;
  private static final int TYPE_DEPOT = 7;
  private static final int TYPE_SPAWN = 8;

  private static final int DIR_UP = 0;
  private static final int DIR_RIGHT = 1;
  private static final int DIR_DOWN = 2;
  private static final int DIR_LEFT = 3;

  private static final float STEP = 1f / 60f;

  private final SurfaceHolder surfaceHolder;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private GameListener listener;
  private Thread thread;
  private boolean running;
  private GameState state = GameState.MENU;

  private int cols = 12;
  private int rows = 10;
  private int[][] type;
  private int[][] orientation;
  private int[][] altOrientation;
  private int[][] switchState;
  private int[][] depotColor;
  private int[][] spawnColor;
  private float[][] blockedTimer;

  private float cellSize;
  private float originX;
  private float originY;

  private float cartX;
  private float cartY;
  private int cartDir;
  private float speed;
  private float speedBoost;
  private float timeLeft;
  private int score;
  private int combo;
  private int parcelColor;
  private float brakeTimer;
  private float brakeCooldown;

  private long lastTime;
  private float accumulator;
  private float hudTimer;

  private int colorRail;
  private int colorRailDark;
  private int colorCartGlow;
  private int colorText;
  private int colorBlocked;
  private int colorRed;
  private int colorBlue;
  private int colorGreen;
  private int colorYellow;

  private float railStroke;
  private float railStrokeBold;
  private float cartRadius;
  private float glowRadius;

  private final ParticleSystem particles = new ParticleSystem();

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    surfaceHolder = getHolder();
    surfaceHolder.addCallback(this);
    setFocusable(true);
    loadResources();
    initGrid();
    resetGame();
  }

  public void setListener(GameListener listener) {
    this.listener = listener;
  }

  private void loadResources() {
    colorRail = getResources().getColor(R.color.cst_rail);
    colorRailDark = getResources().getColor(R.color.cst_rail_dark);
    colorCartGlow = getResources().getColor(R.color.cst_cart_glow);
    colorText = getResources().getColor(R.color.cst_text_primary);
    colorBlocked = getResources().getColor(R.color.cst_blocked);
    colorRed = getResources().getColor(R.color.cst_parcel_red);
    colorBlue = getResources().getColor(R.color.cst_parcel_blue);
    colorGreen = getResources().getColor(R.color.cst_parcel_green);
    colorYellow = getResources().getColor(R.color.cst_parcel_yellow);

    railStroke = getResources().getDimension(R.dimen.cst_stroke_s);
    railStrokeBold = getResources().getDimension(R.dimen.cst_stroke_m);
    cartRadius = getResources().getDimension(R.dimen.cst_pad_10);
    glowRadius = getResources().getDimension(R.dimen.cst_pad_16);
  }

  private void initGrid() {
    type = new int[rows][cols];
    orientation = new int[rows][cols];
    altOrientation = new int[rows][cols];
    switchState = new int[rows][cols];
    depotColor = new int[rows][cols];
    spawnColor = new int[rows][cols];
    blockedTimer = new float[rows][cols];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        type[r][c] = TYPE_EMPTY;
        orientation[r][c] = 0;
        altOrientation[r][c] = -1;
        switchState[r][c] = 0;
        depotColor[r][c] = -1;
        spawnColor[r][c] = -1;
        blockedTimer[r][c] = 0f;
      }
    }
    buildTrack();
  }

  private void buildTrack() {
    for (int c = 1; c < cols - 1; c++) {
      type[2][c] = TYPE_STRAIGHT;
      orientation[2][c] = 1;
      type[7][c] = TYPE_STRAIGHT;
      orientation[7][c] = 1;
    }
    for (int r = 2; r < 7; r++) {
      type[r][1] = TYPE_STRAIGHT;
      orientation[r][1] = 0;
      type[r][cols - 2] = TYPE_STRAIGHT;
      orientation[r][cols - 2] = 0;
    }
    type[2][1] = TYPE_CURVE;
    orientation[2][1] = 2;
    altOrientation[2][1] = 3;
    type[2][cols - 2] = TYPE_CURVE;
    orientation[2][cols - 2] = 1;
    altOrientation[2][cols - 2] = 0;
    type[7][1] = TYPE_CURVE;
    orientation[7][1] = 3;
    altOrientation[7][1] = 2;
    type[7][cols - 2] = TYPE_CURVE;
    orientation[7][cols - 2] = 0;
    altOrientation[7][cols - 2] = 1;

    type[4][5] = TYPE_TJUNCTION;
    orientation[4][5] = DIR_UP;
    type[5][5] = TYPE_STRAIGHT;
    orientation[5][5] = 0;
    type[6][5] = TYPE_CURVE;
    orientation[6][5] = 0;
    altOrientation[6][5] = 1;
    type[6][6] = TYPE_STRAIGHT;
    orientation[6][6] = 1;
    type[6][7] = TYPE_CURVE;
    orientation[6][7] = 1;
    altOrientation[6][7] = 2;
    type[5][7] = TYPE_STRAIGHT;
    orientation[5][7] = 0;
    type[4][7] = TYPE_TJUNCTION;
    orientation[4][7] = DIR_UP;

    type[4][3] = TYPE_TJUNCTION;
    orientation[4][3] = DIR_UP;
    type[5][3] = TYPE_STRAIGHT;
    orientation[5][3] = 0;
    type[6][3] = TYPE_CURVE;
    orientation[6][3] = 3;
    altOrientation[6][3] = 2;
    type[6][2] = TYPE_STRAIGHT;
    orientation[6][2] = 1;

    type[4][1] = TYPE_TJUNCTION;
    orientation[4][1] = DIR_LEFT;
    type[4][cols - 2] = TYPE_TJUNCTION;
    orientation[4][cols - 2] = DIR_RIGHT;

    type[2][5] = TYPE_DEPOT;
    orientation[2][5] = 1;
    depotColor[2][5] = 0;
    type[2][7] = TYPE_DEPOT;
    orientation[2][7] = 1;
    depotColor[2][7] = 1;
    type[7][4] = TYPE_DEPOT;
    orientation[7][4] = 1;
    depotColor[7][4] = 2;
    type[7][8] = TYPE_DEPOT;
    orientation[7][8] = 1;
    depotColor[7][8] = 3;

    type[5][1] = TYPE_SPAWN;
    orientation[5][1] = 0;
    spawnColor[5][1] = 0;
    type[5][cols - 2] = TYPE_SPAWN;
    orientation[5][cols - 2] = 0;
    spawnColor[5][cols - 2] = 1;
    type[2][9] = TYPE_SPAWN;
    orientation[2][9] = 1;
    spawnColor[2][9] = 2;
    type[7][2] = TYPE_SPAWN;
    orientation[7][2] = 1;
    spawnColor[7][2] = 3;

    type[4][6] = TYPE_CROSS;
    type[5][6] = TYPE_CROSS;

    type[3][9] = TYPE_DEAD_END;
    orientation[3][9] = DIR_DOWN;
    type[6][9] = TYPE_DEAD_END;
    orientation[6][9] = DIR_UP;

    type[3][2] = TYPE_BARRIER;
    type[6][9] = TYPE_DEAD_END;
  }

  public void startGame() {
    resetGame();
    changeState(GameState.PLAYING);
  }

  public void pauseGame() {
    if (state == GameState.PLAYING) {
      changeState(GameState.PAUSED);
    }
  }

  public void resumeGame() {
    if (state == GameState.PAUSED) {
      changeState(GameState.PLAYING);
    }
  }

  public void goToMenu() {
    changeState(GameState.MENU);
  }

  public void restartGame() {
    startGame();
  }

  public void triggerBrake() {
    if (brakeCooldown <= 0f) {
      brakeTimer = 2f;
      brakeCooldown = 6f;
    }
  }

  private void changeState(GameState newState) {
    state = newState;
    if (listener != null) {
      mainHandler.post(() -> listener.onStateChanged(state, score));
    }
  }

  private void resetGame() {
    cartX = 1.5f;
    cartY = 2.5f;
    cartDir = DIR_RIGHT;
    speed = 2.2f;
    speedBoost = 0f;
    timeLeft = 60f;
    score = 0;
    combo = 1;
    parcelColor = -1;
    brakeTimer = 0f;
    brakeCooldown = 0f;
    hudTimer = 0f;
    particles.clear();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    running = true;
    thread = new Thread(this);
    thread.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    updateDimensions(width, height);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    running = false;
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void updateDimensions(int width, int height) {
    float cellW = width / (float) cols;
    float cellH = height / (float) rows;
    cellSize = Math.min(cellW, cellH);
    originX = (width - cellSize * cols) * 0.5f;
    originY = (height - cellSize * rows) * 0.5f;
  }

  @Override
  public void run() {
    lastTime = System.nanoTime();
    while (running) {
      long now = System.nanoTime();
      float frameTime = (now - lastTime) / 1000000000f;
      if (frameTime > 0.1f) {
        frameTime = 0.1f;
      }
      lastTime = now;
      accumulator += frameTime;
      while (accumulator >= STEP) {
        update(STEP);
        accumulator -= STEP;
      }
      drawFrame();
    }
  }

  private void update(float dt) {
    if (state != GameState.PLAYING) {
      if (state == GameState.MENU) {
        hudTimer = 0f;
      }
      return;
    }
    speedBoost += dt * 0.03f;
    float currentSpeed = speed + speedBoost;
    if (brakeTimer > 0f) {
      brakeTimer -= dt;
      currentSpeed *= 0.5f;
    }
    if (brakeCooldown > 0f) {
      brakeCooldown -= dt;
    }

    timeLeft -= dt;
    if (timeLeft <= 0f && parcelColor >= 0) {
      gameOver();
      return;
    }
    if (timeLeft < 0f) {
      timeLeft = 0f;
    }

    moveCart(currentSpeed * dt);
    updateBlocked(dt);
    particles.update(dt);

    hudTimer += dt;
    if (hudTimer >= 0.2f) {
      hudTimer = 0f;
      notifyHud();
    }
  }

  private void updateBlocked(float dt) {
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        if (blockedTimer[r][c] > 0f) {
          blockedTimer[r][c] -= dt;
          if (blockedTimer[r][c] < 0f) {
            blockedTimer[r][c] = 0f;
          }
        }
      }
    }
    if (Math.random() < 0.01f) {
      int r = 2 + (int) (Math.random() * (rows - 4));
      int c = 2 + (int) (Math.random() * (cols - 4));
      if (type[r][c] == TYPE_STRAIGHT || type[r][c] == TYPE_CURVE || type[r][c] == TYPE_TJUNCTION) {
        blockedTimer[r][c] = 3f;
      }
    }
  }

  private void moveCart(float distance) {
    cartX += dirX(cartDir) * distance;
    cartY += dirY(cartDir) * distance;

    int gridX = Math.round(cartX - 0.5f);
    int gridY = Math.round(cartY - 0.5f);
    if (gridX < 0 || gridX >= cols || gridY < 0 || gridY >= rows) {
      gameOver();
      return;
    }
    float centerX = gridX + 0.5f;
    float centerY = gridY + 0.5f;
    float dx = cartX - centerX;
    float dy = cartY - centerY;
    if (Math.abs(dx) < 0.1f && Math.abs(dy) < 0.1f) {
      handleTile(gridY, gridX);
    }
  }

  private void handleTile(int r, int c) {
    if (blockedTimer[r][c] > 0f) {
      crash();
      return;
    }
    int tile = type[r][c];
    int orient = orientation[r][c];
    if (tile == TYPE_CURVE && altOrientation[r][c] >= 0) {
      orient = switchState[r][c] == 0 ? orientation[r][c] : altOrientation[r][c];
    }
    if (tile == TYPE_BARRIER) {
      crash();
      return;
    }
    if (tile == TYPE_DEAD_END) {
      crash();
      return;
    }
    if (tile == TYPE_DEPOT) {
      handleDepot(r, c);
    }
    if (tile == TYPE_SPAWN) {
      handleSpawn(r, c);
    }
    int nextDir = resolveDirection(tile, orient, cartDir, switchState[r][c]);
    if (nextDir < 0) {
      crash();
      return;
    }
    cartDir = nextDir;
  }

  private void handleDepot(int r, int c) {
    if (parcelColor >= 0 && depotColor[r][c] == parcelColor) {
      score += 100 * combo;
      combo += 1;
      timeLeft += 5f;
      particles.emit(cartX, cartY, getParcelColor(parcelColor));
      parcelColor = -1;
    }
  }

  private void handleSpawn(int r, int c) {
    if (parcelColor < 0) {
      parcelColor = spawnColor[r][c];
    }
  }

  private int resolveDirection(int tile, int orient, int incoming, int switchValue) {
    if (tile == TYPE_STRAIGHT || tile == TYPE_DEPOT || tile == TYPE_SPAWN) {
      if (orient == 0) {
        if (incoming == DIR_UP || incoming == DIR_DOWN) {
          return incoming;
        }
      } else {
        if (incoming == DIR_LEFT || incoming == DIR_RIGHT) {
          return incoming;
        }
      }
      return -1;
    }
    if (tile == TYPE_CURVE) {
      if (orient == 0) {
        if (incoming == DIR_UP) return DIR_RIGHT;
        if (incoming == DIR_LEFT) return DIR_DOWN;
      } else if (orient == 1) {
        if (incoming == DIR_RIGHT) return DIR_DOWN;
        if (incoming == DIR_UP) return DIR_LEFT;
      } else if (orient == 2) {
        if (incoming == DIR_DOWN) return DIR_LEFT;
        if (incoming == DIR_RIGHT) return DIR_UP;
      } else if (orient == 3) {
        if (incoming == DIR_LEFT) return DIR_UP;
        if (incoming == DIR_DOWN) return DIR_RIGHT;
      }
      return -1;
    }
    if (tile == TYPE_TJUNCTION) {
      int missing = orient;
      if (incoming == missing) {
        return -1;
      }
      int optionA = (missing + 1) % 4;
      int optionB = (missing + 3) % 4;
      int forward = (incoming + 2) % 4;
      if (forward == missing) {
        return switchValue == 0 ? optionA : optionB;
      }
      return switchValue == 0 ? forward : (forward == optionA ? optionB : optionA);
    }
    if (tile == TYPE_CROSS) {
      return incoming;
    }
    return -1;
  }

  private void crash() {
    particles.emit(cartX, cartY, colorBlocked);
    gameOver();
  }

  private void gameOver() {
    changeState(GameState.GAME_OVER);
  }

  private void notifyHud() {
    if (listener != null) {
      String parcel = parcelColor >= 0 ? parcelName(parcelColor) : "NONE";
      int displayTime = Math.max(0, Math.round(timeLeft));
      boolean brakeReady = brakeCooldown <= 0f;
      mainHandler.post(() -> listener.onHudUpdate(score, combo, displayTime, parcel, brakeReady, state));
    }
  }

  private String parcelName(int color) {
    if (color == 0) return "RED";
    if (color == 1) return "BLUE";
    if (color == 2) return "GREEN";
    return "YELLOW";
  }

  private int getParcelColor(int color) {
    if (color == 0) return colorRed;
    if (color == 1) return colorBlue;
    if (color == 2) return colorGreen;
    return colorYellow;
  }

  private float dirX(int dir) {
    if (dir == DIR_RIGHT) return 1f;
    if (dir == DIR_LEFT) return -1f;
    return 0f;
  }

  private float dirY(int dir) {
    if (dir == DIR_DOWN) return 1f;
    if (dir == DIR_UP) return -1f;
    return 0f;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      float x = event.getX();
      float y = event.getY();
      int col = (int) ((x - originX) / cellSize);
      int row = (int) ((y - originY) / cellSize);
      if (row >= 0 && row < rows && col >= 0 && col < cols) {
        int tile = type[row][col];
        if (tile == TYPE_TJUNCTION || tile == TYPE_CURVE) {
          switchState[row][col] = 1 - switchState[row][col];
          return true;
        }
      }
    }
    return true;
  }

  private void drawFrame() {
    Canvas canvas = surfaceHolder.lockCanvas();
    if (canvas == null) {
      return;
    }
    canvas.drawColor(getResources().getColor(R.color.cst_bg_main));
    drawGrid(canvas);
    particles.draw(canvas, cellSize, originX, originY);
    drawCart(canvas);
    surfaceHolder.unlockCanvasAndPost(canvas);
  }

  private void drawGrid(Canvas canvas) {
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeCap(Paint.Cap.ROUND);
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        drawTile(canvas, r, c);
      }
    }
  }

  private void drawTile(Canvas canvas, int r, int c) {
    int tile = type[r][c];
    if (tile == TYPE_EMPTY) {
      return;
    }
    float left = originX + c * cellSize;
    float top = originY + r * cellSize;
    float cx = left + cellSize * 0.5f;
    float cy = top + cellSize * 0.5f;
    float half = cellSize * 0.45f;

    if (blockedTimer[r][c] > 0f) {
      paint.setColor(colorBlocked);
      paint.setStrokeWidth(railStrokeBold);
      canvas.drawLine(cx - half, cy - half, cx + half, cy + half, paint);
      canvas.drawLine(cx + half, cy - half, cx - half, cy + half, paint);
      return;
    }

    paint.setColor(colorRail);
    paint.setStrokeWidth(railStrokeBold);

    if (tile == TYPE_STRAIGHT || tile == TYPE_DEPOT || tile == TYPE_SPAWN) {
      if (orientation[r][c] == 0) {
        canvas.drawLine(cx, cy - half, cx, cy + half, paint);
      } else {
        canvas.drawLine(cx - half, cy, cx + half, cy, paint);
      }
    } else if (tile == TYPE_CURVE) {
      int orient = orientation[r][c];
      if (altOrientation[r][c] >= 0 && switchState[r][c] == 1) {
        orient = altOrientation[r][c];
      }
      drawCurve(canvas, cx, cy, half, orient);
    } else if (tile == TYPE_TJUNCTION) {
      drawT(canvas, cx, cy, half, orientation[r][c], switchState[r][c]);
    } else if (tile == TYPE_CROSS) {
      canvas.drawLine(cx, cy - half, cx, cy + half, paint);
      canvas.drawLine(cx - half, cy, cx + half, cy, paint);
    } else if (tile == TYPE_DEAD_END) {
      paint.setColor(colorRailDark);
      canvas.drawLine(cx, cy - half, cx, cy, paint);
      paint.setColor(colorBlocked);
      paint.setStrokeWidth(railStroke);
      canvas.drawLine(cx - half * 0.3f, cy - half, cx + half * 0.3f, cy - half, paint);
    }

    if (tile == TYPE_DEPOT) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(getParcelColor(depotColor[r][c]));
      canvas.drawCircle(cx, cy, cellSize * 0.18f, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(colorRailDark);
    }
    if (tile == TYPE_SPAWN) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(getParcelColor(spawnColor[r][c]));
      canvas.drawRect(cx - cellSize * 0.18f, cy - cellSize * 0.18f, cx + cellSize * 0.18f, cy + cellSize * 0.18f, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(colorRailDark);
    }
  }

  private void drawCurve(Canvas canvas, float cx, float cy, float half, int orient) {
    if (orient == 0) {
      canvas.drawLine(cx, cy - half, cx, cy, paint);
      canvas.drawLine(cx, cy, cx + half, cy, paint);
    } else if (orient == 1) {
      canvas.drawLine(cx + half, cy, cx, cy, paint);
      canvas.drawLine(cx, cy, cx, cy + half, paint);
    } else if (orient == 2) {
      canvas.drawLine(cx, cy + half, cx, cy, paint);
      canvas.drawLine(cx, cy, cx - half, cy, paint);
    } else {
      canvas.drawLine(cx - half, cy, cx, cy, paint);
      canvas.drawLine(cx, cy, cx, cy - half, paint);
    }
  }

  private void drawT(Canvas canvas, float cx, float cy, float half, int missing, int switchValue) {
    paint.setColor(colorRail);
    paint.setStrokeWidth(railStrokeBold);
    if (missing != DIR_UP) {
      canvas.drawLine(cx, cy - half, cx, cy, paint);
    }
    if (missing != DIR_RIGHT) {
      canvas.drawLine(cx, cy, cx + half, cy, paint);
    }
    if (missing != DIR_DOWN) {
      canvas.drawLine(cx, cy, cx, cy + half, paint);
    }
    if (missing != DIR_LEFT) {
      canvas.drawLine(cx - half, cy, cx, cy, paint);
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(switchValue == 0 ? colorRail : colorRailDark);
    canvas.drawCircle(cx, cy, cellSize * 0.08f, paint);
    paint.setStyle(Paint.Style.STROKE);
  }

  private void drawCart(Canvas canvas) {
    float px = originX + cartX * cellSize;
    float py = originY + cartY * cellSize;
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(colorCartGlow);
    canvas.drawCircle(px, py, glowRadius, paint);
    paint.setColor(colorText);
    canvas.drawCircle(px, py, cartRadius, paint);
  }

  private static class ParticleSystem {
    private static final int MAX = 64;
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final float[] life = new float[MAX];
    private final int[] color = new int[MAX];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    void emit(float px, float py, int c) {
      for (int i = 0; i < MAX; i++) {
        if (life[i] <= 0f) {
          x[i] = px;
          y[i] = py;
          float angle = (float) (Math.random() * Math.PI * 2);
          float speed = 0.8f + (float) Math.random() * 1.2f;
          vx[i] = (float) Math.cos(angle) * speed;
          vy[i] = (float) Math.sin(angle) * speed;
          life[i] = 0.6f;
          color[i] = c;
          return;
        }
      }
    }

    void update(float dt) {
      for (int i = 0; i < MAX; i++) {
        if (life[i] > 0f) {
          life[i] -= dt;
          x[i] += vx[i] * dt;
          y[i] += vy[i] * dt;
        }
      }
    }

    void draw(Canvas canvas, float cell, float originX, float originY) {
      paint.setStyle(Paint.Style.FILL);
      for (int i = 0; i < MAX; i++) {
        if (life[i] > 0f) {
          paint.setColor(color[i]);
          float px = originX + x[i] * cell;
          float py = originY + y[i] * cell;
          canvas.drawCircle(px, py, cell * 0.06f, paint);
        }
      }
    }

    void clear() {
      for (int i = 0; i < MAX; i++) {
        life[i] = 0f;
      }
    }
  }
}
