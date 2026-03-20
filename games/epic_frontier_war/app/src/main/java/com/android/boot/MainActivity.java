package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.TonePlayer;
import com.android.boot.core.BattleManager;
import com.android.boot.core.GameState;
import com.android.boot.core.RuneFavor;
import com.android.boot.ui.GameHudController;
import com.android.boot.ui.GameView;
import com.android.boot.ui.OverlayController;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private GameHudController hudController;
    private OverlayController overlayController;
    private TonePlayer tonePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tonePlayer = new TonePlayer();
        gameView = findViewById(R.id.game_view);
        overlayController = new OverlayController(
                findViewById(R.id.menu_overlay),
                findViewById(R.id.pause_overlay),
                findViewById(R.id.game_over_overlay),
                findViewById(R.id.how_to_play_overlay),
                (TextView) findViewById(R.id.game_over_title),
                (TextView) findViewById(R.id.summary_units),
                (TextView) findViewById(R.id.summary_kills),
                (TextView) findViewById(R.id.summary_spells),
                (TextView) findViewById(R.id.summary_damage),
                (Button) findViewById(R.id.btn_pause),
                (View) findViewById(R.id.playing_hud)
        );
        hudController = new GameHudController(this, gameView, overlayController, tonePlayer);
        gameView.bindControllers(hudController, overlayController, tonePlayer);
        bindButtons();
        overlayController.showMenu();
    }

    private void bindButtons() {
        ((Button) findViewById(R.id.btn_start)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.startNewMatch();
        });
        ((Button) findViewById(R.id.btn_how_to_play)).setOnClickListener(v -> {
            tonePlayer.playTap();
            overlayController.showHowToPlay();
        });
        ((ImageButton) findViewById(R.id.btn_menu_mute)).setOnClickListener(v -> toggleMute((ImageButton) v));
        ((Button) findViewById(R.id.btn_pause)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.pauseBattle();
        });
        ((ImageButton) findViewById(R.id.btn_pause_toggle)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.pauseBattle();
        });
        ((Button) findViewById(R.id.btn_resume)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.resumeBattle();
        });
        ((Button) findViewById(R.id.btn_pause_restart)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.startNewMatch();
        });
        ((Button) findViewById(R.id.btn_pause_menu)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.returnToMenu();
        });
        ((ImageButton) findViewById(R.id.btn_pause_mute)).setOnClickListener(v -> toggleMute((ImageButton) v));
        ((Button) findViewById(R.id.btn_over_restart)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.startNewMatch();
        });
        ((Button) findViewById(R.id.btn_over_menu)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.returnToMenu();
        });
        ((Button) findViewById(R.id.btn_help_close)).setOnClickListener(v -> {
            tonePlayer.playTap();
            overlayController.hideHowToPlay();
        });
        ((Button) findViewById(R.id.btn_help_start)).setOnClickListener(v -> {
            tonePlayer.playTap();
            gameView.startNewMatch();
        });
        hudController.bindBattleButtons();
    }

    private void toggleMute(ImageButton button) {
        tonePlayer.toggleMute();
        hudController.refreshMuteButtons();
        tonePlayer.playTap();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onHostPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
    }
}
