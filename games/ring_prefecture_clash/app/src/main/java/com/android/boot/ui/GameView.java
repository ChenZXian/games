package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.SimpleAudio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
  public enum UpgradeType { LOGISTICS, MORALE, COMMAND }

  public interface HudListener {
    void onHud(HudSnapshot hud);
  }

  public interface ResultListener {
    void onResult(ResultSnapshot result);
  }

  public static class HudSnapshot {
    public String regionLabel = "";
    public String stageLabel = "";
    public String frontLabel = "";
    public String objectiveLabel = "";
    public String medalLabel = "";
    public String nodeName = "";
    public String nodeBonus = "";
    public String nodeUnits = "";
    public String nodeGrowth = "";
    public String timeLabel = "";
    public String stageSummary = "";
    public String stageRules = "";
    public int frontMeter = 50;
    public int surgeCharges = 0;
    public int boostCharges = 0;
    public int guardCharges = 0;
  }

  public static class ResultSnapshot {
    public boolean victory;
    public String summary = "";
  }

  private static class Node {
    String name;
    String type;
    float x;
    float y;
    int owner;
    int units;
    int capacity;
    float growth;
    int[] links;
    boolean capital;
    float bonusTimer;
    float guardTimer;
  }

  private static class Stream {
    int owner;
    int fromIndex;
    int toIndex;
    float progress;
    float speed;
    int power;
    boolean boosted;
  }

  private static class StageData {
    String region;
    String title;
    String ruleText;
    Node[] nodes;
    int index;
    float aiAggression;
    float railBoost;
    float moraleBoost;
    float portBoost;
  }

  private static class UpgradeState {
    int logistics;
    int morale;
    int command;
    int medals;
  }

  private static class AssetProfile {
    Bitmap playerA;
    Bitmap playerB;
    Bitmap enemyA;
    Bitmap enemyB;
    Bitmap capital;
    Bitmap city;
    Bitmap rail;
    Bitmap shrine;
    Bitmap port;
    Bitmap background;
  }

  private final SurfaceHolder holder;
  private final Paint bgPaint = new Paint();
  private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint routeEnemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint routeHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint streamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint neutralRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF nodeRect = new RectF();
  private final Random random = new Random(14);
  private final HudSnapshot hud = new HudSnapshot();
  private final ResultSnapshot result = new ResultSnapshot();
  private final List<StageData> stages = new ArrayList<>();
  private final List<Stream> streams = new ArrayList<>();
  private final SimpleAudio audio;
  private final AssetProfile assets = new AssetProfile();

  private Thread loopThread;
  private boolean running;
  private boolean paused = true;
  private boolean inResult;
  private boolean inMenu = true;
  private HudListener hudListener;
  private ResultListener resultListener;
  private StageData currentStage;
  private final UpgradeState upgrades = new UpgradeState();
  private int selectedStage;
  private int currentNodeIndex = -1;
  private int dragStartNodeIndex = -1;
  private float dragX;
  private float dragY;
  private float aiDecisionTimer;
  private float elapsedSeconds;
  private float updateSpeed = 1f;
  private float sendPercent = 0.7f;
  private long lastTapTime;
  private int lastTapNode = -1;
  private float skillSurgeTimer;
  private float skillBoostTimer;
  private float skillGuardTimer;
  private int surgeCharges = 1;
  private int boostCharges = 1;
  private int guardCharges = 1;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    holder = getHolder();
    holder.addCallback(this);
    bgPaint.setColor(Color.parseColor("#F5FBFF"));
    routePaint.setColor(Color.parseColor("#60BCD9"));
    routePaint.setStrokeWidth(8f);
    routePaint.setStyle(Paint.Style.STROKE);
    routeEnemyPaint.setColor(Color.parseColor("#E07B7B"));
    routeEnemyPaint.setStrokeWidth(8f);
    routeEnemyPaint.setStyle(Paint.Style.STROKE);
    routeHighlightPaint.setColor(Color.parseColor("#FFC95E"));
    routeHighlightPaint.setStrokeWidth(10f);
    routeHighlightPaint.setStyle(Paint.Style.STROKE);
    nodePaint.setStyle(Paint.Style.FILL);
    textPaint.setColor(Color.parseColor("#17233B"));
    textPaint.setTextSize(34f);
    textPaint.setFakeBoldText(true);
    smallTextPaint.setColor(Color.parseColor("#35568A"));
    smallTextPaint.setTextSize(22f);
    streamPaint.setStyle(Paint.Style.FILL);
    pulsePaint.setStyle(Paint.Style.STROKE);
    pulsePaint.setStrokeWidth(6f);
    neutralRingPaint.setColor(Color.parseColor("#9AB4C8"));
    neutralRingPaint.setStyle(Paint.Style.STROKE);
    neutralRingPaint.setStrokeWidth(5f);
    audio = new SimpleAudio(context);
    buildStages();
    loadArtProfile();
    setFocusable(true);
    updateHud();
  }

  public void setHudListener(HudListener hudListener) {
    this.hudListener = hudListener;
    updateHud();
  }

  public void setResultListener(ResultListener resultListener) {
    this.resultListener = resultListener;
  }

  public HudSnapshot getHudSnapshot() {
    return hud;
  }

  public String getMedalLabel() {
    return "Medals " + upgrades.medals;
  }

  public String getUpgradeLabel(UpgradeType type) {
    if (type == UpgradeType.LOGISTICS) {
      return "Logistics Lv " + upgrades.logistics;
    }
    if (type == UpgradeType.MORALE) {
      return "Morale Lv " + upgrades.morale;
    }
    return "Command Lv " + upgrades.command;
  }

  public void purchaseUpgrade(UpgradeType type) {
    if (upgrades.medals <= 0) {
      return;
    }
    upgrades.medals--;
    if (type == UpgradeType.LOGISTICS) {
      upgrades.logistics++;
    } else if (type == UpgradeType.MORALE) {
      upgrades.morale++;
    } else {
      upgrades.command++;
      surgeCharges++;
      boostCharges++;
      guardCharges++;
    }
    updateHud();
  }

  public void startSelectedStage() {
    startStage(selectedStage);
  }

  public void shiftSelectedStage(int delta) {
    selectedStage += delta;
    if (selectedStage < 0) {
      selectedStage = stages.size() - 1;
    }
    if (selectedStage >= stages.size()) {
      selectedStage = 0;
    }
    updateHud();
  }

  public void pauseGame() {
    paused = true;
  }

  public void resumeGame() {
    if (!inResult) {
      paused = false;
    }
  }

  public void restartCurrentStage() {
    if (currentStage != null) {
      startStage(currentStage.index);
    }
  }

  public void returnToMenu() {
    paused = true;
    inMenu = true;
    inResult = false;
    currentNodeIndex = -1;
    audio.playBgm("bgm_menu.wav");
    updateHud();
  }

  public void advanceAfterResult() {
    if (result.victory) {
      selectedStage = Math.min(selectedStage + 1, stages.size() - 1);
    }
    startStage(selectedStage);
  }

  public String toggleSpeedLabel() {
    updateSpeed = updateSpeed > 1.1f ? 1f : 2f;
    return updateSpeed > 1.1f ? "2x" : "1x";
  }

  public String toggleSendModeLabel() {
    sendPercent = sendPercent > 0.5f ? 0.35f : 0.7f;
    return sendPercent > 0.5f ? "70%" : "35%";
  }

  public void triggerSkill(int index) {
    if (paused || currentStage == null || inMenu || inResult) {
      return;
    }
    if (index == 0 && surgeCharges > 0 && skillSurgeTimer <= 0f) {
      surgeCharges--;
      skillSurgeTimer = 8f + upgrades.command;
      audio.playSfx("sfx_attack.wav");
    } else if (index == 1 && boostCharges > 0 && skillBoostTimer <= 0f) {
      boostCharges--;
      skillBoostTimer = 10f + upgrades.command * 1.2f;
      audio.playSfx("sfx_collect.wav");
    } else if (index == 2 && guardCharges > 0 && skillGuardTimer <= 0f) {
      guardCharges--;
      skillGuardTimer = 10f + upgrades.command;
      for (Node node : currentStage.nodes) {
        if (node.owner == 1) {
          node.guardTimer = 10f + upgrades.command;
        }
      }
      audio.playSfx("sfx_build.wav");
    }
    updateHud();
  }

  public void resumeView() {
    if (loopThread == null && holder.getSurface().isValid()) {
      running = true;
      loopThread = new Thread(this);
      loopThread.start();
    }
  }

  public void pauseView() {
    running = false;
    if (loopThread != null) {
      try {
        loopThread.join(400);
      } catch (InterruptedException ignored) {
      }
      loopThread = null;
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    running = true;
    loopThread = new Thread(this);
    loopThread.start();
    audio.playBgm("bgm_menu.wav");
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    pauseView();
    audio.release();
  }

  @Override
  public void run() {
    long last = SystemClock.elapsedRealtime();
    while (running) {
      long now = SystemClock.elapsedRealtime();
      float delta = Math.min(0.033f, (now - last) / 1000f);
      last = now;
      if (!paused && currentStage != null && !inResult) {
        update(delta * updateSpeed);
      }
      drawFrame();
      SystemClock.sleep(16);
    }
  }

  private void startStage(int index) {
    selectedStage = index;
    currentStage = cloneStage(stages.get(index));
    streams.clear();
    paused = false;
    inResult = false;
    inMenu = false;
    currentNodeIndex = -1;
    dragStartNodeIndex = -1;
    elapsedSeconds = 0f;
    aiDecisionTimer = 0f;
    skillSurgeTimer = 0f;
    skillBoostTimer = 0f;
    skillGuardTimer = 0f;
    surgeCharges = 1 + upgrades.command / 2;
    boostCharges = 1 + upgrades.command / 2;
    guardCharges = 1 + upgrades.command / 2;
    result.summary = "";
    audio.playBgm("bgm.wav");
    updateHud();
  }

  private StageData cloneStage(StageData source) {
    StageData stage = new StageData();
    stage.region = source.region;
    stage.title = source.title;
    stage.ruleText = source.ruleText;
    stage.index = source.index;
    stage.aiAggression = source.aiAggression;
    stage.railBoost = source.railBoost;
    stage.moraleBoost = source.moraleBoost;
    stage.portBoost = source.portBoost;
    stage.nodes = new Node[source.nodes.length];
    for (int i = 0; i < source.nodes.length; i++) {
      Node from = source.nodes[i];
      Node node = new Node();
      node.name = from.name;
      node.type = from.type;
      node.x = from.x;
      node.y = from.y;
      node.owner = from.owner;
      node.units = from.units;
      node.capacity = from.capacity;
      node.growth = from.growth;
      node.links = from.links.clone();
      node.capital = from.capital;
      stage.nodes[i] = node;
    }
    return stage;
  }

  private void update(float dt) {
    elapsedSeconds += dt;
    skillSurgeTimer = Math.max(0f, skillSurgeTimer - dt);
    skillBoostTimer = Math.max(0f, skillBoostTimer - dt);
    skillGuardTimer = Math.max(0f, skillGuardTimer - dt);
    for (Node node : currentStage.nodes) {
      node.bonusTimer = Math.max(0f, node.bonusTimer - dt);
      node.guardTimer = Math.max(0f, node.guardTimer - dt);
      if (node.owner == 0) {
        continue;
      }
      float growthFactor = 1f;
      if ("rail".equals(node.type)) {
        growthFactor += currentStage.railBoost;
      } else if ("shrine".equals(node.type)) {
        growthFactor += currentStage.moraleBoost;
      } else if ("port".equals(node.type)) {
        growthFactor += currentStage.portBoost;
      }
      if (node.owner == 1) {
        growthFactor += upgrades.morale * 0.08f;
        if (skillBoostTimer > 0f) {
          growthFactor += 0.45f;
        }
      }
      node.bonusTimer += dt * node.growth * growthFactor;
      while (node.bonusTimer >= 1f && node.units < node.capacity) {
        node.units++;
        node.bonusTimer -= 1f;
      }
    }

    for (int i = streams.size() - 1; i >= 0; i--) {
      Stream stream = streams.get(i);
      Node from = currentStage.nodes[stream.fromIndex];
      Node to = currentStage.nodes[stream.toIndex];
      float speed = stream.speed;
      if ("rail".equals(from.type) || "rail".equals(to.type)) {
        speed *= 1.2f + currentStage.railBoost;
      }
      if ("port".equals(from.type) || "port".equals(to.type)) {
        speed *= 1.08f + currentStage.portBoost * 0.5f;
      }
      if (stream.owner == 1 && skillSurgeTimer > 0f) {
        speed *= 1.35f;
      }
      stream.progress += dt * speed;
      if (stream.progress >= 1f) {
        resolveArrival(stream, to);
        streams.remove(i);
      }
    }

    aiDecisionTimer += dt;
    if (aiDecisionTimer >= Math.max(0.28f, 0.9f - currentStage.aiAggression * 0.2f)) {
      aiDecisionTimer = 0f;
      runAiTurn();
    }

    int playerOwned = 0;
    int enemyOwned = 0;
    int playerCapital = 0;
    int enemyCapital = 0;
    for (Node node : currentStage.nodes) {
      if (node.owner == 1) {
        playerOwned++;
        if (node.capital) {
          playerCapital++;
        }
      } else if (node.owner == 2) {
        enemyOwned++;
        if (node.capital) {
          enemyCapital++;
        }
      }
    }
    if (enemyCapital == 0 || enemyOwned == 0) {
      finishStage(true);
    } else if (playerCapital == 0 || playerOwned == 0) {
      finishStage(false);
    }
    updateHud();
  }

  private void runAiTurn() {
    int bestFrom = -1;
    int bestTo = -1;
    float bestScore = -999f;
    for (int i = 0; i < currentStage.nodes.length; i++) {
      Node node = currentStage.nodes[i];
      if (node.owner != 2 || node.units < 10) {
        continue;
      }
      for (int targetIndex : node.links) {
        Node target = currentStage.nodes[targetIndex];
        float score = 0f;
        if (target.owner == 1) {
          score += 18f;
          if (target.capital) {
            score += 12f;
          }
        } else if (target.owner == 0) {
          score += 9f;
        } else {
          continue;
        }
        score += node.units * 0.2f - target.units * 0.17f;
        if ("rail".equals(target.type)) {
          score += 3f;
        }
        if ("shrine".equals(target.type)) {
          score += 2f;
        }
        if (random.nextFloat() * 2f > 1.2f) {
          score += currentStage.aiAggression * 1.5f;
        }
        if (score > bestScore) {
          bestScore = score;
          bestFrom = i;
          bestTo = targetIndex;
        }
      }
    }
    if (bestFrom >= 0 && bestTo >= 0) {
      sendUnits(bestFrom, bestTo, 0.62f + currentStage.aiAggression * 0.06f, 2);
    }
  }

  private void resolveArrival(Stream stream, Node target) {
    if (target.owner == stream.owner) {
      target.units = Math.min(target.capacity, target.units + stream.power);
    } else {
      int damage = stream.power;
      if (target.guardTimer > 0f && target.owner == 1) {
        damage = Math.max(1, (int) (damage * 0.7f));
      }
      target.units -= damage;
      if (target.units < 0) {
        target.owner = stream.owner;
        target.units = Math.min(target.capacity / 2, Math.abs(target.units));
        if (stream.owner == 1) {
          audio.playSfx("sfx_capture.wav");
        }
      } else if (stream.owner == 1) {
        audio.playSfx("sfx_attack.wav");
      }
    }
  }

  private void finishStage(boolean victory) {
    if (inResult) {
      return;
    }
    paused = true;
    inResult = true;
    result.victory = victory;
    if (victory) {
      int earned = 2 + (selectedStage % 3 == 0 ? 1 : 0);
      upgrades.medals += earned;
      result.summary = "Secured " + currentStage.title + " and earned " + earned + " medals.";
      audio.playSfx("sfx_win.wav");
    } else {
      result.summary = "The rival front collapsed your command net. Rebuild and try again.";
      audio.playSfx("sfx_fail.wav");
    }
    if (resultListener != null) {
      resultListener.onResult(result);
    }
    updateHud();
  }

  private void updateHud() {
    StageData stage = currentStage != null ? currentStage : stages.get(selectedStage);
    hud.regionLabel = stage.region;
    hud.stageLabel = (stage.index + 1) + " / " + stages.size();
    hud.stageSummary = stage.title;
    hud.stageRules = stage.ruleText;
    hud.objectiveLabel = "Capture all rival capitals";
    hud.medalLabel = getMedalLabel();
    hud.surgeCharges = surgeCharges;
    hud.boostCharges = boostCharges;
    hud.guardCharges = guardCharges;
    hud.timeLabel = formatTime((int) elapsedSeconds);
    if (currentStage == null) {
      hud.frontLabel = "Planning";
      hud.frontMeter = 50;
      hud.nodeName = "Select a campaign stage";
      hud.nodeBonus = "Rail speed, shrine morale, and port supply vary by region.";
      hud.nodeUnits = "0";
      hud.nodeGrowth = "+0";
    } else {
      int player = 0;
      int enemy = 0;
      int playerUnits = 0;
      int enemyUnits = 0;
      for (Node node : currentStage.nodes) {
        if (node.owner == 1) {
          player++;
          playerUnits += node.units;
        } else if (node.owner == 2) {
          enemy++;
          enemyUnits += node.units;
        }
      }
      int total = Math.max(1, player + enemy);
      hud.frontMeter = Math.max(5, Math.min(95, (int) ((player * 100f) / total)));
      if (playerUnits > enemyUnits + 12) {
        hud.frontLabel = "Pressing";
      } else if (enemyUnits > playerUnits + 12) {
        hud.frontLabel = "Under Pressure";
      } else {
        hud.frontLabel = "Stable";
      }
      if (currentNodeIndex >= 0 && currentNodeIndex < currentStage.nodes.length) {
        Node node = currentStage.nodes[currentNodeIndex];
        hud.nodeName = node.name;
        hud.nodeBonus = describeBonus(node.type);
        hud.nodeUnits = String.valueOf(node.units);
        hud.nodeGrowth = String.format(Locale.US, "+%.1f", node.growth);
      } else {
        hud.nodeName = stage.title;
        hud.nodeBonus = stage.ruleText;
        hud.nodeUnits = String.valueOf(playerUnits);
        hud.nodeGrowth = String.format(Locale.US, "+%.1f", 1.6f + upgrades.morale * 0.1f);
      }
    }
    if (hudListener != null) {
      hudListener.onHud(hud);
    }
  }

  private String describeBonus(String type) {
    if ("rail".equals(type)) {
      return "Rail Hub: faster streams and stronger growth";
    }
    if ("shrine".equals(type)) {
      return "Shrine: morale surge and steady growth";
    }
    if ("port".equals(type)) {
      return "Port: flexible supply route bonus";
    }
    if ("capital".equals(type)) {
      return "Capital: command node and victory target";
    }
    return "City: stable production anchor";
  }

  private String formatTime(int total) {
    int minutes = total / 60;
    int seconds = total % 60;
    return String.format(Locale.US, "%02d:%02d", minutes, seconds);
  }

  private void drawFrame() {
    if (!holder.getSurface().isValid()) {
      return;
    }
    Canvas canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
    canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
    if (assets.background != null) {
      canvas.drawBitmap(assets.background, null, new RectF(0f, 0f, getWidth(), getHeight()), null);
    }
    if (currentStage != null) {
      drawRoutes(canvas);
      drawNodes(canvas);
      drawStreams(canvas);
      if (dragStartNodeIndex >= 0) {
        Node start = currentStage.nodes[dragStartNodeIndex];
        routeHighlightPaint.setAlpha(160);
        canvas.drawLine(scaleX(start.x), scaleY(start.y), dragX, dragY, routeHighlightPaint);
      }
    } else {
      smallTextPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText("Open Stage Map to review all 40 campaign battles.", getWidth() * 0.5f, getHeight() * 0.48f, textPaint);
      canvas.drawText("Round battalions, rail speed routes, shrine morale, and port supply all matter.", getWidth() * 0.5f, getHeight() * 0.56f, smallTextPaint);
    }
    holder.unlockCanvasAndPost(canvas);
  }

  private void drawRoutes(Canvas canvas) {
    for (int i = 0; i < currentStage.nodes.length; i++) {
      Node node = currentStage.nodes[i];
      for (int target : node.links) {
        if (target < i) {
          continue;
        }
        Node other = currentStage.nodes[target];
        Paint paint = node.owner == 2 && other.owner == 2 ? routeEnemyPaint : routePaint;
        canvas.drawLine(scaleX(node.x), scaleY(node.y), scaleX(other.x), scaleY(other.y), paint);
      }
    }
  }

  private void drawNodes(Canvas canvas) {
    for (Node node : currentStage.nodes) {
      float cx = scaleX(node.x);
      float cy = scaleY(node.y);
      float radius = node.capital ? 46f : 34f;
      Bitmap badge = badgeFor(node);
      if (badge != null) {
        nodeRect.set(cx - radius * 1.2f, cy - radius * 1.2f, cx + radius * 1.2f, cy + radius * 1.2f);
        canvas.drawBitmap(badge, null, nodeRect, null);
      } else {
        nodePaint.setColor(colorFor(node.owner));
        canvas.drawCircle(cx, cy, radius, nodePaint);
      }
      if (node.owner == 0) {
        canvas.drawCircle(cx, cy, radius + 8f, neutralRingPaint);
      }
      if (node.guardTimer > 0f) {
        pulsePaint.setColor(Color.parseColor("#FFB020"));
        canvas.drawCircle(cx, cy, radius + 14f, pulsePaint);
      }
      if (node == currentStage.nodes[currentNodeIndex < 0 ? 0 : currentNodeIndex] && currentNodeIndex >= 0) {
        pulsePaint.setColor(Color.parseColor("#33A1FF"));
        canvas.drawCircle(cx, cy, radius + 10f, pulsePaint);
      }
      textPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(String.valueOf(node.units), cx, cy + 12f, textPaint);
      smallTextPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(node.name, cx, cy - radius - 14f, smallTextPaint);
    }
  }

  private void drawStreams(Canvas canvas) {
    for (Stream stream : streams) {
      Node from = currentStage.nodes[stream.fromIndex];
      Node to = currentStage.nodes[stream.toIndex];
      float x = lerp(scaleX(from.x), scaleX(to.x), stream.progress);
      float y = lerp(scaleY(from.y), scaleY(to.y), stream.progress);
      Bitmap token = tokenFor(stream.owner, ((int) (stream.progress * 8)) % 2 == 0);
      float size = 24f + Math.min(stream.power, 10) * 1.4f;
      if (token != null) {
        nodeRect.set(x - size, y - size, x + size, y + size);
        canvas.drawBitmap(token, null, nodeRect, null);
      } else {
        streamPaint.setColor(colorFor(stream.owner));
        canvas.drawCircle(x, y, size, streamPaint);
      }
    }
  }

  private Bitmap badgeFor(Node node) {
    if ("capital".equals(node.type)) {
      return assets.capital;
    }
    if ("rail".equals(node.type)) {
      return assets.rail;
    }
    if ("shrine".equals(node.type)) {
      return assets.shrine;
    }
    if ("port".equals(node.type)) {
      return assets.port;
    }
    return assets.city;
  }

  private Bitmap tokenFor(int owner, boolean alt) {
    if (owner == 1) {
      return alt ? assets.playerA : assets.playerB;
    }
    return alt ? assets.enemyA : assets.enemyB;
  }

  private int colorFor(int owner) {
    if (owner == 1) {
      return Color.parseColor("#2E89FF");
    }
    if (owner == 2) {
      return Color.parseColor("#F26464");
    }
    return Color.parseColor("#A8BDCC");
  }

  private float scaleX(float normalized) {
    return normalized * getWidth();
  }

  private float scaleY(float normalized) {
    return normalized * getHeight();
  }

  private float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (paused || currentStage == null || inMenu || inResult) {
      return true;
    }
    dragX = event.getX();
    dragY = event.getY();
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      dragStartNodeIndex = findNode(event.getX(), event.getY());
      if (dragStartNodeIndex >= 0) {
        currentNodeIndex = dragStartNodeIndex;
        long now = SystemClock.elapsedRealtime();
        if (dragStartNodeIndex == lastTapNode && now - lastTapTime < 320) {
          dispatchAllIn(dragStartNodeIndex);
        }
        lastTapNode = dragStartNodeIndex;
        lastTapTime = now;
      }
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      int endNode = findNode(event.getX(), event.getY());
      if (dragStartNodeIndex >= 0 && endNode >= 0 && dragStartNodeIndex != endNode) {
        sendUnits(dragStartNodeIndex, endNode, sendPercent, 1);
      }
      dragStartNodeIndex = -1;
    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
      dragStartNodeIndex = -1;
    }
    updateHud();
    return true;
  }

  private void dispatchAllIn(int fromIndex) {
    Node node = currentStage.nodes[fromIndex];
    if (node.owner != 1 || node.units < 12) {
      return;
    }
    for (int target : node.links) {
      Node next = currentStage.nodes[target];
      if (next.owner != 1) {
        sendUnits(fromIndex, target, 0.9f, 1);
        break;
      }
    }
  }

  private int findNode(float x, float y) {
    for (int i = 0; i < currentStage.nodes.length; i++) {
      Node node = currentStage.nodes[i];
      float dx = x - scaleX(node.x);
      float dy = y - scaleY(node.y);
      float radius = node.capital ? 54f : 42f;
      if (dx * dx + dy * dy <= radius * radius) {
        return i;
      }
    }
    return -1;
  }

  private void sendUnits(int fromIndex, int toIndex, float ratio, int owner) {
    Node from = currentStage.nodes[fromIndex];
    if (from.owner != owner || !isConnected(from, toIndex) || from.units < 6) {
      return;
    }
    int moving = Math.max(4, (int) (from.units * ratio));
    moving = Math.min(moving, from.units - 2);
    if (moving <= 0) {
      return;
    }
    from.units -= moving;
    Stream stream = new Stream();
    stream.owner = owner;
    stream.fromIndex = fromIndex;
    stream.toIndex = toIndex;
    stream.progress = 0f;
    stream.speed = 0.28f + upgrades.logistics * 0.015f;
    stream.power = moving;
    stream.boosted = owner == 1 && skillSurgeTimer > 0f;
    streams.add(stream);
    if (owner == 1) {
      audio.playSfx("sfx_move.wav");
    }
  }

  private boolean isConnected(Node node, int target) {
    for (int link : node.links) {
      if (link == target) {
        return true;
      }
    }
    return false;
  }

  private void loadArtProfile() {
    try {
      AssetManager assetManager = getContext().getAssets();
      assets.playerA = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/player_round_a.png");
      assets.playerB = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/player_round_b.png");
      assets.enemyA = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/enemy_round_a.png");
      assets.enemyB = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/enemy_round_b.png");
      assets.capital = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/node_capital.png");
      assets.city = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/node_city.png");
      assets.rail = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/node_rail.png");
      assets.shrine = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/node_shrine.png");
      assets.port = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/node_port.png");
      assets.background = loadBitmap(assetManager, "game_art/ring_prefecture_tokens/map_backdrop.png");
      readRuntimeArtMap(assetManager);
    } catch (Exception ignored) {
    }
  }

  private Bitmap loadBitmap(AssetManager assetManager, String path) {
    try {
      InputStream inputStream = assetManager.open(path);
      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
      inputStream.close();
      return bitmap;
    } catch (Exception ignored) {
      return null;
    }
  }

  private void readRuntimeArtMap(AssetManager assetManager) {
    try {
      InputStream inputStream = assetManager.open("game_art/runtime_art_map.json");
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
      reader.close();
      inputStream.close();
      JSONObject root = new JSONObject(builder.toString());
      JSONArray entities = root.getJSONArray("entities");
      if (entities.length() > 0) {
        hud.nodeBonus = "Runtime art map integrated";
      }
    } catch (Exception ignored) {
    }
  }

  private void buildStages() {
    String[] regions = {"Hokkaido Arc", "Tohoku Drive", "Kanto Pulse", "Chubu Crosswind", "Kansai Chain", "Kyushu Tide"};
    for (int i = 0; i < 40; i++) {
      stages.add(generateStage(i, regions[i % regions.length]));
    }
  }

  private StageData generateStage(int index, String region) {
    StageData stage = new StageData();
    stage.region = region;
    stage.index = index;
    stage.title = "Stage " + (index + 1) + "  " + region;
    stage.aiAggression = 1.1f + (index / 8) * 0.22f;
    stage.railBoost = index % 3 == 0 ? 0.25f : 0.12f;
    stage.moraleBoost = index % 4 == 1 ? 0.28f : 0.15f;
    stage.portBoost = index % 5 == 2 ? 0.24f : 0.1f;
    stage.ruleText = ruleTextFor(index);
    int template = index % 5;
    stage.nodes = templateNodes(template, index);
    return stage;
  }

  private String ruleTextFor(int index) {
    switch (index % 5) {
      case 0:
        return "Rail hubs accelerate stream speed and thicken front pressure.";
      case 1:
        return "Shrines add morale growth and reward compact defense.";
      case 2:
        return "Ports create flexible flank routes across water gaps.";
      case 3:
        return "Twin capitals demand multi-front reinforcement discipline.";
      default:
        return "Mountain junctions force careful route timing and reserve control.";
    }
  }

  private Node[] templateNodes(int template, int index) {
    float[][] positions;
    int[][] links;
    String[] types;
    boolean[] capitals;
    if (template == 0) {
      positions = new float[][]{{0.12f,0.50f},{0.25f,0.28f},{0.25f,0.72f},{0.42f,0.36f},{0.42f,0.64f},{0.60f,0.50f},{0.76f,0.28f},{0.76f,0.72f},{0.90f,0.50f}};
      links = new int[][]{{1,2},{0,3,4},{0,3,4},{1,2,5},{1,2,5},{3,4,6,7},{5,8},{5,8},{6,7}};
      types = new String[]{"capital","city","city","rail","shrine","city","rail","port","capital"};
      capitals = new boolean[]{true,false,false,false,false,false,false,false,true};
    } else if (template == 1) {
      positions = new float[][]{{0.12f,0.32f},{0.12f,0.68f},{0.30f,0.50f},{0.46f,0.28f},{0.46f,0.72f},{0.62f,0.50f},{0.78f,0.28f},{0.78f,0.72f},{0.90f,0.50f}};
      links = new int[][]{{2},{2},{0,1,3,4},{2,5,6},{2,5,7},{3,4,8},{3,8},{4,8},{5,6,7}};
      types = new String[]{"capital","capital","rail","city","shrine","city","port","rail","capital"};
      capitals = new boolean[]{true,true,false,false,false,false,false,false,true};
    } else if (template == 2) {
      positions = new float[][]{{0.14f,0.50f},{0.26f,0.26f},{0.26f,0.74f},{0.44f,0.22f},{0.44f,0.50f},{0.44f,0.78f},{0.64f,0.34f},{0.64f,0.66f},{0.84f,0.50f}};
      links = new int[][]{{1,2,4},{0,3,4},{0,4,5},{1,6},{0,1,2,6,7},{2,7},{3,4,8},{4,5,8},{6,7}};
      types = new String[]{"capital","port","rail","city","shrine","city","rail","port","capital"};
      capitals = new boolean[]{true,false,false,false,false,false,false,false,true};
    } else if (template == 3) {
      positions = new float[][]{{0.12f,0.50f},{0.28f,0.18f},{0.28f,0.50f},{0.28f,0.82f},{0.50f,0.32f},{0.50f,0.68f},{0.72f,0.18f},{0.72f,0.50f},{0.72f,0.82f},{0.90f,0.50f}};
      links = new int[][]{{1,2,3},{0,4},{0,4,5},{0,5},{1,2,6,7},{2,3,7,8},{4,9},{4,5,9},{5,9},{6,7,8}};
      types = new String[]{"capital","rail","city","port","shrine","rail","city","city","port","capital"};
      capitals = new boolean[]{true,false,false,false,false,false,false,false,false,true};
    } else {
      positions = new float[][]{{0.10f,0.50f},{0.24f,0.34f},{0.24f,0.66f},{0.40f,0.22f},{0.40f,0.50f},{0.40f,0.78f},{0.58f,0.34f},{0.58f,0.66f},{0.76f,0.50f},{0.90f,0.50f}};
      links = new int[][]{{1,2},{0,3,4},{0,4,5},{1,6},{1,2,6,7},{2,7},{3,4,8},{4,5,8},{6,7,9},{8}};
      types = new String[]{"capital","city","port","rail","shrine","rail","city","port","city","capital"};
      capitals = new boolean[]{true,false,false,false,false,false,false,false,false,true};
    }
    Node[] nodes = new Node[positions.length];
    for (int i = 0; i < positions.length; i++) {
      Node node = new Node();
      node.name = shortName(index, i, types[i]);
      node.type = types[i];
      node.capital = capitals[i];
      node.x = positions[i][0];
      node.y = positions[i][1];
      node.links = links[i];
      node.capacity = node.capital ? 90 : 68;
      node.growth = node.capital ? 1.45f : 1.0f + (i % 3) * 0.18f;
      if (i == 0 || (template == 1 && i == 1)) {
        node.owner = 1;
        node.units = node.capital ? 34 : 18;
      } else if (i == positions.length - 1) {
        node.owner = 2;
        node.units = 34 + index / 3;
      } else if (i > positions.length - 3 && template == 3) {
        node.owner = 2;
        node.units = 16 + index / 4;
      } else if (i % 4 == 0) {
        node.owner = 0;
        node.units = 12 + index / 6;
      } else if (i % 3 == 0 && index > 8) {
        node.owner = 2;
        node.units = 14 + index / 5;
      } else {
        node.owner = 0;
        node.units = 8 + (i % 5);
      }
      nodes[i] = node;
    }
    return nodes;
  }

  private String shortName(int stageIndex, int nodeIndex, String type) {
    String prefix = "City";
    if ("capital".equals(type)) {
      prefix = "Capital";
    } else if ("rail".equals(type)) {
      prefix = "Rail";
    } else if ("shrine".equals(type)) {
      prefix = "Shrine";
    } else if ("port".equals(type)) {
      prefix = "Port";
    }
    return prefix + " " + (stageIndex + 1) + "-" + (nodeIndex + 1);
  }
}
