package com.android.boot.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.GameAudio;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
  public enum Ability {
    NONE,
    RALLY,
    SHIELD_WALL,
    SCOUT_FLARE,
    REPAIR_BRIDGE
  }

  public enum UpgradeBranch {
    LOGISTICS,
    WAR_COUNCIL,
    IRON_DISCIPLINE
  }

  public interface Callbacks {
    void onHudChanged(HudSnapshot snapshot);
    void onBattleEnded(ResultSnapshot snapshot);
    void onMenuMusicNeeded(boolean menuVisible);
  }

  public static final class HudSnapshot {
    public final String provinceName;
    public final String ownedText;
    public final String enemyText;
    public final String incomeText;
    public final String commandText;
    public final String objectiveText;
    public final String doctrineText;
    public final String alertText;
    public final String rallyLabel;
    public final String shieldLabel;
    public final String scoutLabel;
    public final String repairLabel;

    public HudSnapshot(String provinceName, String ownedText, String enemyText, String incomeText, String commandText,
                       String objectiveText, String doctrineText, String alertText, String rallyLabel,
                       String shieldLabel, String scoutLabel, String repairLabel) {
      this.provinceName = provinceName;
      this.ownedText = ownedText;
      this.enemyText = enemyText;
      this.incomeText = incomeText;
      this.commandText = commandText;
      this.objectiveText = objectiveText;
      this.doctrineText = doctrineText;
      this.alertText = alertText;
      this.rallyLabel = rallyLabel;
      this.shieldLabel = shieldLabel;
      this.scoutLabel = scoutLabel;
      this.repairLabel = repairLabel;
    }
  }

  public static final class ResultSnapshot {
    public final boolean victory;
    public final String summary;

    public ResultSnapshot(boolean victory, String summary) {
      this.victory = victory;
      this.summary = summary;
    }
  }

  private enum NodeType {
    CAPITAL,
    CITY,
    FARM,
    MINE,
    BARRACKS,
    WATCHTOWER,
    FORT
  }

  private enum UnitType {
    LEVY,
    SPEAR,
    ARCHER,
    CAVALRY
  }

  private static final class Node {
    int id;
    String name;
    NodeType type;
    UnitType unitType;
    float x;
    float y;
    int owner;
    float troops;
    float growthRate;
    boolean connected;
    boolean visible;
    float shieldTimer;
    float rallyTimer;
    int[] links;
    boolean capital;
    boolean selected;
  }

  private static final class Link {
    int a;
    int b;
    boolean bridge;
    boolean broken;
    float pulse;
  }

  private static final class Packet {
    int owner;
    UnitType unitType;
    float amount;
    int fromId;
    int toId;
    float progress;
    float speed;
    boolean allied;
  }

  private static final class Chapter {
    int index;
    String provinceName;
    String objective;
    Node[] seeds;
    Link[] seedLinks;
  }

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF boardRect = new RectF();
  private final RectF tempRect = new RectF();
  private final Path path = new Path();
  private final Random random = new Random(77);
  private final ArrayList<Node> nodes = new ArrayList<>();
  private final ArrayList<Link> links = new ArrayList<>();
  private final ArrayList<Packet> packets = new ArrayList<>();
  private final ArrayList<Chapter> chapters = new ArrayList<>();
  private final HashMap<String, Bitmap> art = new HashMap<>();
  private final Runnable ticker = new Runnable() {
    @Override
    public void run() {
      if (!attached) {
        return;
      }
      long now = System.nanoTime();
      float dt = (now - lastFrameNs) / 1000000000f;
      if (dt < 0f) {
        dt = 0f;
      }
      if (dt > 0.033f) {
        dt = 0.033f;
      }
      lastFrameNs = now;
      update(dt);
      invalidate();
      handler.postDelayed(this, 16L);
    }
  };

  private Callbacks callbacks;
  private GameAudio audio;
  private final SharedPreferences prefs;
  private Node selectedNode;
  private Chapter currentChapter;
  private boolean attached;
  private boolean battleActive;
  private boolean paused;
  private boolean menuMode = true;
  private boolean ended;
  private long lastFrameNs;
  private float growthTick;
  private float aiTick;
  private float scoutRevealTimer;
  private float sendRatio = 0.5f;
  private float commandPoints = 8f;
  private float commandIncome = 0f;
  private int currentChapterIndex = 1;
  private Ability selectedAbility = Ability.NONE;
  private String alertText = "";
  private int logisticsLevel;
  private int councilLevel;
  private int disciplineLevel;

  public GameView(Context context) {
    super(context);
    prefs = context.getSharedPreferences("iron_banner_realms", Context.MODE_PRIVATE);
    init();
  }

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    prefs = context.getSharedPreferences("iron_banner_realms", Context.MODE_PRIVATE);
    init();
  }

  private void init() {
    logisticsLevel = prefs.getInt("logistics_level", 0);
    councilLevel = prefs.getInt("council_level", 0);
    disciplineLevel = prefs.getInt("discipline_level", 0);
    textPaint.setFakeBoldText(true);
    seedChapters();
    loadArt();
    sendHud();
  }

  public void setCallbacks(Callbacks callbacks) {
    this.callbacks = callbacks;
    sendHud();
  }

  public void setAudio(GameAudio audio) {
    this.audio = audio;
  }

  public void resumeView() {
    paused = false;
  }

  public void pauseView() {
    paused = true;
  }

  public void pauseBattle() {
    paused = true;
  }

  public void resumeBattle() {
    paused = false;
  }

  public void returnToMenu() {
    battleActive = false;
    ended = false;
    menuMode = true;
    selectedNode = null;
    packets.clear();
    if (callbacks != null) {
      callbacks.onMenuMusicNeeded(true);
    }
    sendHud();
  }

  public void startChapter(int chapterIndex) {
    currentChapterIndex = chapterIndex;
    Chapter chapter = chapters.get(Math.max(0, Math.min(chapterIndex - 1, chapters.size() - 1)));
    currentChapter = chapter;
    nodes.clear();
    links.clear();
    packets.clear();
    for (Node seed : chapter.seeds) {
      Node node = new Node();
      node.id = seed.id;
      node.name = seed.name;
      node.type = seed.type;
      node.unitType = seed.unitType;
      node.x = seed.x;
      node.y = seed.y;
      node.owner = seed.owner;
      node.troops = seed.troops;
      node.growthRate = seed.growthRate;
      node.links = seed.links;
      node.capital = seed.capital;
      nodes.add(node);
    }
    for (Link seed : chapter.seedLinks) {
      Link link = new Link();
      link.a = seed.a;
      link.b = seed.b;
      link.bridge = seed.bridge;
      link.broken = seed.broken;
      links.add(link);
    }
    selectedNode = null;
    growthTick = 0f;
    aiTick = 1.6f;
    scoutRevealTimer = 0f;
    commandPoints = 8f + councilLevel * 1.5f;
    commandIncome = 0f;
    battleActive = true;
    paused = false;
    menuMode = false;
    ended = false;
    selectedAbility = Ability.NONE;
    alertText = "Break the enemy capital by severing the road network.";
    computeVisibility();
    computeSupply();
    if (callbacks != null) {
      callbacks.onMenuMusicNeeded(false);
    }
    sendHud();
  }

  public boolean startNextChapter() {
    if (currentChapterIndex < chapters.size()) {
      startChapter(currentChapterIndex + 1);
      return true;
    }
    returnToMenu();
    return false;
  }

  public void retryCurrentChapter() {
    startChapter(currentChapterIndex);
  }

  public String toggleSendRatioLabel() {
    if (sendRatio < 0.4f) {
      sendRatio = 0.6f;
    } else if (sendRatio < 0.9f) {
      sendRatio = 1f;
    } else {
      sendRatio = 0.35f;
    }
    return getRatioLabel();
  }

  public void setSelectedAbility(Ability ability) {
    selectedAbility = selectedAbility == ability ? Ability.NONE : ability;
    if (audio != null) {
      audio.playClick();
    }
    sendHud();
  }

  public String getUpgradeLabel(UpgradeBranch branch) {
    int level = branch == UpgradeBranch.LOGISTICS ? logisticsLevel : branch == UpgradeBranch.WAR_COUNCIL ? councilLevel : disciplineLevel;
    int cost = 3 + level * 2;
    return "Lv " + level + "  Cost " + cost;
  }

  public void buyUpgrade(UpgradeBranch branch) {
    int level = branch == UpgradeBranch.LOGISTICS ? logisticsLevel : branch == UpgradeBranch.WAR_COUNCIL ? councilLevel : disciplineLevel;
    int stars = prefs.getInt("stars", 0);
    int cost = 3 + level * 2;
    if (stars < cost) {
      alertText = "Not enough campaign stars.";
      sendHud();
      return;
    }
    stars -= cost;
    if (branch == UpgradeBranch.LOGISTICS) {
      logisticsLevel++;
    } else if (branch == UpgradeBranch.WAR_COUNCIL) {
      councilLevel++;
    } else {
      disciplineLevel++;
    }
    prefs.edit()
        .putInt("stars", stars)
        .putInt("logistics_level", logisticsLevel)
        .putInt("council_level", councilLevel)
        .putInt("discipline_level", disciplineLevel)
        .apply();
    alertText = "Realm doctrine strengthened.";
    if (audio != null) {
      audio.playCapture();
    }
    sendHud();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    attached = true;
    lastFrameNs = System.nanoTime();
    handler.post(ticker);
  }

  @Override
  protected void onDetachedFromWindow() {
    attached = false;
    handler.removeCallbacks(ticker);
    super.onDetachedFromWindow();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() != MotionEvent.ACTION_UP || !battleActive || paused || ended) {
      return true;
    }
    Node tapped = findNodeAt(event.getX(), event.getY());
    if (tapped == null) {
      selectedNode = null;
      selectedAbility = Ability.NONE;
      sendHud();
      return true;
    }
    if (selectedAbility != Ability.NONE && handleAbility(tapped)) {
      selectedAbility = Ability.NONE;
      sendHud();
      return true;
    }
    if (tapped.owner == 1) {
      if (selectedNode != null && selectedNode.owner == 1 && selectedNode.id != tapped.id && areLinked(selectedNode.id, tapped.id)) {
        dispatchTroops(selectedNode, tapped, sendRatio);
      } else {
        selectedNode = tapped;
        alertText = "Selected " + tapped.name + ".";
        if (audio != null) {
          audio.playClick();
        }
      }
    } else if (selectedNode != null && selectedNode.owner == 1 && areLinked(selectedNode.id, tapped.id)) {
      dispatchTroops(selectedNode, tapped, sendRatio);
    } else {
      selectedNode = null;
      alertText = "Capture roads, farms, and forts before the capital.";
    }
    sendHud();
    return true;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(color(R.color.cst_bg_main));
    paint.setColor(color(R.color.cst_bg_alt));
    canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
    float pad = dp(10f);
    boardRect.set(pad, pad, getWidth() - pad, getHeight() - pad);
    drawTerrainBackdrop(canvas);
    drawLinks(canvas);
    drawPackets(canvas);
    drawNodes(canvas);
    if (!battleActive) {
      drawIdleBoard(canvas);
    }
  }

  private void update(float dt) {
    if (!battleActive || paused || ended) {
      return;
    }
    growthTick += dt;
    aiTick -= dt;
    if (scoutRevealTimer > 0f) {
      scoutRevealTimer = Math.max(0f, scoutRevealTimer - dt);
    }
    for (int i = 0; i < links.size(); i++) {
      if (links.get(i).pulse > 0f) {
        links.get(i).pulse = Math.max(0f, links.get(i).pulse - dt);
      }
    }
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      node.shieldTimer = Math.max(0f, node.shieldTimer - dt);
      node.rallyTimer = Math.max(0f, node.rallyTimer - dt);
    }
    if (growthTick >= 0.25f) {
      growthTick = 0f;
      computeSupply();
      applyGrowth(0.25f);
      computeVisibility();
    }
    updatePackets(dt);
    if (aiTick <= 0f) {
      aiTick = 1.4f - logisticsLevel * 0.04f;
      runEnemyStep();
    }
    checkEndState();
    sendHud();
  }

  private void applyGrowth(float dt) {
    float commandGain = 0f;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      float growth = node.growthRate;
      if (node.owner == 1) {
        growth += logisticsLevel * 0.08f;
      }
      if (!node.connected) {
        growth *= 0.55f;
      }
      if (node.owner != 0) {
        node.troops = Math.min(node.troops + growth * dt, node.capital ? 65f : 42f);
      }
      if (node.owner == 1) {
        commandGain += 0.08f;
        if (node.type == NodeType.MINE) {
          commandGain += 0.05f;
        }
        if (node.type == NodeType.WATCHTOWER) {
          commandGain += 0.03f;
        }
      }
    }
    commandIncome = commandGain * 4f;
    commandPoints = Math.min(18f, commandPoints + commandGain + councilLevel * 0.02f);
  }

  private void updatePackets(float dt) {
    for (int i = packets.size() - 1; i >= 0; i--) {
      Packet packet = packets.get(i);
      packet.progress += packet.speed * dt;
      if (packet.progress >= 1f) {
        resolveArrival(packet);
        packets.remove(i);
      }
    }
  }

  private void resolveArrival(Packet packet) {
    Node target = getNode(packet.toId);
    if (target == null) {
      return;
    }
    if (target.owner == packet.owner) {
      target.troops = Math.min(target.troops + packet.amount, target.capital ? 70f : 48f);
      return;
    }
    float defense = target.troops * defenseMultiplier(target.unitType, packet.unitType);
    if (target.shieldTimer > 0f) {
      defense *= 1.22f + disciplineLevel * 0.05f;
    }
    float attack = packet.amount * attackMultiplier(packet.unitType, target.unitType);
    if (attack > defense) {
      target.owner = packet.owner;
      target.unitType = packet.unitType;
      target.troops = Math.max(2f, attack - defense);
      target.shieldTimer = 0f;
      if (target.type == NodeType.WATCHTOWER) {
        scoutRevealTimer = Math.max(scoutRevealTimer, packet.owner == 1 ? 4f : 0f);
      }
      alertText = target.name + " falls to " + ownerLabel(packet.owner) + ".";
      if (audio != null) {
        audio.playCapture();
      }
    } else {
      target.troops = Math.max(1f, defense - attack);
      if (audio != null) {
        audio.playBattle();
      }
    }
    if (target.capital && target.owner == 1) {
      alertText = "The crown banner rises over the capital.";
    }
  }

  private void runEnemyStep() {
    ArrayList<Node> enemyNodes = new ArrayList<>();
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).owner == -1 && nodes.get(i).troops > 8f) {
        enemyNodes.add(nodes.get(i));
      }
    }
    for (int i = 0; i < enemyNodes.size(); i++) {
      Node source = enemyNodes.get(i);
      Node bestTarget = null;
      float bestScore = -999f;
      for (int j = 0; j < source.links.length; j++) {
        Node target = getNode(source.links[j]);
        if (target == null || target.owner == -1) {
          continue;
        }
        float score = (target.capital ? 24f : 0f) + (target.owner == 1 ? 8f : 4f) + (22f - target.troops);
        if (target.type == NodeType.FARM) {
          score += 5f;
        }
        if (target.type == NodeType.MINE) {
          score += 4f;
        }
        if (source.connected) {
          score += 2f;
        }
        if (target.shieldTimer > 0f) {
          score -= 6f;
        }
        if (score > bestScore) {
          bestScore = score;
          bestTarget = target;
        }
      }
      if (bestTarget != null) {
        dispatchTroops(source, bestTarget, source.troops > 20f ? 0.55f : 0.4f);
      }
    }
  }

  private boolean handleAbility(Node tapped) {
    if (selectedAbility == Ability.RALLY) {
      if (tapped.owner != 1 || commandPoints < 3f) {
        alertText = "Rally needs a friendly city and 3 command.";
        return true;
      }
      tapped.rallyTimer = 10f;
      commandPoints -= 3f;
      alertText = "Rally drums echo from " + tapped.name + ".";
      if (audio != null) {
        audio.playClick();
      }
      return true;
    }
    if (selectedAbility == Ability.SHIELD_WALL) {
      if (tapped.owner != 1 || commandPoints < 3f) {
        alertText = "Shield Wall needs a friendly node and 3 command.";
        return true;
      }
      tapped.shieldTimer = 12f + disciplineLevel * 1.4f;
      commandPoints -= 3f;
      alertText = tapped.name + " braces behind shield walls.";
      if (audio != null) {
        audio.playClick();
      }
      return true;
    }
    if (selectedAbility == Ability.SCOUT_FLARE) {
      if (commandPoints < 2f) {
        alertText = "Scout Flare needs 2 command.";
        return true;
      }
      scoutRevealTimer = 10f + councilLevel;
      commandPoints -= 2f;
      alertText = "Signal flares reveal every rival road.";
      if (audio != null) {
        audio.playClick();
      }
      return true;
    }
    if (selectedAbility == Ability.REPAIR_BRIDGE) {
      if (commandPoints < 3f) {
        alertText = "Bridge Repair needs 3 command.";
        return true;
      }
      Link broken = findBrokenLinkForNode(tapped.id);
      if (broken == null) {
        alertText = "No broken bridge route is linked to this node.";
        return true;
      }
      broken.broken = false;
      broken.pulse = 1f;
      commandPoints -= 3f;
      alertText = "Bridge crews reopen the crossing.";
      if (audio != null) {
        audio.playRepair();
      }
      return true;
    }
    return false;
  }

  private void dispatchTroops(Node from, Node to, float ratio) {
    if (from == null || to == null || from.owner == 0 || from.troops < 2f) {
      return;
    }
    Link link = getLink(from.id, to.id);
    if (link == null || link.broken) {
      alertText = "That route is broken.";
      return;
    }
    float amount = Math.max(1f, (float) Math.floor(from.troops * ratio));
    if (amount >= from.troops) {
      amount = from.troops - 1f;
    }
    if (amount < 1f) {
      return;
    }
    from.troops -= amount;
    Packet packet = new Packet();
    packet.owner = from.owner;
    packet.unitType = from.unitType;
    packet.amount = amount;
    packet.fromId = from.id;
    packet.toId = to.id;
    packet.speed = baseSpeed(from.unitType) * (from.connected ? 1f : 0.78f) * (from.rallyTimer > 0f ? 1.4f : 1f) * (1f + logisticsLevel * 0.04f);
    packet.allied = from.owner == 1;
    packets.add(packet);
    if (from.owner == 1) {
      alertText = "Marching on " + to.name + ".";
      if (audio != null) {
        audio.playDispatch();
      }
    }
  }

  private void computeSupply() {
    markConnected(1);
    markConnected(-1);
  }

  private void markConnected(int owner) {
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).owner == owner) {
        nodes.get(i).connected = false;
      }
    }
    ArrayDeque<Node> queue = new ArrayDeque<>();
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.owner == owner && node.capital) {
        node.connected = true;
        queue.add(node);
      }
    }
    while (!queue.isEmpty()) {
      Node current = queue.removeFirst();
      for (int i = 0; i < current.links.length; i++) {
        Node next = getNode(current.links[i]);
        Link link = getLink(current.id, current.links[i]);
        if (next != null && link != null && !link.broken && next.owner == owner && !next.connected) {
          next.connected = true;
          queue.add(next);
        }
      }
    }
  }

  private void computeVisibility() {
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      node.visible = node.owner == 1 || scoutRevealTimer > 0f;
    }
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.owner != 1) {
        continue;
      }
      node.visible = true;
      for (int j = 0; j < node.links.length; j++) {
        Node linked = getNode(node.links[j]);
        if (linked != null) {
          linked.visible = true;
        }
      }
      if (node.type == NodeType.WATCHTOWER) {
        for (int j = 0; j < node.links.length; j++) {
          Node linked = getNode(node.links[j]);
          if (linked == null) {
            continue;
          }
          for (int k = 0; k < linked.links.length; k++) {
            Node second = getNode(linked.links[k]);
            if (second != null) {
              second.visible = true;
            }
          }
        }
      }
    }
  }

  private void checkEndState() {
    Node playerCapital = null;
    Node enemyCapital = null;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.capital && node.owner == 1) {
        playerCapital = node;
      }
      if (node.capital && node.owner == -1) {
        enemyCapital = node;
      }
    }
    if (enemyCapital == null && !ended) {
      ended = true;
      battleActive = false;
      int stars = 2 + (ownedCount() >= nodes.size() * 0.7f ? 1 : 0);
      prefs.edit().putInt("stars", prefs.getInt("stars", 0) + stars).apply();
      if (audio != null) {
        audio.playWin();
      }
      if (callbacks != null) {
        callbacks.onBattleEnded(new ResultSnapshot(true, "Province secured with " + stars + " campaign stars."));
      }
    } else if (playerCapital == null && !ended) {
      ended = true;
      battleActive = false;
      if (audio != null) {
        audio.playFail();
      }
      if (callbacks != null) {
        callbacks.onBattleEnded(new ResultSnapshot(false, "The crown city has fallen. Retake the roads and try again."));
      }
    }
  }

  private void drawIdleBoard(Canvas canvas) {
    paint.setColor(adjustAlpha(color(R.color.cst_panel_bg), 0.78f));
    tempRect.set(boardRect.left + dp(26f), boardRect.top + dp(26f), boardRect.right - dp(26f), boardRect.bottom - dp(26f));
    canvas.drawRoundRect(tempRect, dp(20f), dp(20f), paint);
    textPaint.setColor(color(R.color.cst_text_primary));
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(dp(20f));
    canvas.drawText(getResources().getString(R.string.label_idle_board), tempRect.centerX(), tempRect.centerY() - dp(8f), textPaint);
    textPaint.setTextSize(dp(12f));
    textPaint.setColor(color(R.color.cst_text_secondary));
    canvas.drawText(getResources().getString(R.string.label_idle_hint), tempRect.centerX(), tempRect.centerY() + dp(18f), textPaint);
  }

  private void drawTerrainBackdrop(Canvas canvas) {
    paint.setColor(color(R.color.cst_game_map_base));
    canvas.drawRoundRect(boardRect, dp(24f), dp(24f), paint);
    float stripe = boardRect.width() / 7f;
    for (int i = 0; i < 7; i++) {
      paint.setColor(i % 2 == 0 ? adjustAlpha(color(R.color.cst_game_field_light), 0.22f) : adjustAlpha(color(R.color.cst_game_field_dark), 0.18f));
      canvas.drawRect(boardRect.left + stripe * i, boardRect.top, boardRect.left + stripe * (i + 1), boardRect.bottom, paint);
    }
    paint.setColor(adjustAlpha(color(R.color.cst_warning), 0.18f));
    canvas.drawCircle(boardRect.left + boardRect.width() * 0.22f, boardRect.top + boardRect.height() * 0.22f, dp(54f), paint);
    paint.setColor(adjustAlpha(color(R.color.cst_accent_2), 0.14f));
    canvas.drawCircle(boardRect.right - boardRect.width() * 0.18f, boardRect.bottom - boardRect.height() * 0.24f, dp(72f), paint);
  }

  private void drawLinks(Canvas canvas) {
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(6f));
    for (int i = 0; i < links.size(); i++) {
      Link link = links.get(i);
      Node a = getNode(link.a);
      Node b = getNode(link.b);
      if (a == null || b == null) {
        continue;
      }
      float ax = nodeX(a.x);
      float ay = nodeY(a.y);
      float bx = nodeX(b.x);
      float by = nodeY(b.y);
      paint.setColor(link.broken ? color(R.color.cst_danger) : color(R.color.cst_game_road));
      if (link.bridge) {
        paint.setStrokeWidth(dp(9f));
      } else {
        paint.setStrokeWidth(dp(6f));
      }
      canvas.drawLine(ax, ay, bx, by, paint);
      if (link.bridge) {
        paint.setColor(adjustAlpha(color(R.color.cst_text_primary), 0.7f + link.pulse * 0.3f));
        paint.setStrokeWidth(dp(2f));
        canvas.drawLine(ax, ay, bx, by, paint);
      }
    }
    paint.setStyle(Paint.Style.FILL);
  }

  private void drawPackets(Canvas canvas) {
    for (int i = 0; i < packets.size(); i++) {
      Packet packet = packets.get(i);
      Node from = getNode(packet.fromId);
      Node to = getNode(packet.toId);
      if (from == null || to == null) {
        continue;
      }
      float x = lerp(nodeX(from.x), nodeX(to.x), packet.progress);
      float y = lerp(nodeY(from.y), nodeY(to.y), packet.progress);
      float size = dp(16f);
      drawPacketBanner(canvas, x, y, size, packet.owner, packet.unitType);
    }
  }

  private void drawPacketBanner(Canvas canvas, float x, float y, float size, int owner, UnitType type) {
    paint.setColor(owner == 1 ? color(R.color.cst_accent_2) : color(R.color.cst_danger));
    path.reset();
    path.moveTo(x - size * 0.45f, y + size * 0.36f);
    path.lineTo(x - size * 0.18f, y - size * 0.42f);
    path.lineTo(x + size * 0.42f, y - size * 0.12f);
    path.lineTo(x - size * 0.02f, y + size * 0.10f);
    path.lineTo(x + size * 0.36f, y + size * 0.42f);
    path.close();
    canvas.drawPath(path, paint);
    paint.setColor(color(R.color.cst_text_primary));
    if (type == UnitType.SPEAR) {
      canvas.drawRect(x - size * 0.08f, y - size * 0.48f, x + size * 0.06f, y + size * 0.48f, paint);
    } else if (type == UnitType.ARCHER) {
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(2f));
      tempRect.set(x - size * 0.28f, y - size * 0.22f, x + size * 0.28f, y + size * 0.22f);
      canvas.drawArc(tempRect, 42f, 96f, false, paint);
      canvas.drawLine(x - size * 0.06f, y - size * 0.18f, x - size * 0.06f, y + size * 0.18f, paint);
      paint.setStyle(Paint.Style.FILL);
    } else if (type == UnitType.CAVALRY) {
      canvas.drawCircle(x, y, size * 0.16f, paint);
      canvas.drawCircle(x + size * 0.22f, y + size * 0.12f, size * 0.1f, paint);
    } else {
      canvas.drawCircle(x, y, size * 0.14f, paint);
    }
  }

  private void drawNodes(Canvas canvas) {
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      float x = nodeX(node.x);
      float y = nodeY(node.y);
      float radius = node.capital ? dp(36f) : node.type == NodeType.FORT ? dp(28f) : dp(24f);
      paint.setColor(node.owner == 1 ? adjustAlpha(color(R.color.cst_success), 0.22f) : node.owner == -1 ? adjustAlpha(color(R.color.cst_danger), 0.22f) : adjustAlpha(color(R.color.cst_warning), 0.18f));
      canvas.drawCircle(x, y, radius * 1.24f, paint);
      drawNodeArt(canvas, node, x, y, radius * 2.1f);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(node.selected || node == selectedNode ? 3f : 2f));
      paint.setColor(node == selectedNode ? color(R.color.cst_warning) : color(R.color.cst_panel_stroke));
      canvas.drawCircle(x, y, radius * 1.02f, paint);
      paint.setStyle(Paint.Style.FILL);
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setTextSize(dp(10f));
      textPaint.setColor(color(R.color.cst_text_primary));
      canvas.drawText(node.name, x, y + radius + dp(13f), textPaint);
      textPaint.setTextSize(dp(12f));
      textPaint.setFakeBoldText(true);
      String troopText = node.owner == -1 && !node.visible ? "?" : String.valueOf(Math.round(node.troops));
      canvas.drawText(troopText, x, y + dp(4f), textPaint);
      if (!node.connected && node.owner != 0) {
        textPaint.setTextSize(dp(8f));
        textPaint.setColor(color(R.color.cst_warning));
        canvas.drawText(getResources().getString(R.string.label_cut_off), x, y - radius - dp(4f), textPaint);
      } else if (node.shieldTimer > 0f) {
        textPaint.setTextSize(dp(8f));
        textPaint.setColor(color(R.color.cst_accent));
        canvas.drawText(getResources().getString(R.string.label_shielded), x, y - radius - dp(4f), textPaint);
      }
    }
  }

  private void drawNodeArt(Canvas canvas, Node node, float x, float y, float size) {
    Bitmap bitmap = art.get(assetKeyFor(node));
    if (bitmap != null) {
      tempRect.set(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f);
      canvas.drawBitmap(bitmap, null, tempRect, null);
    } else {
      paint.setColor(node.owner == 1 ? color(R.color.cst_accent_2) : node.owner == -1 ? color(R.color.cst_danger) : color(R.color.cst_game_wood));
      canvas.drawCircle(x, y, size * 0.42f, paint);
    }
    if (node.type == NodeType.WATCHTOWER) {
      paint.setColor(color(R.color.cst_text_primary));
      canvas.drawRect(x - size * 0.08f, y - size * 0.42f, x + size * 0.08f, y + size * 0.28f, paint);
      canvas.drawCircle(x, y - size * 0.42f, size * 0.12f, paint);
    }
  }

  private void sendHud() {
    if (callbacks == null) {
      return;
    }
    int owned = ownedCount();
    int enemy = enemyCount();
    String province = currentChapter == null ? getResources().getString(R.string.label_menu_ready) : currentChapter.provinceName;
    String objective = currentChapter == null ? getResources().getString(R.string.label_objective_idle) : currentChapter.objective;
    String doctrine = "Log " + logisticsLevel + "  Council " + councilLevel + "  Iron " + disciplineLevel;
    callbacks.onHudChanged(new HudSnapshot(
        province,
        String.valueOf(owned),
        String.valueOf(enemy),
        String.format(Locale.US, "+%.1f", totalIncome()),
        String.format(Locale.US, "%.1f", commandPoints),
        objective,
        doctrine,
        alertText,
        "Rally " + abilityCostLabel(3f),
        "Shield " + abilityCostLabel(3f),
        "Scout " + abilityCostLabel(2f),
        "Repair " + abilityCostLabel(3f)
    ));
  }

  private String abilityCostLabel(float cost) {
    return "(" + Math.round(cost) + ")";
  }

  private int ownedCount() {
    int count = 0;
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).owner == 1) {
        count++;
      }
    }
    return count;
  }

  private int enemyCount() {
    int count = 0;
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).owner == -1) {
        count++;
      }
    }
    return count;
  }

  private float totalIncome() {
    float total = 0f;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.owner == 1) {
        total += node.connected ? node.growthRate : node.growthRate * 0.55f;
      }
    }
    return total;
  }

  private Node findNodeAt(float sx, float sy) {
    for (int i = nodes.size() - 1; i >= 0; i--) {
      Node node = nodes.get(i);
      float radius = node.capital ? dp(38f) : node.type == NodeType.FORT ? dp(30f) : dp(26f);
      float dx = sx - nodeX(node.x);
      float dy = sy - nodeY(node.y);
      if (dx * dx + dy * dy <= radius * radius) {
        return node;
      }
    }
    return null;
  }

  private Link findBrokenLinkForNode(int nodeId) {
    for (int i = 0; i < links.size(); i++) {
      Link link = links.get(i);
      if (link.broken && (link.a == nodeId || link.b == nodeId)) {
        return link;
      }
    }
    return null;
  }

  private boolean areLinked(int fromId, int toId) {
    return getLink(fromId, toId) != null;
  }

  private Link getLink(int fromId, int toId) {
    for (int i = 0; i < links.size(); i++) {
      Link link = links.get(i);
      if ((link.a == fromId && link.b == toId) || (link.a == toId && link.b == fromId)) {
        return link;
      }
    }
    return null;
  }

  private Node getNode(int id) {
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).id == id) {
        return nodes.get(i);
      }
    }
    return null;
  }

  private float attackMultiplier(UnitType attacker, UnitType defender) {
    if (attacker == UnitType.CAVALRY && defender == UnitType.ARCHER) {
      return 1.32f;
    }
    if (attacker == UnitType.SPEAR && defender == UnitType.CAVALRY) {
      return 1.34f;
    }
    if (attacker == UnitType.ARCHER && defender == UnitType.SPEAR) {
      return 1.24f;
    }
    if (attacker == UnitType.LEVY && defender == UnitType.ARCHER) {
      return 0.92f;
    }
    return 1f + disciplineLevel * 0.02f;
  }

  private float defenseMultiplier(UnitType defender, UnitType attacker) {
    if (defender == UnitType.SPEAR && attacker == UnitType.CAVALRY) {
      return 1.18f;
    }
    if (defender == UnitType.ARCHER && attacker == UnitType.SPEAR) {
      return 0.92f;
    }
    if (defender == UnitType.CAVALRY && attacker == UnitType.ARCHER) {
      return 0.9f;
    }
    return 1f;
  }

  private float baseSpeed(UnitType type) {
    if (type == UnitType.CAVALRY) {
      return 0.34f;
    }
    if (type == UnitType.ARCHER) {
      return 0.26f;
    }
    if (type == UnitType.SPEAR) {
      return 0.24f;
    }
    return 0.22f;
  }

  private String ownerLabel(int owner) {
    return owner == 1 ? "the crown" : owner == -1 ? "the rival league" : "neutral hands";
  }

  private float nodeX(float normalizedX) {
    return boardRect.left + boardRect.width() * normalizedX;
  }

  private float nodeY(float normalizedY) {
    return boardRect.top + boardRect.height() * normalizedY;
  }

  private float lerp(float from, float to, float t) {
    return from + (to - from) * t;
  }

  private int color(int resId) {
    return ContextCompat.getColor(getContext(), resId);
  }

  private int adjustAlpha(int color, float alpha) {
    int a = Math.min(255, Math.max(0, Math.round(255f * alpha)));
    return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
  }

  private float dp(float value) {
    return getResources().getDisplayMetrics().density * value;
  }

  private String getRatioLabel() {
    return sendRatio >= 0.99f ? "100%" : sendRatio > 0.5f ? "60%" : sendRatio > 0.34f ? "50%" : "35%";
  }

  private String assetKeyFor(Node node) {
    if (node.type == NodeType.CAPITAL) {
      return "game_art/kenney_tiny_town/assets/Tiles/tile_0121.png";
    }
    if (node.type == NodeType.CITY) {
      return "game_art/kenney_tiny_town/assets/Tiles/tile_0120.png";
    }
    if (node.type == NodeType.FARM) {
      return "game_art/kenney_tiny_town/assets/Tiles/tile_0072.png";
    }
    if (node.type == NodeType.MINE) {
      return "game_art/kenney_tiny_town/assets/Tiles/tile_0038.png";
    }
    if (node.type == NodeType.BARRACKS) {
      return "game_art/kenney_tiny_town/assets/Tiles/tile_0117.png";
    }
    if (node.type == NodeType.WATCHTOWER) {
      return "game_art/kenney_tiny_town/assets/Tiles/tile_0108.png";
    }
    return "game_art/kenney_tiny_town/assets/Tiles/tile_0096.png";
  }

  private void loadArt() {
    String[] keys = new String[]{
        "game_art/kenney_tiny_town/assets/Tiles/tile_0121.png",
        "game_art/kenney_tiny_town/assets/Tiles/tile_0120.png",
        "game_art/kenney_tiny_town/assets/Tiles/tile_0117.png",
        "game_art/kenney_tiny_town/assets/Tiles/tile_0096.png",
        "game_art/kenney_tiny_town/assets/Tiles/tile_0072.png",
        "game_art/kenney_tiny_town/assets/Tiles/tile_0038.png",
        "game_art/kenney_tiny_town/assets/Tiles/tile_0108.png"
    };
    for (int i = 0; i < keys.length; i++) {
      Bitmap bitmap = loadBitmap(keys[i]);
      if (bitmap != null) {
        art.put(keys[i], bitmap);
      }
    }
  }

  private Bitmap loadBitmap(String assetPath) {
    AssetManager manager = getContext().getAssets();
    InputStream stream = null;
    try {
      stream = manager.open(assetPath);
      Bitmap bitmap = BitmapFactory.decodeStream(stream);
      if (bitmap == null) {
        return null;
      }
      return bitmap.copy(Bitmap.Config.ARGB_8888, false);
    } catch (IOException ignored) {
      return null;
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private void seedChapters() {
    chapters.clear();
    chapters.add(buildChapter(1, "Northmarch Vale", "Seize the rebel capital after securing two farms.",
        new float[][]{
            {0.10f, 0.54f}, {0.20f, 0.32f}, {0.21f, 0.72f}, {0.36f, 0.48f}, {0.52f, 0.28f},
            {0.53f, 0.72f}, {0.70f, 0.48f}, {0.84f, 0.32f}, {0.86f, 0.68f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.WATCHTOWER, NodeType.CITY, NodeType.MINE, NodeType.FARM, NodeType.FORT, NodeType.BARRACKS, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.ARCHER, UnitType.SPEAR, UnitType.SPEAR, UnitType.LEVY, UnitType.SPEAR, UnitType.ARCHER, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, 0, -1, -1, -1},
        new float[]{18, 10, 9, 12, 10, 10, 12, 11, 18},
        new float[]{2.5f, 1.8f, 1.4f, 1.8f, 1.5f, 1.8f, 1.5f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,3},{3,4},{3,5},{4,6},{5,6},{6,7},{6,8},{7,8}}));
    chapters.add(buildChapter(2, "Bridgewatch Reaches", "Repair the bridge route and crush the river citadel.",
        new float[][]{
            {0.08f, 0.50f}, {0.20f, 0.24f}, {0.24f, 0.76f}, {0.40f, 0.50f}, {0.56f, 0.24f},
            {0.56f, 0.76f}, {0.70f, 0.50f}, {0.84f, 0.24f}, {0.88f, 0.76f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.FORT, NodeType.WATCHTOWER, NodeType.MINE, NodeType.BARRACKS, NodeType.CITY, NodeType.FORT, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.ARCHER, UnitType.SPEAR, UnitType.ARCHER, UnitType.LEVY, UnitType.SPEAR, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, -1, -1, -1, -1, -1},
        new float[]{18, 9, 10, 10, 10, 11, 12, 12, 20},
        new float[]{2.5f, 1.8f, 1.4f, 1.4f, 1.6f, 1.6f, 1.8f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,3},{3,4},{3,5},{4,6},{5,6},{6,7},{6,8},{7,8}}));
    chapters.add(buildChapter(3, "Oakwall Corridor", "Use watchtowers to expose the forest road ambush.",
        new float[][]{
            {0.08f, 0.52f}, {0.18f, 0.26f}, {0.22f, 0.74f}, {0.34f, 0.34f}, {0.38f, 0.66f},
            {0.52f, 0.50f}, {0.66f, 0.32f}, {0.68f, 0.70f}, {0.84f, 0.52f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.WATCHTOWER, NodeType.FARM, NodeType.FORT, NodeType.MINE, NodeType.CITY, NodeType.BARRACKS, NodeType.FORT, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.ARCHER, UnitType.LEVY, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.SPEAR, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, -1, -1, -1, -1},
        new float[]{18, 10, 10, 11, 10, 13, 12, 13, 20},
        new float[]{2.5f, 1.4f, 1.8f, 1.4f, 1.6f, 1.8f, 1.6f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,4},{3,5},{4,5},{5,6},{5,7},{6,8},{7,8}}));
    chapters.add(buildChapter(4, "Twinroad Bastions", "Split forces and hold both roads before the countercharge.",
        new float[][]{
            {0.10f, 0.48f}, {0.22f, 0.22f}, {0.22f, 0.76f}, {0.38f, 0.22f}, {0.38f, 0.76f},
            {0.54f, 0.48f}, {0.72f, 0.22f}, {0.72f, 0.76f}, {0.88f, 0.48f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.FARM, NodeType.FORT, NodeType.FORT, NodeType.CITY, NodeType.BARRACKS, NodeType.MINE, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.CAVALRY, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, -1, -1, -1, -1},
        new float[]{18, 9, 9, 11, 11, 13, 12, 11, 20},
        new float[]{2.5f, 1.8f, 1.8f, 1.4f, 1.4f, 1.8f, 1.6f, 1.6f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,4},{3,5},{4,5},{5,6},{5,7},{6,8},{7,8}}));
    chapters.add(buildChapter(5, "Ironford Siege", "Advance through the ford and break the fortress ring.",
        new float[][]{
            {0.10f, 0.50f}, {0.22f, 0.30f}, {0.22f, 0.70f}, {0.38f, 0.50f}, {0.54f, 0.28f},
            {0.54f, 0.72f}, {0.70f, 0.50f}, {0.84f, 0.30f}, {0.84f, 0.70f}, {0.92f, 0.50f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.FORT, NodeType.WATCHTOWER, NodeType.BARRACKS, NodeType.MINE, NodeType.CITY, NodeType.FORT, NodeType.FORT, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.ARCHER, UnitType.ARCHER, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.CAVALRY, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, -1, -1, -1, -1, -1, -1},
        new float[]{18, 9, 11, 10, 12, 11, 12, 12, 14, 21},
        new float[]{2.5f, 1.8f, 1.4f, 1.4f, 1.6f, 1.6f, 1.8f, 1.4f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,3},{3,4},{3,5},{4,6},{5,6},{6,7},{6,8},{7,9},{8,9}}));
    chapters.add(buildChapter(6, "Goldwind Pass", "Win the mine line and starve the enemy core.",
        new float[][]{
            {0.08f, 0.54f}, {0.18f, 0.24f}, {0.22f, 0.76f}, {0.34f, 0.52f}, {0.46f, 0.24f},
            {0.46f, 0.76f}, {0.62f, 0.50f}, {0.76f, 0.24f}, {0.78f, 0.76f}, {0.92f, 0.50f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.FORT, NodeType.CITY, NodeType.MINE, NodeType.MINE, NodeType.WATCHTOWER, NodeType.BARRACKS, NodeType.FORT, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.ARCHER, UnitType.SPEAR, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, -1, -1, -1, -1, -1},
        new float[]{19, 9, 10, 12, 10, 11, 10, 12, 13, 21},
        new float[]{2.5f, 1.8f, 1.4f, 1.8f, 1.6f, 1.6f, 1.4f, 1.6f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,3},{3,4},{3,5},{4,6},{5,6},{6,7},{6,8},{7,9},{8,9}}));
    chapters.add(buildChapter(7, "Ravenpost Downs", "Outrun cavalry raids and secure the barracks chain.",
        new float[][]{
            {0.08f, 0.50f}, {0.18f, 0.28f}, {0.18f, 0.72f}, {0.32f, 0.50f}, {0.48f, 0.26f},
            {0.48f, 0.74f}, {0.64f, 0.50f}, {0.78f, 0.22f}, {0.78f, 0.78f}, {0.92f, 0.50f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.FARM, NodeType.CITY, NodeType.BARRACKS, NodeType.WATCHTOWER, NodeType.CITY, NodeType.FORT, NodeType.MINE, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.ARCHER, UnitType.ARCHER, UnitType.CAVALRY, UnitType.SPEAR, UnitType.SPEAR, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, -1, -1, -1, -1, -1},
        new float[]{18, 9, 9, 12, 12, 10, 12, 12, 11, 21},
        new float[]{2.5f, 1.8f, 1.8f, 1.8f, 1.6f, 1.4f, 1.8f, 1.4f, 1.6f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,3},{3,4},{3,5},{4,6},{5,6},{6,7},{6,8},{7,9},{8,9}}));
    chapters.add(buildChapter(8, "Southgate Circuit", "Circle the roads and break the double-fort line.",
        new float[][]{
            {0.08f, 0.50f}, {0.20f, 0.22f}, {0.20f, 0.78f}, {0.36f, 0.22f}, {0.36f, 0.78f},
            {0.52f, 0.50f}, {0.68f, 0.22f}, {0.68f, 0.78f}, {0.84f, 0.50f}, {0.94f, 0.50f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.MINE, NodeType.FORT, NodeType.FORT, NodeType.WATCHTOWER, NodeType.BARRACKS, NodeType.CITY, NodeType.FORT, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.ARCHER, UnitType.CAVALRY, UnitType.SPEAR, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, -1, -1, -1, -1, -1},
        new float[]{19, 9, 10, 12, 12, 10, 12, 12, 13, 22},
        new float[]{2.5f, 1.8f, 1.6f, 1.4f, 1.4f, 1.4f, 1.6f, 1.8f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,4},{3,5},{4,5},{5,6},{5,7},{6,8},{7,8},{8,9}}));
    chapters.add(buildChapter(9, "Kingroad Expanse", "Push across the grand road and isolate the final provinces.",
        new float[][]{
            {0.06f, 0.48f}, {0.16f, 0.24f}, {0.16f, 0.72f}, {0.30f, 0.50f}, {0.44f, 0.24f},
            {0.44f, 0.72f}, {0.58f, 0.50f}, {0.72f, 0.24f}, {0.72f, 0.72f}, {0.86f, 0.50f}, {0.95f, 0.50f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.FARM, NodeType.CITY, NodeType.MINE, NodeType.BARRACKS, NodeType.WATCHTOWER, NodeType.FORT, NodeType.CITY, NodeType.FORT, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.ARCHER, UnitType.SPEAR, UnitType.CAVALRY, UnitType.SPEAR, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1},
        new float[]{19, 9, 9, 12, 10, 12, 10, 13, 12, 14, 22},
        new float[]{2.5f, 1.8f, 1.8f, 1.8f, 1.6f, 1.6f, 1.4f, 1.4f, 1.8f, 1.4f, 2.5f},
        new int[][]{{0,1},{0,2},{1,3},{2,3},{3,4},{3,5},{4,6},{5,6},{6,7},{6,8},{7,9},{8,9},{9,10}}));
    chapters.add(buildChapter(10, "Crownfire Capital", "Collapse both outer rings and storm the final capital.",
        new float[][]{
            {0.06f, 0.50f}, {0.16f, 0.18f}, {0.16f, 0.82f}, {0.28f, 0.34f}, {0.28f, 0.66f},
            {0.44f, 0.18f}, {0.44f, 0.82f}, {0.58f, 0.34f}, {0.58f, 0.66f}, {0.74f, 0.50f},
            {0.86f, 0.34f}, {0.86f, 0.66f}, {0.95f, 0.50f}
        },
        new NodeType[]{NodeType.CAPITAL, NodeType.FARM, NodeType.MINE, NodeType.FORT, NodeType.FORT, NodeType.BARRACKS, NodeType.WATCHTOWER, NodeType.CITY, NodeType.CITY, NodeType.FORT, NodeType.FORT, NodeType.BARRACKS, NodeType.CAPITAL},
        new UnitType[]{UnitType.LEVY, UnitType.LEVY, UnitType.SPEAR, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.ARCHER, UnitType.SPEAR, UnitType.CAVALRY, UnitType.SPEAR, UnitType.SPEAR, UnitType.ARCHER, UnitType.CAVALRY},
        new int[]{1, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1},
        new float[]{20, 9, 10, 12, 12, 12, 10, 12, 12, 13, 13, 12, 24},
        new float[]{2.5f, 1.8f, 1.6f, 1.4f, 1.4f, 1.6f, 1.4f, 1.8f, 1.8f, 1.4f, 1.4f, 1.6f, 2.7f},
        new int[][]{{0,1},{0,2},{1,3},{2,4},{3,5},{4,6},{5,7},{6,8},{7,9},{8,9},{9,10},{9,11},{10,12},{11,12}}));
    markBridges();
  }

  private Chapter buildChapter(int index, String provinceName, String objective, float[][] positions, NodeType[] types,
                               UnitType[] unitTypes, int[] owners, float[] troops, float[] growth, int[][] linkPairs) {
    Chapter chapter = new Chapter();
    chapter.index = index;
    chapter.provinceName = provinceName;
    chapter.objective = objective;
    chapter.seeds = new Node[positions.length];
    ArrayList<int[]> linkBuckets = new ArrayList<>();
    for (int i = 0; i < positions.length; i++) {
      Node node = new Node();
      node.id = i;
      node.name = defaultName(types[i], i);
      node.type = types[i];
      node.unitType = unitTypes[i];
      node.x = positions[i][0];
      node.y = positions[i][1];
      node.owner = owners[i];
      node.troops = troops[i];
      node.growthRate = growth[i];
      node.capital = types[i] == NodeType.CAPITAL;
      chapter.seeds[i] = node;
      linkBuckets.add(new int[0]);
    }
    chapter.seedLinks = new Link[linkPairs.length];
    for (int i = 0; i < linkPairs.length; i++) {
      Link link = new Link();
      link.a = linkPairs[i][0];
      link.b = linkPairs[i][1];
      chapter.seedLinks[i] = link;
    }
    for (int i = 0; i < chapter.seeds.length; i++) {
      ArrayList<Integer> ids = new ArrayList<>();
      for (int j = 0; j < chapter.seedLinks.length; j++) {
        Link link = chapter.seedLinks[j];
        if (link.a == i) {
          ids.add(link.b);
        } else if (link.b == i) {
          ids.add(link.a);
        }
      }
      chapter.seeds[i].links = new int[ids.size()];
      for (int j = 0; j < ids.size(); j++) {
        chapter.seeds[i].links[j] = ids.get(j);
      }
    }
    return chapter;
  }

  private void markBridges() {
    if (chapters.size() < 2) {
      return;
    }
    setBridge(chapters.get(1), 3, 4, true);
    setBridge(chapters.get(4), 6, 7, false);
    setBridge(chapters.get(5), 3, 5, false);
    setBridge(chapters.get(9), 9, 10, false);
    setBridge(chapters.get(9), 9, 11, true);
  }

  private void setBridge(Chapter chapter, int a, int b, boolean broken) {
    for (int i = 0; i < chapter.seedLinks.length; i++) {
      Link link = chapter.seedLinks[i];
      if ((link.a == a && link.b == b) || (link.a == b && link.b == a)) {
        link.bridge = true;
        link.broken = broken;
        return;
      }
    }
  }

  private String defaultName(NodeType type, int index) {
    if (type == NodeType.CAPITAL) {
      return index == 0 ? "Crownhold" : "Rival Crown";
    }
    if (type == NodeType.CITY) {
      return "City " + (index + 1);
    }
    if (type == NodeType.FARM) {
      return "Farm " + (index + 1);
    }
    if (type == NodeType.MINE) {
      return "Mine " + (index + 1);
    }
    if (type == NodeType.BARRACKS) {
      return "Barracks " + (index + 1);
    }
    if (type == NodeType.WATCHTOWER) {
      return "Watchtower " + (index + 1);
    }
    return "Fort " + (index + 1);
  }
}
