package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.core.GameState;
import com.google.android.material.button.MaterialButton;
import com.android.boot.ui.GameView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private View playOverlay;
    private View menuOverlay;
    private View strategyOverlay;
    private View pauseOverlay;
    private View resultOverlay;
    private View helpOverlay;
    private View btnPause;
    private View bottomActionBar;
    private TextView hudHealth;
    private TextView hudInfection;
    private TextView hudFuel;
    private TextView hudLoot;
    private TextView hudSurvivors;
    private TextView hudRegion;
    private TextView hudObjective;
    private TextView hudWeapon;
    private TextView hudAmmo;
    private TextView hudUtility;
    private TextView hudStatus;
    private TextView strategyTitle;
    private TextView strategySummary;
    private TextView strategyHint;
    private TextView pauseSummary;
    private TextView resultTitle;
    private TextView resultBody;
    private MaterialButton btnMute;
    private GridLayout regionGrid;
    private final List<MaterialButton> regionButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        bindViews();
        wireButtons();
        gameView.setListener(this::syncUi);
        buildRegionButtons();
        syncUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
    }

    @Override
    protected void onPause() {
        gameView.onHostPause();
        super.onPause();
    }

    private void bindViews() {
        playOverlay = findViewById(R.id.play_overlay);
        menuOverlay = findViewById(R.id.menu_overlay);
        strategyOverlay = findViewById(R.id.strategy_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        resultOverlay = findViewById(R.id.result_overlay);
        helpOverlay = findViewById(R.id.help_overlay);
        btnPause = findViewById(R.id.btn_pause);
        bottomActionBar = findViewById(R.id.bottom_action_bar);
        hudHealth = findViewById(R.id.hud_health);
        hudInfection = findViewById(R.id.hud_infection);
        hudFuel = findViewById(R.id.hud_fuel);
        hudLoot = findViewById(R.id.hud_loot);
        hudSurvivors = findViewById(R.id.hud_survivors);
        hudRegion = findViewById(R.id.hud_region);
        hudObjective = findViewById(R.id.hud_objective);
        hudWeapon = findViewById(R.id.hud_weapon);
        hudAmmo = findViewById(R.id.hud_ammo);
        hudUtility = findViewById(R.id.hud_utility);
        hudStatus = findViewById(R.id.hud_status);
        strategyTitle = findViewById(R.id.strategy_title);
        strategySummary = findViewById(R.id.strategy_summary);
        strategyHint = findViewById(R.id.strategy_hint);
        pauseSummary = findViewById(R.id.pause_summary);
        resultTitle = findViewById(R.id.result_title);
        resultBody = findViewById(R.id.result_body);
        btnMute = findViewById(R.id.btn_mute);
        regionGrid = findViewById(R.id.region_grid);
    }

    private void wireButtons() {
        findViewById(R.id.btn_start).setOnClickListener(v -> {
            gameView.openWorldMap();
            syncUi();
        });
        findViewById(R.id.btn_how_to_play).setOnClickListener(v -> helpOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.btn_close_help).setOnClickListener(v -> helpOverlay.setVisibility(View.GONE));
        btnMute.setOnClickListener(v -> {
            gameView.setMuted(!gameView.isMuted());
            syncUi();
        });
        findViewById(R.id.btn_pause).setOnClickListener(v -> {
            gameView.pauseGame();
            syncUi();
        });
        findViewById(R.id.btn_resume).setOnClickListener(v -> {
            gameView.resumeGame();
            syncUi();
        });
        findViewById(R.id.btn_restart).setOnClickListener(v -> {
            gameView.retryRegion();
            syncUi();
        });
        findViewById(R.id.btn_pause_menu).setOnClickListener(v -> {
            gameView.returnToMenu();
            syncUi();
        });
        findViewById(R.id.btn_result_retry).setOnClickListener(v -> {
            gameView.retryRegion();
            syncUi();
        });
        findViewById(R.id.btn_result_safehouse).setOnClickListener(v -> {
            gameView.openSafehouse();
            syncUi();
        });
        findViewById(R.id.btn_result_menu).setOnClickListener(v -> {
            gameView.returnToMenu();
            syncUi();
        });
        findViewById(R.id.btn_workshop).setOnClickListener(v -> {
            gameView.purchaseUpgrade("workshop");
            syncUi();
        });
        findViewById(R.id.btn_infirmary).setOnClickListener(v -> {
            gameView.purchaseUpgrade("infirmary");
            syncUi();
        });
        findViewById(R.id.btn_garage).setOnClickListener(v -> {
            gameView.purchaseUpgrade("garage");
            syncUi();
        });
        findViewById(R.id.btn_safehouse).setOnClickListener(v -> {
            gameView.openSafehouse();
            syncUi();
        });
        findViewById(R.id.btn_back_menu).setOnClickListener(v -> {
            gameView.returnToMenu();
            syncUi();
        });
        findViewById(R.id.btn_reload).setOnClickListener(v -> gameView.triggerReload());
        findViewById(R.id.btn_heal).setOnClickListener(v -> gameView.triggerHeal());
        findViewById(R.id.btn_interact).setOnClickListener(v -> gameView.triggerInteract());
        findViewById(R.id.btn_bike).setOnClickListener(v -> gameView.triggerBikeToggle());
        findViewById(R.id.btn_utility).setOnClickListener(v -> gameView.triggerUtility());
        findViewById(R.id.btn_switch_weapon).setOnClickListener(v -> gameView.triggerSwitchWeapon());
    }

    private void buildRegionButtons() {
        regionGrid.removeAllViews();
        regionButtons.clear();
        for (int i = 0; i < gameView.getRegionCount(); i++) {
            final int index = i;
            MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setTextAppearance(this, R.style.TextAppearance_Game_Body);
            button.setBackgroundResource(R.drawable.ui_button_secondary);
            button.setTextColor(getColor(R.color.cst_text_on_secondary));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            button.setLayoutParams(params);
            button.setOnClickListener(v -> {
                if (gameView.canEnterRegion(index)) {
                    gameView.startRegion(index);
                    syncUi();
                }
            });
            regionButtons.add(button);
            regionGrid.addView(button);
        }
    }

    private void syncUi() {
        GameView.HudSnapshot hud = gameView.getHudSnapshot();
        GameView.SafehouseSnapshot safehouse = gameView.getSafehouseSnapshot();
        GameState state = gameView.getState();

        hudHealth.setText(hud.healthText);
        hudInfection.setText(hud.infectionText);
        hudFuel.setText(hud.fuelText);
        hudLoot.setText(hud.lootText);
        hudSurvivors.setText(hud.survivorText);
        hudRegion.setText(hud.regionText);
        hudObjective.setText(hud.objectiveText);
        hudWeapon.setText(hud.weaponText);
        hudAmmo.setText(hud.ammoText);
        hudUtility.setText(hud.utilityText);
        hudStatus.setText(hud.statusText);

        strategyTitle.setText(state == GameState.WORLD_MAP ? getString(R.string.label_world_map) : getString(R.string.label_safehouse));
        strategySummary.setText(safehouse.summaryText);
        strategyHint.setText(safehouse.hintText);
        pauseSummary.setText(hud.pauseText);
        resultTitle.setText(gameView.getResultTitle());
        resultBody.setText(gameView.getResultBody());
        btnMute.setText(getString(R.string.btn_mute) + ": " + (gameView.isMuted() ? "Off" : "On"));

        for (int i = 0; i < regionButtons.size(); i++) {
            regionButtons.get(i).setText(gameView.getRegionLabel(i));
            regionButtons.get(i).setEnabled(gameView.canEnterRegion(i));
        }

        boolean playing = state == GameState.PLAYING;
        boolean paused = state == GameState.PAUSED;
        playOverlay.setVisibility((playing || paused) ? View.VISIBLE : View.GONE);
        btnPause.setVisibility(playing ? View.VISIBLE : View.GONE);
        bottomActionBar.setVisibility(playing ? View.VISIBLE : View.GONE);
        menuOverlay.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
        strategyOverlay.setVisibility((state == GameState.WORLD_MAP || state == GameState.SAFEHOUSE) ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(paused ? View.VISIBLE : View.GONE);
        resultOverlay.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    }
}
