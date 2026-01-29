package com.android.boot.core;

import android.content.Context;

import com.android.boot.R;
import com.android.boot.entity.Node;
import com.android.boot.fx.Particle;
import com.android.boot.ui.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {
  public enum Difficulty {
    EASY,
    NORMAL,
    HARD
  }

  private final Context context;
  private final Random random = new Random();
  private final List<Node> nodes = new ArrayList<>();
  private final List<Particle> particles = new ArrayList<>();
  private final List<LevelData> campaignLevels = new ArrayList<>();
  private float elapsedTime;
  private float speed = 1f;
  private int sendPercent = 50;
  private boolean playing;
  private boolean campaign;
  private int campaignIndex;
  private Difficulty difficulty = Difficulty.NORMAL;
  private GameView.GameState resultState;
  private float[] aiTimers;
  private float[] aiIntervals;

  public GameEngine(Context context) {
    this.context = context;
    buildCampaignLevels();
  }

  public void reset() {
    playing = false;
    nodes.clear();
    particles.clear();
    elapsedTime = 0f;
    resultState = null;
  }

  public void startSkirmish() {
    reset();
    campaign = false;
    campaignIndex = 0;
    difficulty = Difficulty.NORMAL;
    generateSkirmish();
    playing = true;
  }

  public void startCampaign(int index) {
    reset();
    campaign = true;
    campaignIndex = index;
    LevelData level = campaignLevels.get(index % campaignLevels.size());
    difficulty = level.difficulty;
    loadLevel(level);
    playing = true;
  }

  public boolean isCampaign() {
    return campaign;
  }

  public int getCampaignIndex() {
    return campaignIndex;
  }

  public float toggleSpeed() {
    speed = speed < 1.5f ? 2f : 1f;
    return speed;
  }

  public int toggleSendPercent() {
    if (sendPercent == 25) {
      sendPercent = 50;
    } else if (sendPercent == 50) {
      sendPercent = 75;
    } else {
      sendPercent = 25;
    }
    return sendPercent;
  }

  public void pause() {
    playing = false;
  }

  public void resume() {
    playing = true;
  }

  public boolean isPlaying() {
    return playing;
  }

  public void update(float delta) {
    float scaledDelta = delta * speed;
    elapsedTime += scaledDelta;
    updateGrowth(scaledDelta);
    updateParticles(scaledDelta);
    updateAi(scaledDelta);
    checkWinLose();
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public List<Particle> getParticles() {
    return particles;
  }

  public int getOwnerColor(int owner) {
    if (owner == 1) {
      return context.getColor(R.color.cst_game_player);
    }
    if (owner == 0) {
      return context.getColor(R.color.cst_game_neutral);
    }
    return context.getColor(R.color.cst_game_enemy);
  }

  public int getOwnerParticleColor(int owner) {
    if (owner == 1) {
      return context.getColor(R.color.cst_game_particle);
    }
    if (owner == 0) {
      return context.getColor(R.color.cst_game_particle_neutral);
    }
    return context.getColor(R.color.cst_game_particle_enemy);
  }

  public void handleTap(float x, float y) {
    Node tapped = findNode(x, y);
    if (tapped == null) {
      clearSelection();
      return;
    }
    if (tapped.owner == 1) {
      selectNode(tapped);
      return;
    }
    Node selected = getSelectedNode();
    if (selected != null && selected.owner == 1) {
      sendUnits(selected, tapped, sendPercent, 1);
      selected.selected = false;
    }
  }

  private void updateGrowth(float delta) {
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.owner == 0) {
        continue;
      }
      node.growthBuffer += node.growthRate * delta;
      if (node.growthBuffer >= 1f) {
        int add = (int) node.growthBuffer;
        node.units += add;
        node.growthBuffer -= add;
      }
    }
  }

  private void updateParticles(float delta) {
    for (int i = particles.size() - 1; i >= 0; i--) {
      Particle particle = particles.get(i);
      particle.x += particle.vx * delta;
      particle.y += particle.vy * delta;
      particle.travel += delta;
      if (particle.travel >= particle.duration) {
        applyArrival(particle.target, particle.units, particle.owner);
        particles.remove(i);
      }
    }
  }

  private void updateAi(float delta) {
    if (aiTimers == null) {
      return;
    }
    for (int i = 0; i < aiTimers.length; i++) {
      aiTimers[i] += delta;
      if (aiTimers[i] >= aiIntervals[i]) {
        aiTimers[i] = 0f;
        aiIntervals[i] = randomInterval();
        runAiTurn(i + 2);
      }
    }
  }

  private void runAiTurn(int owner) {
    Node source = chooseAiSource(owner);
    if (source == null) {
      return;
    }
    Node target = chooseAiTarget(owner, source);
    if (target == null) {
      return;
    }
    int[] options = new int[]{40, 50, 60};
    int percent = options[random.nextInt(options.length)];
    sendUnits(source, target, percent, owner);
  }

  private Node chooseAiSource(int owner) {
    int threshold = difficulty == Difficulty.EASY ? 20 : difficulty == Difficulty.HARD ? 40 : 30;
    Node best = null;
    int bestUnits = 0;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.owner != owner) {
        continue;
      }
      if (node.units < threshold) {
        continue;
      }
      if (node.units > bestUnits) {
        bestUnits = node.units;
        best = node;
      }
    }
    return best;
  }

  private Node chooseAiTarget(int owner, Node source) {
    Node best = null;
    float bestScore = -1f;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      if (node.owner == owner) {
        continue;
      }
      float dx = node.x - source.x;
      float dy = node.y - source.y;
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      float score = 200f - dist - node.units * 2f;
      if (node.owner == 1 && random.nextFloat() < 0.2f) {
        score += 50f;
      }
      if (score > bestScore) {
        bestScore = score;
        best = node;
      }
    }
    return best;
  }

  private void sendUnits(Node source, Node target, int percent, int owner) {
    if (source.units <= 1) {
      return;
    }
    int amount = Math.max(1, source.units * percent / 100);
    source.units -= amount;
    int count = Math.max(6, Math.min(30, amount / 3));
    int baseUnits = amount / count;
    int remainder = amount - baseUnits * count;
    for (int i = 0; i < count; i++) {
      int units = baseUnits + (i < remainder ? 1 : 0);
      float angle = (float) (random.nextFloat() * Math.PI * 2f);
      float jitter = source.radius * 0.3f;
      float startX = source.x + (float) Math.cos(angle) * jitter;
      float startY = source.y + (float) Math.sin(angle) * jitter;
      Particle particle = Particle.create(startX, startY, target, units, owner);
      particles.add(particle);
    }
  }

  private void applyArrival(Node target, int units, int owner) {
    if (target.owner == owner) {
      target.units += units;
      return;
    }
    target.units -= units;
    if (target.units < 0) {
      target.owner = owner;
      target.units = Math.abs(target.units);
    }
  }

  private void checkWinLose() {
    int owned = getOwnedCount();
    if (owned == 0 && resultState == null) {
      resultState = GameView.GameState.LOSE;
      playing = false;
    } else if (owned == nodes.size() && resultState == null) {
      resultState = GameView.GameState.WIN;
      playing = false;
    }
  }

  public int getOwnedCount() {
    int count = 0;
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).owner == 1) {
        count++;
      }
    }
    return count;
  }

  public int getTotalCount() {
    return nodes.size();
  }

  public String getElapsedTimeText() {
    int totalSeconds = (int) elapsedTime;
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;
    String sec = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
    return minutes + ":" + sec;
  }

  public GameView.GameState getResultState() {
    return resultState;
  }

  public void clearResultState() {
    resultState = null;
  }

  private void selectNode(Node node) {
    clearSelection();
    node.selected = true;
  }

  private void clearSelection() {
    for (int i = 0; i < nodes.size(); i++) {
      nodes.get(i).selected = false;
    }
  }

  private Node getSelectedNode() {
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).selected) {
        return nodes.get(i);
      }
    }
    return null;
  }

  private Node findNode(float x, float y) {
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      float dx = x - node.x;
      float dy = y - node.y;
      if (dx * dx + dy * dy <= node.radius * node.radius) {
        return node;
      }
    }
    return null;
  }

  private void generateSkirmish() {
    int count = 18 + random.nextInt(13);
    int width = context.getResources().getDisplayMetrics().widthPixels;
    int height = context.getResources().getDisplayMetrics().heightPixels;
    int aiCount = 1 + random.nextInt(3);
    aiTimers = new float[aiCount];
    aiIntervals = new float[aiCount];
    for (int i = 0; i < aiCount; i++) {
      aiIntervals[i] = randomInterval();
    }
    nodes.clear();
    for (int i = 0; i < count; i++) {
      Node node = randomNode(width, height);
      nodes.add(node);
    }
    if (!nodes.isEmpty()) {
      nodes.get(0).owner = 1;
      nodes.get(0).units = 30;
      for (int i = 0; i < aiCount; i++) {
        int index = 1 + i;
        if (index < nodes.size()) {
          nodes.get(index).owner = i + 2;
          nodes.get(index).units = 28;
        }
      }
    }
  }

  private Node randomNode(int width, int height) {
    float radius = 34f + random.nextFloat() * 18f;
    float x = radius + random.nextFloat() * (width - radius * 2f);
    float y = radius + random.nextFloat() * (height - radius * 2f);
    Node node = new Node(x, y, radius);
    node.units = 15 + random.nextInt(20);
    node.owner = 0;
    node.growthRate = 2f + random.nextFloat() * 2f;
    return node;
  }

  private void loadLevel(LevelData level) {
    nodes.clear();
    particles.clear();
    elapsedTime = 0f;
    aiTimers = new float[level.aiCount];
    aiIntervals = new float[level.aiCount];
    for (int i = 0; i < level.aiCount; i++) {
      aiIntervals[i] = randomInterval();
    }
    int width = context.getResources().getDisplayMetrics().widthPixels;
    int height = context.getResources().getDisplayMetrics().heightPixels;
    for (int i = 0; i < level.nodes.size(); i++) {
      LevelNode spec = level.nodes.get(i);
      Node node = new Node(spec.x * width, spec.y * height, spec.radius * width);
      node.owner = spec.owner;
      node.units = spec.units;
      node.growthRate = spec.growth;
      nodes.add(node);
    }
  }

  private float randomInterval() {
    if (difficulty == Difficulty.HARD) {
      return 0.8f + random.nextFloat() * 0.3f;
    }
    if (difficulty == Difficulty.EASY) {
      return 1.2f + random.nextFloat() * 0.3f;
    }
    return 1.0f + random.nextFloat() * 0.3f;
  }

  private void buildCampaignLevels() {
    campaignLevels.add(levelOne());
    campaignLevels.add(levelTwo());
    campaignLevels.add(levelThree());
    campaignLevels.add(levelFour());
    campaignLevels.add(levelFive());
    campaignLevels.add(levelSix());
    campaignLevels.add(levelSeven());
    campaignLevels.add(levelEight());
    campaignLevels.add(levelNine());
    campaignLevels.add(levelTen());
    campaignLevels.add(levelEleven());
    campaignLevels.add(levelTwelve());
    campaignLevels.add(levelThirteen());
    campaignLevels.add(levelFourteen());
    campaignLevels.add(levelFifteen());
  }

  private LevelData levelOne() {
    LevelData level = new LevelData(Difficulty.EASY, 1);
    level.nodes.add(new LevelNode(0.2f, 0.5f, 0.06f, 1, 24, 2.2f));
    level.nodes.add(new LevelNode(0.5f, 0.3f, 0.055f, 0, 16, 1.8f));
    level.nodes.add(new LevelNode(0.5f, 0.7f, 0.055f, 0, 16, 1.8f));
    level.nodes.add(new LevelNode(0.8f, 0.5f, 0.06f, 2, 24, 2.2f));
    return level;
  }

  private LevelData levelTwo() {
    LevelData level = new LevelData(Difficulty.EASY, 1);
    level.nodes.add(new LevelNode(0.15f, 0.5f, 0.06f, 1, 26, 2.2f));
    level.nodes.add(new LevelNode(0.35f, 0.25f, 0.05f, 0, 18, 1.6f));
    level.nodes.add(new LevelNode(0.35f, 0.75f, 0.05f, 0, 18, 1.6f));
    level.nodes.add(new LevelNode(0.65f, 0.25f, 0.05f, 0, 18, 1.6f));
    level.nodes.add(new LevelNode(0.65f, 0.75f, 0.05f, 0, 18, 1.6f));
    level.nodes.add(new LevelNode(0.85f, 0.5f, 0.06f, 2, 26, 2.2f));
    return level;
  }

  private LevelData levelThree() {
    LevelData level = new LevelData(Difficulty.NORMAL, 2);
    level.nodes.add(new LevelNode(0.2f, 0.5f, 0.06f, 1, 26, 2.4f));
    level.nodes.add(new LevelNode(0.5f, 0.2f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.5f, 0.5f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.5f, 0.8f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.8f, 0.35f, 0.06f, 2, 26, 2.4f));
    level.nodes.add(new LevelNode(0.8f, 0.65f, 0.06f, 3, 26, 2.4f));
    return level;
  }

  private LevelData levelFour() {
    LevelData level = new LevelData(Difficulty.NORMAL, 2);
    level.nodes.add(new LevelNode(0.15f, 0.5f, 0.06f, 1, 26, 2.4f));
    level.nodes.add(new LevelNode(0.4f, 0.3f, 0.055f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.4f, 0.7f, 0.055f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.6f, 0.3f, 0.055f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.6f, 0.7f, 0.055f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.85f, 0.4f, 0.06f, 2, 28, 2.4f));
    level.nodes.add(new LevelNode(0.85f, 0.6f, 0.06f, 3, 28, 2.4f));
    return level;
  }

  private LevelData levelFive() {
    LevelData level = new LevelData(Difficulty.NORMAL, 2);
    level.nodes.add(new LevelNode(0.2f, 0.2f, 0.06f, 1, 28, 2.6f));
    level.nodes.add(new LevelNode(0.2f, 0.8f, 0.055f, 0, 18, 1.7f));
    level.nodes.add(new LevelNode(0.5f, 0.5f, 0.05f, 0, 22, 2.0f));
    level.nodes.add(new LevelNode(0.8f, 0.2f, 0.055f, 0, 18, 1.7f));
    level.nodes.add(new LevelNode(0.8f, 0.8f, 0.06f, 2, 28, 2.6f));
    level.nodes.add(new LevelNode(0.5f, 0.2f, 0.05f, 3, 24, 2.2f));
    return level;
  }

  private LevelData levelSix() {
    LevelData level = new LevelData(Difficulty.NORMAL, 2);
    level.nodes.add(new LevelNode(0.15f, 0.5f, 0.06f, 1, 28, 2.6f));
    level.nodes.add(new LevelNode(0.35f, 0.2f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.35f, 0.5f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.35f, 0.8f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.65f, 0.2f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.65f, 0.5f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.65f, 0.8f, 0.05f, 0, 20, 1.8f));
    level.nodes.add(new LevelNode(0.85f, 0.4f, 0.06f, 2, 30, 2.6f));
    level.nodes.add(new LevelNode(0.85f, 0.6f, 0.06f, 3, 30, 2.6f));
    return level;
  }

  private LevelData levelSeven() {
    LevelData level = new LevelData(Difficulty.NORMAL, 2);
    level.nodes.add(new LevelNode(0.2f, 0.5f, 0.06f, 1, 30, 2.6f));
    level.nodes.add(new LevelNode(0.45f, 0.25f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.45f, 0.75f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.55f, 0.5f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.75f, 0.3f, 0.06f, 2, 30, 2.6f));
    level.nodes.add(new LevelNode(0.75f, 0.7f, 0.06f, 3, 30, 2.6f));
    return level;
  }

  private LevelData levelEight() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.18f, 0.5f, 0.06f, 1, 30, 2.7f));
    level.nodes.add(new LevelNode(0.4f, 0.2f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.4f, 0.5f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.4f, 0.8f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.6f, 0.2f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.6f, 0.5f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.6f, 0.8f, 0.05f, 0, 22, 1.9f));
    level.nodes.add(new LevelNode(0.82f, 0.3f, 0.06f, 2, 32, 2.7f));
    level.nodes.add(new LevelNode(0.82f, 0.5f, 0.06f, 3, 32, 2.7f));
    level.nodes.add(new LevelNode(0.82f, 0.7f, 0.06f, 4, 32, 2.7f));
    return level;
  }

  private LevelData levelNine() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.2f, 0.2f, 0.06f, 1, 32, 2.8f));
    level.nodes.add(new LevelNode(0.2f, 0.8f, 0.055f, 0, 24, 2.1f));
    level.nodes.add(new LevelNode(0.5f, 0.2f, 0.055f, 0, 24, 2.1f));
    level.nodes.add(new LevelNode(0.5f, 0.8f, 0.055f, 0, 24, 2.1f));
    level.nodes.add(new LevelNode(0.5f, 0.5f, 0.05f, 0, 24, 2.1f));
    level.nodes.add(new LevelNode(0.8f, 0.2f, 0.06f, 2, 32, 2.8f));
    level.nodes.add(new LevelNode(0.8f, 0.5f, 0.06f, 3, 32, 2.8f));
    level.nodes.add(new LevelNode(0.8f, 0.8f, 0.06f, 4, 32, 2.8f));
    return level;
  }

  private LevelData levelTen() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.2f, 0.5f, 0.06f, 1, 34, 2.9f));
    level.nodes.add(new LevelNode(0.35f, 0.3f, 0.05f, 0, 26, 2.1f));
    level.nodes.add(new LevelNode(0.35f, 0.7f, 0.05f, 0, 26, 2.1f));
    level.nodes.add(new LevelNode(0.5f, 0.5f, 0.05f, 0, 26, 2.1f));
    level.nodes.add(new LevelNode(0.65f, 0.3f, 0.05f, 0, 26, 2.1f));
    level.nodes.add(new LevelNode(0.65f, 0.7f, 0.05f, 0, 26, 2.1f));
    level.nodes.add(new LevelNode(0.8f, 0.3f, 0.06f, 2, 34, 2.9f));
    level.nodes.add(new LevelNode(0.8f, 0.5f, 0.06f, 3, 34, 2.9f));
    level.nodes.add(new LevelNode(0.8f, 0.7f, 0.06f, 4, 34, 2.9f));
    return level;
  }

  private LevelData levelEleven() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.15f, 0.5f, 0.06f, 1, 34, 3.0f));
    level.nodes.add(new LevelNode(0.35f, 0.2f, 0.05f, 0, 26, 2.2f));
    level.nodes.add(new LevelNode(0.35f, 0.5f, 0.05f, 0, 26, 2.2f));
    level.nodes.add(new LevelNode(0.35f, 0.8f, 0.05f, 0, 26, 2.2f));
    level.nodes.add(new LevelNode(0.55f, 0.3f, 0.05f, 0, 26, 2.2f));
    level.nodes.add(new LevelNode(0.55f, 0.7f, 0.05f, 0, 26, 2.2f));
    level.nodes.add(new LevelNode(0.75f, 0.3f, 0.06f, 2, 36, 3.0f));
    level.nodes.add(new LevelNode(0.75f, 0.7f, 0.06f, 3, 36, 3.0f));
    level.nodes.add(new LevelNode(0.85f, 0.5f, 0.06f, 4, 36, 3.0f));
    return level;
  }

  private LevelData levelTwelve() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.2f, 0.2f, 0.06f, 1, 36, 3.1f));
    level.nodes.add(new LevelNode(0.2f, 0.8f, 0.055f, 0, 28, 2.4f));
    level.nodes.add(new LevelNode(0.4f, 0.5f, 0.05f, 0, 28, 2.4f));
    level.nodes.add(new LevelNode(0.6f, 0.5f, 0.05f, 0, 28, 2.4f));
    level.nodes.add(new LevelNode(0.8f, 0.2f, 0.06f, 2, 36, 3.1f));
    level.nodes.add(new LevelNode(0.8f, 0.5f, 0.06f, 3, 36, 3.1f));
    level.nodes.add(new LevelNode(0.8f, 0.8f, 0.06f, 4, 36, 3.1f));
    return level;
  }

  private LevelData levelThirteen() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.15f, 0.5f, 0.06f, 1, 36, 3.2f));
    level.nodes.add(new LevelNode(0.35f, 0.25f, 0.05f, 0, 28, 2.4f));
    level.nodes.add(new LevelNode(0.35f, 0.75f, 0.05f, 0, 28, 2.4f));
    level.nodes.add(new LevelNode(0.5f, 0.5f, 0.05f, 0, 28, 2.4f));
    level.nodes.add(new LevelNode(0.7f, 0.3f, 0.06f, 2, 38, 3.2f));
    level.nodes.add(new LevelNode(0.7f, 0.7f, 0.06f, 3, 38, 3.2f));
    level.nodes.add(new LevelNode(0.85f, 0.5f, 0.06f, 4, 38, 3.2f));
    return level;
  }

  private LevelData levelFourteen() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.2f, 0.5f, 0.06f, 1, 38, 3.3f));
    level.nodes.add(new LevelNode(0.4f, 0.25f, 0.05f, 0, 30, 2.6f));
    level.nodes.add(new LevelNode(0.4f, 0.5f, 0.05f, 0, 30, 2.6f));
    level.nodes.add(new LevelNode(0.4f, 0.75f, 0.05f, 0, 30, 2.6f));
    level.nodes.add(new LevelNode(0.6f, 0.25f, 0.05f, 0, 30, 2.6f));
    level.nodes.add(new LevelNode(0.6f, 0.5f, 0.05f, 0, 30, 2.6f));
    level.nodes.add(new LevelNode(0.6f, 0.75f, 0.05f, 0, 30, 2.6f));
    level.nodes.add(new LevelNode(0.8f, 0.3f, 0.06f, 2, 40, 3.3f));
    level.nodes.add(new LevelNode(0.8f, 0.5f, 0.06f, 3, 40, 3.3f));
    level.nodes.add(new LevelNode(0.8f, 0.7f, 0.06f, 4, 40, 3.3f));
    return level;
  }

  private LevelData levelFifteen() {
    LevelData level = new LevelData(Difficulty.HARD, 3);
    level.nodes.add(new LevelNode(0.15f, 0.5f, 0.06f, 1, 40, 3.4f));
    level.nodes.add(new LevelNode(0.35f, 0.2f, 0.05f, 0, 32, 2.8f));
    level.nodes.add(new LevelNode(0.35f, 0.5f, 0.05f, 0, 32, 2.8f));
    level.nodes.add(new LevelNode(0.35f, 0.8f, 0.05f, 0, 32, 2.8f));
    level.nodes.add(new LevelNode(0.55f, 0.3f, 0.05f, 0, 32, 2.8f));
    level.nodes.add(new LevelNode(0.55f, 0.7f, 0.05f, 0, 32, 2.8f));
    level.nodes.add(new LevelNode(0.75f, 0.2f, 0.06f, 2, 42, 3.4f));
    level.nodes.add(new LevelNode(0.75f, 0.5f, 0.06f, 3, 42, 3.4f));
    level.nodes.add(new LevelNode(0.75f, 0.8f, 0.06f, 4, 42, 3.4f));
    level.nodes.add(new LevelNode(0.9f, 0.5f, 0.06f, 5, 42, 3.4f));
    return level;
  }

  private static class LevelData {
    private final Difficulty difficulty;
    private final int aiCount;
    private final List<LevelNode> nodes = new ArrayList<>();

    LevelData(Difficulty difficulty, int aiCount) {
      this.difficulty = difficulty;
      this.aiCount = aiCount;
    }
  }

  private static class LevelNode {
    private final float x;
    private final float y;
    private final float radius;
    private final int owner;
    private final int units;
    private final float growth;

    LevelNode(float x, float y, float radius, int owner, int units, float growth) {
      this.x = x;
      this.y = y;
      this.radius = radius;
      this.owner = owner;
      this.units = units;
      this.growth = growth;
    }
  }
}
