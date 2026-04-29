package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.boot.core.CastawayState;
import com.android.boot.core.CastawayWorld;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GameView gameView;
    private TextView tvFood;
    private TextView tvWater;
    private TextView tvMorale;
    private TextView tvPopulation;
    private TextView tvDay;
    private TextView tvWeather;
    private TextView tvHint;
    private TextView tvObjective;
    private Button btnBuild;
    private Button btnAssign;
    private Button btnCraft;
    private Button btnMap;
    private FrameBindings bindings;
    private final Runnable uiTicker = new Runnable() {
        @Override
        public void run() {
            if (gameView != null) {
                updateUi(gameView.snapshot());
            }
            handler.postDelayed(this, 160L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindings = new FrameBindings();
        gameView = findViewById(R.id.game_view);
        tvFood = findViewById(R.id.tv_food);
        tvWater = findViewById(R.id.tv_water);
        tvMorale = findViewById(R.id.tv_morale);
        tvPopulation = findViewById(R.id.tv_population);
        tvDay = findViewById(R.id.tv_day);
        tvWeather = findViewById(R.id.tv_weather);
        tvHint = findViewById(R.id.tv_hint);
        tvObjective = findViewById(R.id.tv_objective);
        btnBuild = findViewById(R.id.btn_build);
        btnAssign = findViewById(R.id.btn_assign);
        btnCraft = findViewById(R.id.btn_craft);
        btnMap = findViewById(R.id.btn_map);
        ImageButton btnPause = findViewById(R.id.btn_pause);
        ImageButton btnMute = findViewById(R.id.btn_mute);
        Button btnObjective = findViewById(R.id.btn_objective);
        bindings.menuOverlay = findViewById(R.id.menu_overlay);
        bindings.pauseOverlay = findViewById(R.id.pause_overlay);
        bindings.resultOverlay = findViewById(R.id.result_overlay);
        bindings.resultTitle = findViewById(R.id.tv_result_title);
        bindings.resultBody = findViewById(R.id.tv_result_body);
        Button btnStart = findViewById(R.id.btn_start_overlay);
        Button btnHelp = findViewById(R.id.btn_help_overlay);
        Button btnResume = findViewById(R.id.btn_resume_overlay);
        Button btnRestart = findViewById(R.id.btn_restart_overlay);
        Button btnMenu = findViewById(R.id.btn_menu_overlay);
        Button btnResultRestart = findViewById(R.id.btn_result_restart);
        Button btnResultMenu = findViewById(R.id.btn_result_menu);

        btnBuild.setOnClickListener(v -> gameView.cycleBuildType());
        btnAssign.setOnClickListener(v -> gameView.reassignSurvivor());
        btnCraft.setOnClickListener(v -> gameView.craftSupplies());
        btnMap.setOnClickListener(v -> gameView.toggleMapFocus());
        btnObjective.setOnClickListener(v -> gameView.advanceObjectiveCard());
        btnPause.setOnClickListener(v -> gameView.togglePause());
        btnMute.setOnClickListener(v -> gameView.toggleMute());
        btnStart.setOnClickListener(v -> gameView.startGame());
        btnHelp.setOnClickListener(v -> tvHint.setText(R.string.help_short));
        btnResume.setOnClickListener(v -> gameView.togglePause());
        btnRestart.setOnClickListener(v -> gameView.restartRun());
        btnMenu.setOnClickListener(v -> gameView.openMenu());
        btnResultRestart.setOnClickListener(v -> gameView.restartRun());
        btnResultMenu.setOnClickListener(v -> gameView.openMenu());

        updateUi(gameView.snapshot());
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
        handler.post(uiTicker);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(uiTicker);
        gameView.onHostPause();
        super.onPause();
    }

    private void updateUi(CastawayWorld.StatusSnapshot snapshot) {
        tvFood.setText("Food " + snapshot.food);
        tvWater.setText("Water " + snapshot.water);
        tvMorale.setText("Morale " + snapshot.morale);
        tvPopulation.setText("Camp " + snapshot.population);
        tvDay.setText("Day " + snapshot.day);
        tvWeather.setText(snapshot.finalStorm ? "Final " + snapshot.weather : snapshot.weather);
        tvHint.setText(snapshot.hint);
        tvObjective.setText(snapshot.objective);
        btnBuild.setText(snapshot.buildLabel);
        btnAssign.setText(snapshot.assignLabel);
        btnCraft.setText(snapshot.craftLabel);
        btnMap.setText(snapshot.mapMode ? "Map On" : "Map");
        bindings.resultTitle.setText(snapshot.resultTitle);
        bindings.resultBody.setText(snapshot.resultBody);
        bindings.menuOverlay.setVisibility(snapshot.state == CastawayState.MENU ? View.VISIBLE : View.GONE);
        bindings.pauseOverlay.setVisibility(snapshot.state == CastawayState.PAUSED ? View.VISIBLE : View.GONE);
        boolean showResult = snapshot.state == CastawayState.GAME_OVER || snapshot.state == CastawayState.VICTORY;
        bindings.resultOverlay.setVisibility(showResult ? View.VISIBLE : View.GONE);
    }

    private static class FrameBindings {
        View menuOverlay;
        View pauseOverlay;
        View resultOverlay;
        TextView resultTitle;
        TextView resultBody;
    }
}
