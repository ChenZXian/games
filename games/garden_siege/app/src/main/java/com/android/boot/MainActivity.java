package com.android.boot;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.data.ProgressStore;
import com.android.boot.ui.GameView;
import com.android.boot.R;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private LinearLayout menuPanel;
    private ProgressStore progressStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressStore = new ProgressStore(this);
        gameView = findViewById(R.id.gameView);
        menuPanel = findViewById(R.id.menuPanel);
        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnHow = findViewById(R.id.btnHow);
        Button btnSettings = findViewById(R.id.btnSettings);
        btnPlay.setOnClickListener(v -> openLevelSelect());
        btnHow.setOnClickListener(v -> openHowTo());
        btnSettings.setOnClickListener(v -> openSettings());
        gameView.setHost(this);
        showMenu();
    }

    public void showMenu() {
        menuPanel.setVisibility(View.VISIBLE);
        gameView.showMenuState();
    }

    public void hideMenuAndStart(int level) {
        menuPanel.setVisibility(View.GONE);
        gameView.startLevel(level, progressStore.isFpsEnabled(), progressStore.isMuted());
    }

    private void openHowTo() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.btn_how_to_play))
                .setMessage("Tap a card to arm, tap a tile to place. Tap sun orbs to collect sun. Use shovel to remove plants.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void openSettings() {
        String[] items = new String[]{"Toggle Mute", "Toggle FPS"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.btn_settings))
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        progressStore.setMuted(!progressStore.isMuted());
                    } else {
                        progressStore.setFpsEnabled(!progressStore.isFpsEnabled());
                    }
                })
                .show();
    }

    private void openLevelSelect() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int unlocked = progressStore.getUnlockedLevels();
        for (int i = 1; i <= 6; i++) {
            Button b = new Button(this);
            long best = progressStore.getBestMillis(i);
            String label = "Level " + i + (best > 0 ? "  Best " + (best / 1000f) + "s" : "");
            b.setText(label);
            b.setEnabled(i <= unlocked);
            int index = i;
            b.setOnClickListener(v -> {
                hideMenuAndStart(index);
            });
            layout.addView(b);
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_levels))
                .setView(layout)
                .setNegativeButton(getString(R.string.btn_menu), null)
                .show();
    }

    public void onLevelVictory(int level, long elapsedMillis) {
        progressStore.unlockLevel(level + 1);
        progressStore.updateBest(level, elapsedMillis);
        runOnUiThread(() -> {
            String[] items = new String[]{getString(R.string.btn_next), getString(R.string.btn_menu)};
            new AlertDialog.Builder(this)
                    .setTitle("Victory")
                    .setItems(items, (d, which) -> {
                        if (which == 0 && level < 6) {
                            hideMenuAndStart(level + 1);
                        } else {
                            showMenu();
                        }
                    })
                    .show();
        });
    }

    public void onLevelDefeat() {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setPositiveButton(getString(R.string.btn_restart), (d, w) -> gameView.restartLevel())
                .setNegativeButton(getString(R.string.btn_menu), (d, w) -> showMenu())
                .show());
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeGame();
    }
}
