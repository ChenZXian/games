package com.android.boot.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;

import com.android.boot.R;
import com.android.boot.entity.Prefecture;
import com.android.boot.fx.Particle;
import com.android.boot.ui.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {
  public enum Formation {
    BALANCED,
    SHIELD_WALL,
    ARCHER_SWARM
  }

  public enum Difficulty {
    EASY,
    NORMAL,
    HARD
  }

  private final Context context;
  private final Random random = new Random();
  private final List<Prefecture> prefectures = new ArrayList<>();
  private final List<Particle> particles = new ArrayList<>();
  private final List<LevelData> campaignLevels = new ArrayList<>();

  // World map size (for camera fitting, background drawing)
  private float worldMapWidth = 0f;
  private float worldMapHeight = 0f;
  private float elapsedTime;
  private float speed = 1f;
  private int sendPercent = 50;
  private Formation formation = Formation.BALANCED;
  private boolean playing;
  private boolean campaign;
  private int campaignIndex;
  private Difficulty difficulty = Difficulty.NORMAL;
  private GameView.GameState resultState;
  private float[] aiTimers;
  private float[] aiIntervals;
  private boolean battlePaused;
  private final boolean[] roundOwnerFought = new boolean[8];
  
  // Turn-based system
  private int currentTurn = 1;  // Current turn number
  private boolean isPlayerTurn = true;  // Whether player turn
  private int currentAiOwner = 2;  // Current AI faction taking action (2, 3, 4...)
  private boolean processingAiTurn = false;  // Whether processing AI turn
  
  // Resource system
  private int playerGold = 500;  // Player initial gold
  private static final int UNIT_COST_SHIELD = 50;
  private static final int UNIT_COST_SWORD = 40;
  private static final int UNIT_COST_ARCHER = 45;
  private static final int UNIT_COST_GIANT_SHIELD = 200;
  private static final int UNIT_COST_GIANT_SWORD = 180;
  private static final int UNIT_COST_GIANT_ARCHER = 190;

  public GameEngine(Context context) {
    this.context = context;
    buildCampaignLevels();
  }

  public void reset() {
    playing = false;
    prefectures.clear();
    particles.clear();
    elapsedTime = 0f;
    resultState = null;
    playerGold = 500;  // Reset player gold
    worldMapWidth = 0f;
    worldMapHeight = 0f;
    // Reset turn system
    currentTurn = 1;
    isPlayerTurn = true;
    currentAiOwner = 2;
    processingAiTurn = false;
  }

  public float getWorldMapWidth() {
    return worldMapWidth;
  }

  public float getWorldMapHeight() {
    return worldMapHeight;
  }

  public void startSkirmish() {
    reset();
    campaign = false;
    campaignIndex = 0;
    difficulty = Difficulty.NORMAL;
    generateSkirmish();
    playing = true;
  }

  // Generate visible map in menu/not started state (no time advance, no AI trigger)
  public void ensurePreviewMap() {
    if (!prefectures.isEmpty()) {
      return;
    }
    reset();
    campaign = false;
    campaignIndex = 0;
    difficulty = Difficulty.NORMAL;
    generateSkirmish();
    playing = false;
    elapsedTime = 0f;
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

  public String toggleFormation() {
    if (formation == Formation.BALANCED) {
      formation = Formation.SHIELD_WALL;
    } else if (formation == Formation.SHIELD_WALL) {
      formation = Formation.ARCHER_SWARM;
    } else {
      formation = Formation.BALANCED;
    }
    return getFormationLabel();
  }

  public String getFormationLabel() {
    if (formation == Formation.SHIELD_WALL) {
      return "S50 W30 A20";
    }
    if (formation == Formation.ARCHER_SWARM) {
      return "S20 W30 A50";
    }
    return "S35 W45 A20";
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

  public void setBattlePaused(boolean paused) {
    battlePaused = paused;
  }

  public boolean isBattlePaused() {
    return battlePaused;
  }

  public void update(float delta) {
    float scaledDelta = delta * speed;
    elapsedTime += scaledDelta;
    // growth is now handled per \"round\" in onBattleResolved(), not per second
    updateParticles(scaledDelta);
    updateResources(scaledDelta);
    if (!battlePaused) {
      updateBattles(scaledDelta);
    }
    // Process AI turn (if not player turn)
    if (!isPlayerTurn) {
      processAiTurn(scaledDelta);
    } else {
      // Pause AI auto action during player turn
      updateAi(scaledDelta);
    }
    checkWinLose();
  }
  
  private void updateResources(float delta) {
    
    for (Prefecture pref : prefectures) {
      if (pref.owner == 1) {
        float income = pref.goldIncome * delta;
        playerGold += (int) income;
      }
    }
  }
  
  public int getPlayerGold() {
    return playerGold;
  }
  
  public boolean buyUnits(Prefecture pref, int unitType, boolean isGiant, int count) {
    if (pref == null || pref.owner != 1) {
      return false;
    }
    
    int cost;
    if (isGiant) {
      switch (unitType) {
        case 0: cost = UNIT_COST_GIANT_SHIELD; break;
        case 1: cost = UNIT_COST_GIANT_SWORD; break;
        case 2: cost = UNIT_COST_GIANT_ARCHER; break;
        default: return false;
      }
    } else {
      switch (unitType) {
        case 0: cost = UNIT_COST_SHIELD; break;
        case 1: cost = UNIT_COST_SWORD; break;
        case 2: cost = UNIT_COST_ARCHER; break;
        default: return false;
      }
    }
    
    int totalCost = cost * count;
    if (playerGold < totalCost) {
      return false;
    }
    
    playerGold -= totalCost;
    
    if (isGiant) {
      switch (unitType) {
        case 0: pref.giantShield += count; break;
        case 1: pref.giantSword += count; break;
        case 2: pref.giantArcher += count; break;
      }
    } else {
      switch (unitType) {
        case 0: pref.shield += count; break;
        case 1: pref.sword += count; break;
        case 2: pref.archer += count; break;
      }
    }
    
    android.util.Log.d("GameEngine", String.format("buyUnits: pref=%s, type=%d, isGiant=%b, count=%d, after: shield=%d, sword=%d, archer=%d", 
      pref.name, unitType, isGiant, count, pref.shield, pref.sword, pref.archer));
    
    return true;
  }
  
  public boolean buildStructure(Prefecture pref, int buildingType) {
    if (pref == null || pref.owner != 1) {
      return false;
    }
    
    int cost = 300;  
    if (playerGold < cost) {
      return false;
    }
    
    playerGold -= cost;
    pref.buildingType = buildingType;
    
    
    switch (buildingType) {
      case Prefecture.BUILDING_CANNON:
        pref.specialEffect = Prefecture.EFFECT_NONE;
        break;
      case Prefecture.BUILDING_BARRACKS:
        pref.growthRate *= 1.5f;
        pref.specialEffect = Prefecture.EFFECT_RAPID_GEN;
        break;
      case Prefecture.BUILDING_FORTRESS:
        pref.specialEffect = Prefecture.EFFECT_DEFENSE_BOOST;
        break;
      case Prefecture.BUILDING_TRAINING:
        pref.specialEffect = Prefecture.EFFECT_GIANT_SPAWN;
        break;
    }
    
    return true;
  }

  public List<Prefecture> getPrefectures() {
    return prefectures;
  }

  public Prefecture hitTest(float x, float y) {
    return findPrefecture(x, y);
  }

  public Prefecture getSelectedPrefecturePublic() {
    return getSelectedPrefecture();
  }

  public void clearSelectionPublic() {
    clearSelection();
  }

  public void selectPrefecturePublic(Prefecture pref) {
    selectPrefecture(pref);
  }

  public boolean isNeighbor(Prefecture a, Prefecture b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.neighbors == null) {
      return false;
    }
    for (int i = 0; i < a.neighbors.length; i++) {
      if (a.neighbors[i] == b.id) {
        return true;
      }
    }
    return false;
  }

  public boolean tryPlayerSend(Prefecture source, Prefecture target) {
    if (source == null || target == null) {
      return false;
    }
    if (source.owner != 1) {
      return false;
    }
    if (!isNeighbor(source, target)) {
      return false;
    }
    
    if (source.owner == target.owner) {
      return false;
    }

    if (source.hasAttackedThisTurn) {
      return false;
    }
    if (!isPlayerTurn) {
      return false;  
    }
    // Send all units when player attacks (keep 1)
    sendUnits(source, target, 100, 1);
    
    source.hasAttackedThisTurn = true;
    return true;
  }
  
  // AI
  public void endPlayerTurn() {
    if (!isPlayerTurn) {
      return;
    }
    isPlayerTurn = false;
    currentAiOwner = 2;  // AI
    processingAiTurn = true;
    android.util.Log.d("GameEngine", String.format("Player turn ended, starting AI turn %d", currentTurn));
  }
  
  // AI
  private boolean allAiFinished() {
    // AIowner >= 2
    for (int owner = 2; owner < 8; owner++) {
      boolean hasTerritory = false;
      for (Prefecture pref : prefectures) {
        if (pref.owner == owner) {
          hasTerritory = true;
          break;
        }
      }
      if (hasTerritory) {
        
        for (Prefecture pref : prefectures) {
          if (pref.owner == owner && !pref.hasAttackedThisTurn && pref.total() > 1) {
            return false;  
          }
        }
      }
    }
    return true;  // AI
  }
  
  // AIupdate
  public void processAiTurn(float delta) {
    if (isPlayerTurn || !processingAiTurn) {
      return;
    }
    
    // Particle
    if (hasActiveBattles() || !particles.isEmpty()) {
      return;  
    }
    
    // AI
    boolean currentAiDone = processSingleAiTurn(currentAiOwner);
    
    if (currentAiDone) {
      // AIAI
      currentAiOwner++;
      // AI
      if (currentAiOwner >= 8 || allAiFinished()) {
        // AI
        startNewTurn();
      }
    }
  }
  
  // AI
  private boolean processSingleAiTurn(int owner) {

    Prefecture source = null;
    for (Prefecture pref : prefectures) {
      if (pref.owner == owner && !pref.hasAttackedThisTurn && pref.total() > 1) {
        source = pref;
        break;
      }
    }
    
    if (source == null) {
      return true;  // AI
    }
    
    
    Prefecture target = chooseAiTarget(owner, source);
    if (target != null) {
      
      sendUnits(source, target, sendPercent, owner);
      source.hasAttackedThisTurn = true;
      android.util.Log.d("GameEngine", String.format("AI %d attacked from %s to %s", owner, source.name, target.name));
      return false;  
    } else {

      source.hasAttackedThisTurn = true;
      return false;  
    }
  }
  
  
  private boolean hasActiveBattles() {
    for (Prefecture pref : prefectures) {
      if (pref.battleActive) {
        return true;
      }
    }
    return false;
  }
  
  
  private void startNewTurn() {
    currentTurn++;
    isPlayerTurn = true;
    processingAiTurn = false;
    currentAiOwner = 2;
    
    
    for (Prefecture pref : prefectures) {
      pref.hasAttackedThisTurn = false;
    }
    
    
    processTurnEndEconomy();
    
    android.util.Log.d("GameEngine", String.format("New turn started: %d", currentTurn));
  }
  
  
  private void processTurnEndEconomy() {

    for (Prefecture pref : prefectures) {
      if (pref.owner == 1) {
        // 5
        float baseIncome = pref.goldIncome > 0 ? pref.goldIncome : 5f;
        // = * 2
        int income = (int) (baseIncome * 2f);
        playerGold += income;
        android.util.Log.d("GameEngine", String.format("Turn end: %s produced %d gold (base=%.1f), total: %d", pref.name, income, baseIncome, playerGold));
      }
    }
  }
  
  
  public int getCurrentTurn() {
    return currentTurn;
  }
  
  public boolean isPlayerTurn() {
    return isPlayerTurn;
  }
  
  public boolean isProcessingAiTurn() {
    return processingAiTurn;
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
    // AI
    // owner 2-7
    int[] aiColors = {
      context.getColor(R.color.cst_game_enemy),  // owner 2
      Color.rgb(200, 100, 100),  // owner 3 -
      Color.rgb(100, 200, 100),  // owner 4 -
      Color.rgb(100, 100, 200),  // owner 5 -
      Color.rgb(200, 200, 100),  // owner 6 -
      Color.rgb(200, 100, 200)   // owner 7 -
    };
    int index = owner - 2;
    if (index >= 0 && index < aiColors.length) {
      return aiColors[index];
    }
    return context.getColor(R.color.cst_game_enemy);
  }

  public String getPrefectureName(Prefecture pref) {
    return pref.name != null ? pref.name : "";
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
    Prefecture tapped = findPrefecture(x, y);
    if (tapped == null) {
      clearSelection();
      return;
    }
    if (tapped.owner == 1) {
      selectPrefecture(tapped);
      return;
    }
    Prefecture selected = getSelectedPrefecture();
    if (selected != null && selected.owner == 1) {
      if (isNeighbor(selected, tapped)) {
        sendUnits(selected, tapped, sendPercent, 1);
        selected.selected = false;
      }
    }
  }

  private void updateGrowth(float delta) {
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      // legacy per-second growth disabled; see doGrowthPulse()
    }
  }

  private void doGrowthPulse() {
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      if (pref.owner == 0) {
        continue;
      }
      float g = pref.growthRate;
      int add = Math.max(1, (int) g);
      int total = pref.shield + pref.sword + pref.archer;
      int sAdd = 0, wAdd = 0, aAdd = 0;
      if (total <= 0) {
        // 1:2:1
        sAdd = add / 4;
        aAdd = sAdd;
        wAdd = add - sAdd - aAdd;
      } else {
        float sRatio = pref.shield / (float) total;
        float wRatio = pref.sword / (float) total;
        float aRatio = pref.archer / (float) total;
        sAdd = Math.round(add * sRatio);
        wAdd = Math.round(add * wRatio);
        aAdd = Math.round(add * aRatio);
        int sum = sAdd + wAdd + aAdd;
        int diff = add - sum;

        wAdd += diff;
        if (wAdd < 0) {
          wAdd = 0;
        }
      }
      pref.shield += sAdd;
      pref.sword += wAdd;
      pref.archer += aAdd;
      pref.growthBuffer = 0f;
    }
  }

  public void onBattleResolved(int attackerOwner, int defenderOwner) {
    markOwnerFought(attackerOwner);
    markOwnerFought(defenderOwner);
    if (allOwnersFoughtThisRound()) {
      doGrowthPulse();
      clearRoundFlags();
    }
  }

  private void markOwnerFought(int owner) {
    if (owner <= 0 || owner >= roundOwnerFought.length) {
      return;
    }
    roundOwnerFought[owner] = true;
  }

  private boolean allOwnersFoughtThisRound() {
    boolean any = false;
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      int owner = pref.owner;
      if (owner <= 0 || owner >= roundOwnerFought.length) {
        continue;
      }
      any = true;
      if (!roundOwnerFought[owner]) {
        return false;
      }
    }
    return any;
  }

  private void clearRoundFlags() {
    for (int i = 0; i < roundOwnerFought.length; i++) {
      roundOwnerFought[i] = false;
    }
  }

  private void updateParticles(float delta) {
    for (int i = particles.size() - 1; i >= 0; i--) {
      Particle particle = particles.get(i);
      particle.x += particle.vx * delta;
      particle.y += particle.vy * delta;
      particle.travel += delta;
      if (particle.travel >= particle.duration) {
        applyArrival(particle.target, particle.shield, particle.sword, particle.archer, particle.owner);
        particles.remove(i);
      }
    }
  }

  private void updateBattles(float delta) {
    float tick = 0.35f;
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      if (!pref.battleActive) {
        continue;
      }
      pref.battleTime += delta;
      pref.battleTickBuffer += delta;
      while (pref.battleTickBuffer >= tick) {
        pref.battleTickBuffer -= tick;
        battleTick(pref, tick);
        if (!pref.battleActive) {
          break;
        }
      }
    }
  }

  private void battleTick(Prefecture target, float dt) {
    int defShield = target.shield;
    int defSword = target.sword;
    int defArcher = target.archer;
    int atkShield = target.battleAtkShield;
    int atkSword = target.battleAtkSword;
    int atkArcher = target.battleAtkArcher;

    if (defShield + defSword + defArcher <= 0) {
      resolveAttackerWin(target);
      return;
    }
    if (atkShield + atkSword + atkArcher <= 0) {
      resolveDefenderWin(target);
      return;
    }

    float approach = clamp01(target.battleTime / 1.35f);
    boolean meleeEngaged = target.battleTime >= 1.10f;
    boolean shieldEngaged = target.battleTime >= 0.85f;

    float atkDpsArcher = atkArcher * (0.28f + 0.42f * approach);
    float defDpsArcher = defArcher * (0.28f + 0.42f * approach);

    float atkDpsSword = meleeEngaged ? (atkSword * 0.38f) : 0f;
    float defDpsSword = meleeEngaged ? (defSword * 0.38f) : 0f;

    float atkDpsShield = shieldEngaged ? (atkShield * 0.12f) : 0f;
    float defDpsShield = shieldEngaged ? (defShield * 0.12f) : 0f;

    int toDefFromArcher = Math.max(0, (int) (atkDpsArcher * dt));
    int toDefFromSword = Math.max(0, (int) (atkDpsSword * dt));
    int toDefFromShield = Math.max(0, (int) (atkDpsShield * dt));
    int toAtkFromArcher = Math.max(0, (int) (defDpsArcher * dt));
    int toAtkFromSword = Math.max(0, (int) (defDpsSword * dt));
    int toAtkFromShield = Math.max(0, (int) (defDpsShield * dt));

    int[] def = new int[]{defShield, defSword, defArcher};
    int[] atk = new int[]{atkShield, atkSword, atkArcher};

    applyDamage(atkTypeArcher(), toDefFromArcher, def);
    applyDamage(atkTypeSword(), toDefFromSword, def);
    applyDamage(atkTypeShield(), toDefFromShield, def);

    applyDamage(atkTypeArcher(), toAtkFromArcher, atk);
    applyDamage(atkTypeSword(), toAtkFromSword, atk);
    applyDamage(atkTypeShield(), toAtkFromShield, atk);

    target.shield = Math.max(0, def[0]);
    target.sword = Math.max(0, def[1]);
    target.archer = Math.max(0, def[2]);
    target.battleAtkShield = Math.max(0, atk[0]);
    target.battleAtkSword = Math.max(0, atk[1]);
    target.battleAtkArcher = Math.max(0, atk[2]);

    if (target.shield + target.sword + target.archer <= 0) {
      resolveAttackerWin(target);
    } else if (target.battleAtkShield + target.battleAtkSword + target.battleAtkArcher <= 0) {
      resolveDefenderWin(target);
    }
  }

  private float clamp01(float v) {
    if (v < 0f) {
      return 0f;
    }
    if (v > 1f) {
      return 1f;
    }
    return v;
  }

  private int atkTypeShield() {
    return 0;
  }

  private int atkTypeSword() {
    return 1;
  }

  private int atkTypeArcher() {
    return 2;
  }

  private void applyDamage(int attackerType, int rawDamage, int[] defender) {
    if (rawDamage <= 0) {
      return;
    }
    int target1;
    int target2;
    int target3;
    if (attackerType == 2) {
      target1 = 1;
      target2 = 2;
      target3 = 0;
    } else if (attackerType == 1) {
      target1 = 0;
      target2 = 1;
      target3 = 2;
    } else {
      target1 = 1;
      target2 = 0;
      target3 = 2;
    }

    int remaining = rawDamage;
    remaining = applyDamageToTarget(attackerType, target1, remaining, defender);
    if (remaining > 0) {
      remaining = applyDamageToTarget(attackerType, target2, remaining, defender);
    }
    if (remaining > 0) {
      applyDamageToTarget(attackerType, target3, remaining, defender);
    }
  }

  private int applyDamageToTarget(int attackerType, int defenderType, int rawDamage, int[] defender) {
    if (rawDamage <= 0) {
      return 0;
    }
    int available = defender[defenderType];
    if (available <= 0) {
      return rawDamage;
    }
    float mult = 1f;
    if (attackerType == 0 && defenderType == 2) {
      mult = 1.4f;
    } else if (attackerType == 1 && defenderType == 0) {
      mult = 1.4f;
    } else if (attackerType == 2 && defenderType == 1) {
      mult = 1.4f;
    } else if (attackerType == 2 && defenderType == 0) {
      mult = 0.65f;
    }
    int kills = Math.max(0, (int) (rawDamage * mult));
    if (kills <= 0) {
      return rawDamage;
    }
    int applied = Math.min(available, kills);
    defender[defenderType] -= applied;
    int usedRaw = Math.max(1, (int) (applied / Math.max(0.1f, mult)));
    return Math.max(0, rawDamage - usedRaw);
  }

  private void resolveAttackerWin(Prefecture target) {
    int keepPercent = 70;
    int kShield = target.battleAtkShield * keepPercent / 100;
    int kSword = target.battleAtkSword * keepPercent / 100;
    int kArcher = target.battleAtkArcher * keepPercent / 100;
    target.owner = target.battleAttackerOwner;
    target.shield = kShield;
    target.sword = kSword;
    target.archer = kArcher;

    if (target.owner == 1) {
      target.goldIncome = 5f + random.nextFloat() * 3f;
    } else {
      target.goldIncome = 0f;
    }
    clearBattle(target);
  }

  private void resolveDefenderWin(Prefecture target) {
    clearBattle(target);
  }

  private void clearBattle(Prefecture target) {
    target.battleActive = false;
    target.battleAttackerOwner = 0;
    target.battleAtkShield = 0;
    target.battleAtkSword = 0;
    target.battleAtkArcher = 0;
    target.battleTickBuffer = 0f;
    target.battleTime = 0f;
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
    Prefecture source = chooseAiSource(owner);
    if (source == null) {
      return;
    }
    Prefecture target = chooseAiTarget(owner, source);
    if (target == null) {
      return;
    }
    int[] options = new int[]{40, 50, 60};
    int percent = options[random.nextInt(options.length)];
    sendUnits(source, target, percent, owner);
  }

  private Prefecture chooseAiSource(int owner) {
    int threshold = difficulty == Difficulty.EASY ? 20 : difficulty == Difficulty.HARD ? 40 : 30;
    Prefecture best = null;
    int bestUnits = 0;
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      if (pref.owner != owner) {
        continue;
      }
      int units = pref.total();
      if (units < threshold) {
        continue;
      }
      if (units > bestUnits) {
        bestUnits = units;
        best = pref;
      }
    }
    return best;
  }

  private Prefecture chooseAiTarget(int owner, Prefecture source) {
    Prefecture best = null;
    float bestScore = -1f;
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      
      if (pref.owner == owner) {
        continue;
      }
      
      if (!isNeighbor(source, pref)) {
        continue;
      }
      float dx = pref.x - source.x;
      float dy = pref.y - source.y;
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      float score = 200f - dist - pref.total() * 2f;
      if (pref.owner == 1 && random.nextFloat() < 0.2f) {
        score += 50f;
      }
      if (score > bestScore) {
        bestScore = score;
        best = pref;
      }
    }
    return best;
  }

  private void sendUnits(Prefecture source, Prefecture target, int percent, int owner) {
    int total = source.total();
    if (total <= 1) {
      return;
    }

    int sendShield, sendSword, sendArcher;
    
    // owner == 1 percent == 1001
    if (owner == 1 && percent == 100) {
      // 1
      // 1
      if (total <= 1) {
        return;
      }
      // 1
      sendShield = source.shield;
      sendSword = source.sword;
      sendArcher = source.archer;
      
      // 1
      if (sendShield > 0) {
        sendShield--;
      } else if (sendSword > 0) {
        sendSword--;
      } else if (sendArcher > 0) {
        sendArcher--;
      }
      
      // 1
      if (sendShield + sendSword + sendArcher <= 0) {
        return;
      }
    } else {
      // AI
      int amount = Math.max(1, total * percent / 100);
      int wantShield;
      int wantSword;
      int wantArcher;
      if (formation == Formation.SHIELD_WALL) {
        wantShield = amount * 50 / 100;
        wantSword = amount * 30 / 100;
        wantArcher = amount - wantShield - wantSword;
      } else if (formation == Formation.ARCHER_SWARM) {
        wantShield = amount * 20 / 100;
        wantSword = amount * 30 / 100;
        wantArcher = amount - wantShield - wantSword;
      } else {
        wantShield = amount * 35 / 100;
        wantSword = amount * 45 / 100;
        wantArcher = amount - wantShield - wantSword;
      }

      sendShield = Math.min(source.shield, wantShield);
      sendSword = Math.min(source.sword, wantSword);
      sendArcher = Math.min(source.archer, wantArcher);
      
      android.util.Log.d("GameEngine", String.format("sendUnits: source=%s, total=%d, percent=%d, amount=%d, source units: shield=%d, sword=%d, archer=%d, want: shield=%d, sword=%d, archer=%d, send: shield=%d, sword=%d, archer=%d", 
        source.name, total, percent, amount, source.shield, source.sword, source.archer, wantShield, wantSword, wantArcher, sendShield, sendSword, sendArcher));
      int sent = sendShield + sendSword + sendArcher;
      int remainNeed = amount - sent;
      if (remainNeed > 0) {
        int addSword = Math.min(source.sword - sendSword, remainNeed);
        sendSword += Math.max(0, addSword);
        remainNeed -= Math.max(0, addSword);
      }
      if (remainNeed > 0) {
        int addShield = Math.min(source.shield - sendShield, remainNeed);
        sendShield += Math.max(0, addShield);
        remainNeed -= Math.max(0, addShield);
      }
      if (remainNeed > 0) {
        int addArcher = Math.min(source.archer - sendArcher, remainNeed);
        sendArcher += Math.max(0, addArcher);
      }
    }
    

    if (owner == 1 && percent == 100) {
      android.util.Log.d("GameEngine", String.format("sendUnits: source=%s, total=%d, percent=%d, PLAYER SEND ALL, source units: shield=%d, sword=%d, archer=%d, send: shield=%d, sword=%d, archer=%d", 
        source.name, total, percent, source.shield + sendShield, source.sword + sendSword, source.archer + sendArcher, sendShield, sendSword, sendArcher));
    }

    if (sendShield + sendSword + sendArcher <= 0) {
      return;
    }

    source.shield -= sendShield;
    source.sword -= sendSword;
    source.archer -= sendArcher;

    int count = Math.max(6, Math.min(24, (sendShield + sendSword + sendArcher) / 4));
    int bShield = sendShield / count;
    int bSword = sendSword / count;
    int bArcher = sendArcher / count;
    int rShield = sendShield - bShield * count;
    int rSword = sendSword - bSword * count;
    int rArcher = sendArcher - bArcher * count;
    for (int i = 0; i < count; i++) {
      int pShield = bShield + (i < rShield ? 1 : 0);
      int pSword = bSword + (i < rSword ? 1 : 0);
      int pArcher = bArcher + (i < rArcher ? 1 : 0);
      if (pShield + pSword + pArcher <= 0) {
        continue;
      }
      float angle = (float) (random.nextFloat() * Math.PI * 2f);
      float jitter = source.radius * 0.3f;
      float startX = source.x + (float) Math.cos(angle) * jitter;
      float startY = source.y + (float) Math.sin(angle) * jitter;
      Particle particle = Particle.create(startX, startY, target, pShield, pSword, pArcher, owner);
      particles.add(particle);
    }
  }

  private void applyArrival(Prefecture target, int shield, int sword, int archer, int owner) {
    if (shield + sword + archer <= 0) {
      return;
    }

    android.util.Log.d("GameEngine", String.format("applyArrival: target=%s, owner=%d, shield=%d, sword=%d, archer=%d, battleActive=%b, battleAttackerOwner=%d, current battleAtk: shield=%d, sword=%d, archer=%d", 
      target.name, owner, shield, sword, archer, target.battleActive, target.battleAttackerOwner, target.battleAtkShield, target.battleAtkSword, target.battleAtkArcher));

    if (target.owner == owner && !target.battleActive) {
      target.shield += shield;
      target.sword += sword;
      target.archer += archer;
      return;
    }


    if (target.battleActive && target.owner == owner) {
      target.shield += shield;
      target.sword += sword;
      target.archer += archer;
      return;
    }

    if (!target.battleActive) {
      target.battleActive = true;
      target.battleNonce += 1;
      target.battleAttackerOwner = owner;
      // Particle Particle

      if (target.battleAtkShield == 0 && target.battleAtkSword == 0 && target.battleAtkArcher == 0) {
        target.battleAtkShield = shield;
        target.battleAtkSword = sword;
        target.battleAtkArcher = archer;
      } else {
        // Particle
        target.battleAtkShield += shield;
        target.battleAtkSword += sword;
        target.battleAtkArcher += archer;
      }
      target.battleTickBuffer = 0f;
      target.battleTime = 0f;
      // battleTick
      // initFrom
      target.battleDefShieldStart = target.shield;
      target.battleDefSwordStart = target.sword;
      target.battleDefArcherStart = target.archer;
      android.util.Log.d("GameEngine", String.format("applyArrival: started battle, battleAtk: shield=%d, sword=%d, archer=%d", 
        target.battleAtkShield, target.battleAtkSword, target.battleAtkArcher));
      return;
    }

    if (target.battleAttackerOwner == owner) {

      target.battleAtkShield += shield;
      target.battleAtkSword += sword;
      target.battleAtkArcher += archer;
      android.util.Log.d("GameEngine", String.format("applyArrival: added to attacker, battleAtk: shield=%d, sword=%d, archer=%d", 
        target.battleAtkShield, target.battleAtkSword, target.battleAtkArcher));
    } else if (target.battleActive && target.owner == owner) {

      target.shield += shield;
      target.sword += sword;
      target.archer += archer;
    } else if (target.battleActive && target.owner != owner && target.battleAttackerOwner != owner) {


    }
  }

  private void checkWinLose() {
    int owned = getOwnedCount();
    if (owned == 0 && resultState == null) {
      resultState = GameView.GameState.LOSE;
      playing = false;
    } else if (owned == prefectures.size() && resultState == null) {
      resultState = GameView.GameState.WIN;
      playing = false;
    }
  }

  public int getOwnedCount() {
    int count = 0;
    for (int i = 0; i < prefectures.size(); i++) {
      if (prefectures.get(i).owner == 1) {
        count++;
      }
    }
    return count;
  }

  public int getTotalCount() {
    return prefectures.size();
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

  private void selectPrefecture(Prefecture pref) {
    clearSelection();
    pref.selected = true;
  }

  private void clearSelection() {
    for (int i = 0; i < prefectures.size(); i++) {
      prefectures.get(i).selected = false;
    }
  }

  private Prefecture getSelectedPrefecture() {
    for (int i = 0; i < prefectures.size(); i++) {
      if (prefectures.get(i).selected) {
        return prefectures.get(i);
      }
    }
    return null;
  }

  private Prefecture findPrefecture(float x, float y) {
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      if (pref.containsPoint(x, y)) {
        return pref;
      }
    }
    return null;
  }

  private void generateSkirmish() {
    buildJapanMap();
    int aiCount = 1 + random.nextInt(3);
    aiTimers = new float[aiCount];
    aiIntervals = new float[aiCount];
    for (int i = 0; i < aiCount; i++) {
      aiIntervals[i] = randomInterval();
    }
    // 3
    int playerCount = 0;
    for (Prefecture pref : prefectures) {
      if (pref.owner == 1) {
        seedGarrison(pref, 30);
        playerCount++;
      }
    }

    if (playerCount == 0 && !prefectures.isEmpty()) {
      prefectures.get(0).owner = 1;
      seedGarrison(prefectures.get(0), 30);
    }
    // AI
    int aiIndex = 0;
    for (Prefecture pref : prefectures) {
      if (pref.owner == 0 && aiIndex < aiCount) {
        pref.owner = aiIndex + 2;
        seedGarrison(pref, 25 + random.nextInt(10));
        aiIndex++;
      }
    }
  }

  private void buildJapanMap() {
    // Voronoi

    prefectures.clear();
    int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
    int screenHeight = context.getResources().getDisplayMetrics().heightPixels;


    float mapWidth = screenWidth * 2.0f;
    float mapHeight = screenHeight * 1.4f;
    worldMapWidth = mapWidth;
    worldMapHeight = mapHeight;

    // 0~1

    String[] cityNames = {
      "Beijing", "Tianjin", "Shenyang", "Changchun", "Harbin",
      "Hohhot", "Shijiazhuang", "Taiyuan", "Jinan", "Qingdao",
      "Zhengzhou", "Xi'an", "Lanzhou", "Urumqi",
      "Shanghai", "Nanjing", "Hefei", "Hangzhou", "Nanchang", "Fuzhou", "Xiamen",
      "Wuhan", "Changsha", "Guiyang", "Chongqing", "Chengdu", "Kunming",
      "Guangzhou", "Shenzhen", "Nanning", "Haikou",
      // Additional cities
      "Dalian", "Tangshan", "Baoding", "Handan", "Luoyang",
      "Xuzhou", "Suzhou", "Wuxi", "Ningbo", "Wenzhou",
      "Quanzhou", "Shantou", "Zhuhai", "Foshan", "Dongguan",
      "Guilin", "Liuzhou", "Beihai", "Sanya", "Lhasa"
    };

    // Coordinates are approximate outline: more in southeast, less in northwest, forming a "China" shape
    float[] xs = {
      0.63f, 0.67f, 0.74f, 0.78f, 0.82f,
      0.55f, 0.58f, 0.52f, 0.69f, 0.73f,
      0.60f, 0.54f, 0.40f, 0.25f,
      0.78f, 0.72f, 0.66f, 0.75f, 0.68f, 0.80f, 0.83f,
      0.63f, 0.62f, 0.53f, 0.59f, 0.54f, 0.50f,
      0.70f, 0.74f, 0.58f, 0.76f,
      
      0.76f, 0.65f, 0.59f, 0.61f, 0.58f,
      0.70f, 0.76f, 0.78f, 0.77f, 0.79f,
      0.81f, 0.72f, 0.75f, 0.73f, 0.76f,
      0.57f, 0.56f, 0.59f, 0.77f, 0.35f
    };
    float[] ys = {
      0.20f, 0.23f, 0.18f, 0.15f, 0.12f,
      0.26f, 0.30f, 0.28f, 0.34f, 0.37f,
      0.38f, 0.40f, 0.36f, 0.30f,
      0.42f, 0.40f, 0.44f, 0.46f, 0.50f, 0.52f, 0.56f,
      0.46f, 0.50f, 0.54f, 0.48f, 0.44f, 0.56f,
      0.60f, 0.63f, 0.58f, 0.68f,
      
      0.19f, 0.29f, 0.31f, 0.35f, 0.37f,
      0.41f, 0.43f, 0.45f, 0.47f, 0.49f,
      0.51f, 0.61f, 0.64f, 0.62f, 0.65f,
      0.55f, 0.57f, 0.60f, 0.70f, 0.42f
    };

    int count = Math.min(cityNames.length, xs.length);

    int cols = 10;
    int rows = 6;
    float cellW = mapWidth * 0.075f;
    float cellH = mapHeight * 0.11f;
    float halfW = cellW * 0.45f;
    float halfH = cellH * 0.45f;
    boolean[][] occupied = new boolean[rows][cols];

    for (int i = 0; i < count; i++) {
      
      int idealCol = Math.min(cols - 1, Math.max(0, (int) (xs[i] * cols)));
      int idealRow = Math.min(rows - 1, Math.max(0, (int) (ys[i] * rows)));


      int bestCol = idealCol;
      int bestRow = idealRow;
      if (occupied[bestRow][bestCol]) {
        int maxR = Math.max(cols, rows);
        outer:
        for (int r = 1; r < maxR; r++) {
          for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
              if (Math.abs(dx) != r && Math.abs(dy) != r) {
                continue;
              }
              int c = idealCol + dx;
              int rr = idealRow + dy;
              if (c < 0 || c >= cols || rr < 0 || rr >= rows) {
                continue;
              }
              if (!occupied[rr][c]) {
                bestCol = c;
                bestRow = rr;
                occupied[rr][c] = true;
                break outer;
              }
            }
          }
        }
      } else {
        occupied[bestRow][bestCol] = true;
      }


      float startX = mapWidth * 0.20f;
      float startY = mapHeight * 0.18f;
      float cx = startX + (bestCol + 0.5f) * cellW;
      float cy = startY + (bestRow + 0.5f) * cellH;

      float radius = Math.min(cellW, cellH) * 0.5f;

      Prefecture pref = new Prefecture(cx, cy, radius, cityNames[i], Prefecture.TYPE_PREFECTURE);
      pref.id = i;
      pref.terrainType = Prefecture.TERRAIN_CITY;


      pref.polygonVertexCount = 4;
      pref.polygonPoints = new float[8];
      pref.polygonPoints[0] = cx - halfW;
      pref.polygonPoints[1] = cy - halfH;
      pref.polygonPoints[2] = cx + halfW;
      pref.polygonPoints[3] = cy - halfH;
      pref.polygonPoints[4] = cx + halfW;
      pref.polygonPoints[5] = cy + halfH;
      pref.polygonPoints[6] = cx - halfW;
      pref.polygonPoints[7] = cy + halfH;

      seedGarrison(pref, 15 + random.nextInt(15));
      pref.owner = 0;
      pref.growthRate = 2f + random.nextFloat() * 1.5f;
      pref.hasAttackedThisTurn = false;  
      // 3
      if (i < 3) {
        pref.owner = 1;
        pref.goldIncome = 5f + random.nextFloat() * 3f;  
      }
      prefectures.add(pref);
    }


    rebuildNeighbors(Math.min(mapWidth, mapHeight) * 0.12f, 4);
  }
  
  private boolean isTooCloseToSeeds(List<float[]> seeds, float x, float y, float minDist) {
    for (float[] seed : seeds) {
      float dx = x - seed[0];
      float dy = y - seed[1];
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      if (dist < minDist) {
        return true;
      }
    }
    return false;
  }
  
  private void generateVoronoiStylePolygons() {
    // prefecture
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      
      
      List<Prefecture> neighbors = new ArrayList<>();
      for (int j = 0; j < prefectures.size(); j++) {
        if (i == j) {
          continue;
        }
        Prefecture other = prefectures.get(j);
        float dx = other.x - pref.x;
        float dy = other.y - pref.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < (pref.radius + other.radius) * 1.5f) {
          neighbors.add(other);
        }
      }
      

      int vertexCount = 6 + random.nextInt(4); // 6-9
      pref.polygonVertexCount = vertexCount;
      pref.polygonPoints = new float[vertexCount * 2];
      

      float angleStep = (float)(Math.PI * 2.0 / vertexCount);
      for (int j = 0; j < vertexCount; j++) {
        float angle = angleStep * j;
        float baseRadius = pref.radius * 1.1f;
        
        
        float minNeighborDist = baseRadius * 2f;
        for (Prefecture neighbor : neighbors) {
          float toNeighbor = (float) Math.atan2(neighbor.y - pref.y, neighbor.x - pref.x);
          float angleDiff = Math.abs(angle - toNeighbor);
          if (angleDiff > Math.PI) {
            angleDiff = (float)(Math.PI * 2 - angleDiff);
          }
          if (angleDiff < Math.PI / 3) { // 60
            float dist = (float) Math.sqrt(
              (neighbor.x - pref.x) * (neighbor.x - pref.x) +
              (neighbor.y - pref.y) * (neighbor.y - pref.y)
            );
            float sharedRadius = (pref.radius + neighbor.radius) * 0.5f;
            if (dist < sharedRadius * 2f) {
              minNeighborDist = Math.min(minNeighborDist, sharedRadius);
            }
          }
        }
        
        float currentRadius = Math.min(baseRadius, minNeighborDist * 0.9f);
        float rVariation = 0.85f + random.nextFloat() * 0.3f;
        currentRadius *= rVariation;
        
        pref.polygonPoints[j * 2] = pref.x + (float)Math.cos(angle) * currentRadius;
        pref.polygonPoints[j * 2 + 1] = pref.y + (float)Math.sin(angle) * currentRadius;
      }
    }
  }
  
  private void adjustPolygonsForNoOverlap() {

    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture a = prefectures.get(i);
      if (a.polygonPoints == null) {
        continue;
      }
      
      
      float minDist = Float.MAX_VALUE;
      for (int j = 0; j < prefectures.size(); j++) {
        if (i == j) {
          continue;
        }
        Prefecture b = prefectures.get(j);
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < minDist) {
          minDist = dist;
        }
      }
      

      if (minDist < a.radius * 2.5f) {
        float scale = Math.min(1f, minDist / (a.radius * 2.5f));
        for (int j = 0; j < a.polygonVertexCount; j++) {
          float px = a.polygonPoints[j * 2];
          float py = a.polygonPoints[j * 2 + 1];
          
          float newX = a.x + (px - a.x) * scale;
          float newY = a.y + (py - a.y) * scale;
          a.polygonPoints[j * 2] = newX;
          a.polygonPoints[j * 2 + 1] = newY;
        }
        a.radius *= scale; 
      }
    }
  }

  private void rebuildNeighbors(float maxDist, int maxPerNode) {
    int n = prefectures.size();
    boolean[][] links = new boolean[n][n];
    for (int i = 0; i < n; i++) {
      Prefecture a = prefectures.get(i);
      for (int k = 0; k < maxPerNode; k++) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int j = 0; j < n; j++) {
          if (i == j) {
            continue;
          }
          if (links[i][j]) {
            continue;
          }
          Prefecture b = prefectures.get(j);
          float dx = b.x - a.x;
          float dy = b.y - a.y;
          float d = (float) Math.sqrt(dx * dx + dy * dy);
          if (d > maxDist) {
            continue;
          }
          if (d < bestD) {
            bestD = d;
            best = j;
          }
        }
        if (best >= 0) {
          links[i][best] = true;
          links[best][i] = true;
        }
      }
    }

    for (int i = 0; i < n; i++) {
      int count = 0;
      for (int j = 0; j < n; j++) {
        if (links[i][j]) {
          count++;
        }
      }
      int[] arr = new int[count];
      int idx = 0;
      for (int j = 0; j < n; j++) {
        if (links[i][j]) {
          arr[idx] = j;
          idx++;
        }
      }
      prefectures.get(i).neighbors = arr;
    }
  }

  private void seedGarrison(Prefecture pref, int total) {
    int s = total * 35 / 100;
    int w = total * 45 / 100;
    int a = total - s - w;
    pref.shield = s;
    pref.sword = w;
    pref.archer = a;
    
    pref.gold = 0;
    if (pref.owner == 1 && pref.goldIncome <= 0f) {
      pref.goldIncome = 5f + random.nextFloat() * 3f;
    }
    pref.buildingType = Prefecture.BUILDING_NONE;
    
    pref.hasAttackedThisTurn = false;
    pref.specialEffect = Prefecture.EFFECT_NONE;
    
    if (random.nextFloat() < 0.15f && pref.type == Prefecture.TYPE_CITY) {
      int[] buildings = {
        Prefecture.BUILDING_CANNON,
        Prefecture.BUILDING_BARRACKS,
        Prefecture.BUILDING_FORTRESS,
        Prefecture.BUILDING_TRAINING
      };
      pref.buildingType = buildings[random.nextInt(buildings.length)];
      switch (pref.buildingType) {
        case Prefecture.BUILDING_BARRACKS:
          pref.specialEffect = Prefecture.EFFECT_RAPID_GEN;
          pref.growthRate *= 1.5f;
          break;
        case Prefecture.BUILDING_FORTRESS:
          pref.specialEffect = Prefecture.EFFECT_DEFENSE_BOOST;
          break;
        case Prefecture.BUILDING_TRAINING:
          pref.specialEffect = Prefecture.EFFECT_GIANT_SPAWN;
          break;
      }
    }
  }

  private void loadLevel(LevelData level) {
    prefectures.clear();
    particles.clear();
    elapsedTime = 0f;
    aiTimers = new float[level.aiCount];
    aiIntervals = new float[level.aiCount];
    for (int i = 0; i < level.aiCount; i++) {
      aiIntervals[i] = randomInterval();
    }
    int width = context.getResources().getDisplayMetrics().widthPixels;
    int height = context.getResources().getDisplayMetrics().heightPixels;
    String[] prefectureNames = {
      "Tokyo", "Osaka", "Kyoto", "Yokohama", "Nagoya", "Sapporo", "Fukuoka",
      "Kobe", "Sendai", "Hiroshima", "Chiba", "Saitama", "Niigata", "Shizuoka"
    };
    for (int i = 0; i < level.nodes.size(); i++) {
      LevelNode spec = level.nodes.get(i);
      String name = i < prefectureNames.length ? prefectureNames[i] : "Prefecture";
      Prefecture pref = new Prefecture(spec.x * width, spec.y * height, spec.radius * width, name, Prefecture.TYPE_PREFECTURE);
      pref.id = i;
      pref.owner = spec.owner;
      seedGarrison(pref, spec.units);
      pref.growthRate = spec.growth;
      prefectures.add(pref);
    }
    rebuildNeighbors(Math.min(width, height) * 0.28f, 3);
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
