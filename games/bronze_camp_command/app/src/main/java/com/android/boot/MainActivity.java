package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.boot.core.BronzeState;
import com.android.boot.core.BronzeWorld;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GameView gameView;
    private TextView tvFood;
    private TextView tvWood;
    private TextView tvStone;
    private TextView tvPop;
    private TextView tvTimer;
    private TextView tvAlert;
    private TextView tvObjective;
    private TextView tvHint;
    private Button btnBuild;
    private Button btnTrain;
    private Button btnUpgrade;
    private Button btnScout;
    private Button btnArmy;
    private FrameBindings bindings;
    private final Runnable uiTicker = new Runnable() {
        @Override
        public void run() {
            if (gameView != null) {
                updateUi(gameView.snapshot());
            }
            handler.postDelayed(this, 150L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindings = new FrameBindings();
        gameView = findViewById(R.id.game_view);
        tvFood = findViewById(R.id.tv_food);
        tvWood = findViewById(R.id.tv_wood);
        tvStone = findViewById(R.id.tv_stone);
        tvPop = findViewById(R.id.tv_pop);
        tvTimer = findViewById(R.id.tv_timer);
        tvAlert = findViewById(R.id.tv_alert);
        tvObjective = findViewById(R.id.tv_objective);
        tvHint = findViewById(R.id.tv_hint);
        btnBuild = findViewById(R.id.btn_build);
        btnTrain = findViewById(R.id.btn_train);
        btnUpgrade = findViewById(R.id.btn_upgrade);
        btnScout = findViewById(R.id.btn_scout);
        btnArmy = findViewById(R.id.btn_army);
        ImageButton btnPause = findViewById(R.id.btn_pause);
        ImageButton btnMute = findViewById(R.id.btn_mute);
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

        btnBuild.setOnClickListener(v -> gameView.cycleBuildPlan());
        btnTrain.setOnClickListener(v -> gameView.trainSelectedUnit());
        btnUpgrade.setOnClickListener(v -> gameView.buyUpgrade());
        btnScout.setOnClickListener(v -> gameView.sendScout());
        btnArmy.setOnClickListener(v -> gameView.selectArmy());
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

    private void updateUi(BronzeWorld.StatusSnapshot snapshot) {
        tvFood.setText("Food " + snapshot.food);
        tvWood.setText("Wood " + snapshot.wood);
        tvStone.setText("Stone " + snapshot.stone);
        tvPop.setText("Pop " + snapshot.population + "/" + snapshot.populationCap);
        tvTimer.setText(snapshot.timeLabel);
        tvAlert.setText(snapshot.alert);
        tvObjective.setText(snapshot.objective);
        tvHint.setText(snapshot.hint);
        btnBuild.setText(snapshot.buildLabel);
        btnTrain.setText(snapshot.trainLabel);
        btnUpgrade.setText(snapshot.upgradeLabel);
        btnScout.setText(snapshot.scoutLabel);
        btnArmy.setText(snapshot.armyLabel);
        bindings.resultTitle.setText(snapshot.resultTitle);
        bindings.resultBody.setText(snapshot.resultBody);
        bindings.menuOverlay.setVisibility(snapshot.state == BronzeState.MENU ? View.VISIBLE : View.GONE);
        bindings.pauseOverlay.setVisibility(snapshot.state == BronzeState.PAUSED ? View.VISIBLE : View.GONE);
        boolean showResult = snapshot.state == BronzeState.GAME_OVER || snapshot.state == BronzeState.VICTORY;
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
