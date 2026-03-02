package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;
import java.util.ArrayList;
import java.util.List;
import android.os.SystemClock;

public class MainActivity extends AppCompatActivity {
  private static class CardSpec {
    String name;
    int energy;
    float cooldown;
    String role;
    String desc;
    int unlockLevel;
  }

  private GameView gameView;
  private LinearLayout levelPanel;
  private LinearLayout deckPanel;
  private LinearLayout pausePanel;
  private LinearLayout gameOverPanel;
  private LinearLayout upgradePanel;
  private LinearLayout poolList;
  private LinearLayout selectedSlots;
  private View skillRow;
  private ImageButton btnPause;
  private TextView hudWave;
  private TextView hudScore;
  private TextView hudGold;
  private TextView hudHp;
  private TextView cardDetail;
  private Button btnUpgradeA;
  private Button btnUpgradeB;
  private Button btnUpgradeC;
  private final List<CardSpec> allCards = new ArrayList<>();
  private final List<CardSpec> unlockedCards = new ArrayList<>();
  private final List<CardSpec> selectedCards = new ArrayList<>();
  private final List<Button> cardButtons = new ArrayList<>();
  private int selectedLevel = 1;
  private final List<Long> attackPlacedUntil = new ArrayList<>();
  private final List<Long> blockPlacedUntil = new ArrayList<>();
  private final List<Long> supportPlacedUntil = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    buildCardPool();
    bindViews();
    bindButtons();
    gameView.setListener(new GameView.Listener() {
      @Override
      public void onHud(GameEngine.HudData hud, float oilCd, float freezeCd, float pushCd) {
        runOnUiThread(() -> {
          hudWave.setText(getString(R.string.label_wave) + " " + hud.wave);
          hudScore.setText(getString(R.string.label_score) + " " + hud.score);
          hudGold.setText(getString(R.string.label_gold) + " " + hud.gold + "  " + getString(R.string.label_energy) + " " + hud.energy);
          hudHp.setText(getString(R.string.label_hp) + " " + hud.hp + "/" + hud.maxHp);
          refreshCardBar(oilCd, freezeCd, pushCd, hud.energy);
        });
      }

      @Override
      public void onState(GameState state, boolean upgradeVisible, GameEngine.UpgradeChoice choice) {
        runOnUiThread(() -> {
          if (upgradeVisible) {
            upgradePanel.setVisibility(View.VISIBLE);
            btnUpgradeA.setText(choice.a);
            btnUpgradeB.setText(choice.b);
            btnUpgradeC.setText(choice.c);
          } else {
            upgradePanel.setVisibility(View.GONE);
          }
          renderState(state);
        });
      }
    });
    renderState(GameState.LEVEL_SELECT);
  }

  private void buildCardPool() {
    addCard("Collector", 18, 6f, "Support", "Generates bonus resources.", 1);
    addCard("DartPod", 14, 2.4f, "Attack", "Rapid single target pod.", 1);
    addCard("BarrierTile", 22, 7f, "Block", "Reinforce keep defenses.", 1);
    addCard("FreezeMist", 30, 10f, "Support", "Slow a random lane.", 1);
    addCard("StoneThrower", 20, 4f, "Attack", "Heavy burst projectile.", 1);
    addCard("BurstPod", 30, 8f, "Attack", "Triple burst volley.", 2);
    addCard("ShockCoil", 26, 8f, "Attack", "Charged strike enhancer.", 3);
    addCard("MedicSprout", 28, 10f, "Support", "Restore keep health.", 4);
    addCard("SpikePatch", 20, 7f, "Block", "Creates a control patch.", 4);
    addCard("Beacon", 18, 11f, "Support", "Converts to instant energy.", 5);
  }

  private void addCard(String name, int energy, float cooldown, String role, String desc, int unlockLevel) {
    CardSpec c = new CardSpec();
    c.name = name;
    c.energy = energy;
    c.cooldown = cooldown;
    c.role = role;
    c.desc = desc;
    c.unlockLevel = unlockLevel;
    allCards.add(c);
  }

  private void bindViews() {
    gameView = findViewById(R.id.game_view);
    levelPanel = findViewById(R.id.level_panel);
    deckPanel = findViewById(R.id.deck_panel);
    pausePanel = findViewById(R.id.pause_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    upgradePanel = findViewById(R.id.upgrade_panel);
    poolList = findViewById(R.id.pool_list);
    selectedSlots = findViewById(R.id.selected_slots);
    skillRow = findViewById(R.id.skill_row);
    btnPause = findViewById(R.id.btn_pause);
    hudWave = findViewById(R.id.hud_wave);
    hudScore = findViewById(R.id.hud_score);
    hudGold = findViewById(R.id.hud_gold);
    hudHp = findViewById(R.id.hud_hp);
    cardDetail = findViewById(R.id.card_detail);
    btnUpgradeA = findViewById(R.id.btn_upgrade_a);
    btnUpgradeB = findViewById(R.id.btn_upgrade_b);
    btnUpgradeC = findViewById(R.id.btn_upgrade_c);
    cardButtons.add(findViewById(R.id.btn_card_1));
    cardButtons.add(findViewById(R.id.btn_card_2));
    cardButtons.add(findViewById(R.id.btn_card_3));
    cardButtons.add(findViewById(R.id.btn_card_4));
    cardButtons.add(findViewById(R.id.btn_card_5));
    cardButtons.add(findViewById(R.id.btn_card_6));
  }

  private void bindButtons() {
    findViewById(R.id.btn_level_1).setOnClickListener(v -> openDeck(1));
    findViewById(R.id.btn_level_2).setOnClickListener(v -> openDeck(2));
    findViewById(R.id.btn_level_3).setOnClickListener(v -> openDeck(3));
    findViewById(R.id.btn_level_4).setOnClickListener(v -> openDeck(4));
    findViewById(R.id.btn_level_5).setOnClickListener(v -> openDeck(5));
    findViewById(R.id.btn_back).setOnClickListener(v -> renderState(GameState.LEVEL_SELECT));
    findViewById(R.id.btn_clear).setOnClickListener(v -> {
      selectedCards.clear();
      renderSelectedSlots();
      bindCardBar();
    });
    findViewById(R.id.btn_auto_pick).setOnClickListener(v -> autoPick());
    findViewById(R.id.btn_start).setOnClickListener(v -> {
      if (selectedCards.size() == 6) {
        attackPlacedUntil.clear();
        blockPlacedUntil.clear();
        supportPlacedUntil.clear();
        gameView.getEngine().startGame();
      }
    });
    findViewById(R.id.btn_resume).setOnClickListener(v -> gameView.getEngine().resume());
    findViewById(R.id.btn_menu).setOnClickListener(v -> renderState(GameState.LEVEL_SELECT));
    findViewById(R.id.btn_restart).setOnClickListener(v -> renderState(GameState.DECK_SELECT));
    btnPause.setOnClickListener(v -> gameView.getEngine().pause());
    btnUpgradeA.setOnClickListener(v -> gameView.getEngine().chooseUpgrade(0));
    btnUpgradeB.setOnClickListener(v -> gameView.getEngine().chooseUpgrade(1));
    btnUpgradeC.setOnClickListener(v -> gameView.getEngine().chooseUpgrade(2));
  }

  private void openDeck(int level) {
    selectedLevel = level;
    unlockedCards.clear();
    for (CardSpec c : allCards) {
      if (c.unlockLevel <= level) {
        unlockedCards.add(c);
      }
    }
    selectedCards.clear();
    renderPoolList();
    renderSelectedSlots();
    bindCardBar();
    renderState(GameState.DECK_SELECT);
  }

  private void autoPick() {
    selectedCards.clear();
    for (int i = 0; i < unlockedCards.size() && selectedCards.size() < 6; i++) {
      selectedCards.add(unlockedCards.get(i));
    }
    while (selectedCards.size() < 6) {
      selectedCards.add(unlockedCards.get(unlockedCards.size() - 1));
    }
    renderSelectedSlots();
    bindCardBar();
  }

  private void renderPoolList() {
    poolList.removeAllViews();
    for (CardSpec c : unlockedCards) {
      Button b = new Button(this);
      b.setText(c.name + "  " + c.role + "  E" + c.energy);
      b.setOnClickListener(v -> {
        if (selectedCards.size() < 6) {
          selectedCards.add(c);
          renderSelectedSlots();
          bindCardBar();
        }
      });
      b.setOnLongClickListener(v -> {
        cardDetail.setText(c.name + "\nEnergy: " + c.energy + "\nCooldown: " + c.cooldown + "s\nRole: " + c.role + "\n" + c.desc);
        cardDetail.setVisibility(View.VISIBLE);
        cardDetail.postDelayed(() -> cardDetail.setVisibility(View.GONE), 2200);
        return true;
      });
      poolList.addView(b);
    }
  }

  private void renderSelectedSlots() {
    selectedSlots.removeAllViews();
    for (int i = 0; i < 6; i++) {
      Button b = new Button(this);
      if (i < selectedCards.size()) {
        CardSpec c = selectedCards.get(i);
        b.setText((i + 1) + ". " + c.name + " [" + c.role + "]");
        int index = i;
        b.setOnClickListener(v -> {
          selectedCards.remove(index);
          renderSelectedSlots();
          bindCardBar();
        });
      } else {
        b.setText((i + 1) + ". Empty");
      }
      selectedSlots.addView(b);
    }
  }

  private void bindCardBar() {
    for (int i = 0; i < cardButtons.size(); i++) {
      Button b = cardButtons.get(i);
      if (i < selectedCards.size()) {
        CardSpec c = selectedCards.get(i);
        b.setText(c.name + " E" + c.energy);
        b.setVisibility(View.VISIBLE);
        b.setOnClickListener(v -> playCard(c));
      } else {
        b.setVisibility(View.GONE);
      }
    }
  }

  private void playCard(CardSpec c) {
    prunePlaced();
    if (!hasRoleCapacity(c.role)) {
      return;
    }
    if ("Oil".equals(c.name) || "SpikePatch".equals(c.name)) {
      gameView.getEngine().useOil();
    } else if ("FreezeMist".equals(c.name)) {
      gameView.getEngine().useFreeze();
    } else if ("Beacon".equals(c.name) || "BurstPod".equals(c.name) || "ShockCoil".equals(c.name) || "DartPod".equals(c.name) || "StoneThrower".equals(c.name)) {
      gameView.getEngine().usePush(gameView.getLastAimX(), gameView.getLastAimY());
    } else if ("MedicSprout".equals(c.name) || "BarrierTile".equals(c.name) || "Collector".equals(c.name)) {
      gameView.getEngine().useOil();
    }
    markPlaced(c.role);
  }

  private void markPlaced(String role) {
    long until = SystemClock.uptimeMillis() + 18000L;
    if ("Attack".equals(role)) {
      attackPlacedUntil.add(until);
    } else if ("Block".equals(role)) {
      blockPlacedUntil.add(until);
    } else {
      supportPlacedUntil.add(until);
    }
  }

  private boolean hasRoleCapacity(String role) {
    if ("Attack".equals(role)) {
      return attackPlacedUntil.size() < 25;
    }
    if ("Block".equals(role)) {
      return blockPlacedUntil.size() < 10;
    }
    return supportPlacedUntil.size() < 8;
  }

  private void prunePlaced() {
    long now = SystemClock.uptimeMillis();
    prune(attackPlacedUntil, now);
    prune(blockPlacedUntil, now);
    prune(supportPlacedUntil, now);
  }

  private void prune(List<Long> list, long now) {
    for (int i = list.size() - 1; i >= 0; i--) {
      if (list.get(i) <= now) {
        list.remove(i);
      }
    }
  }

  private void refreshCardBar(float oilCd, float freezeCd, float pushCd, int energy) {
    prunePlaced();
    for (int i = 0; i < selectedCards.size() && i < cardButtons.size(); i++) {
      CardSpec c = selectedCards.get(i);
      float cd = 0f;
      if ("FreezeMist".equals(c.name)) {
        cd = freezeCd;
      } else if ("SpikePatch".equals(c.name) || "MedicSprout".equals(c.name) || "BarrierTile".equals(c.name) || "Collector".equals(c.name)) {
        cd = oilCd;
      } else {
        cd = pushCd;
      }
      Button b = cardButtons.get(i);
      if (cd > 0f) {
        b.setText(c.name + " " + (int) (cd * 100f) + "%");
      } else {
        b.setText(c.name + " E" + c.energy);
      }
      boolean enabled = energy >= c.energy && cd <= 0f && hasRoleCapacity(c.role);
      b.setEnabled(enabled);
      b.setAlpha(enabled ? 1f : 0.45f);
    }
  }

  private void renderState(GameState state) {
    boolean playing = state == GameState.PLAYING;
    levelPanel.setVisibility(state == GameState.LEVEL_SELECT ? View.VISIBLE : View.GONE);
    deckPanel.setVisibility(state == GameState.DECK_SELECT ? View.VISIBLE : View.GONE);
    pausePanel.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
    gameOverPanel.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    skillRow.setVisibility(playing ? View.VISIBLE : View.GONE);
    btnPause.setVisibility(playing ? View.VISIBLE : View.GONE);
    if (state == GameState.LEVEL_SELECT) {
      gameView.getEngine().goToMenu();
      cardDetail.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseLoop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.resumeLoop();
  }
}
