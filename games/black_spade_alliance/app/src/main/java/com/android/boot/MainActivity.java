package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameSnapshot;
import com.android.boot.ui.GameView;

public final class MainActivity extends AppCompatActivity implements GameView.UiCallbacks {
    private GameView gameView;
    private LinearLayout menuOverlay;
    private LinearLayout helpOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout resultOverlay;
    private TextView txtRound;
    private TextView txtScore;
    private TextView txtAlliance;
    private TextView txtLead;
    private TextView txtInfo;
    private TextView txtResultTitle;
    private TextView txtResultBody;
    private Button btnPlay;
    private Button btnPass;
    private Button btnNext;
    private ImageButton btnMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.menu_overlay);
        helpOverlay = findViewById(R.id.help_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        resultOverlay = findViewById(R.id.result_overlay);
        txtRound = findViewById(R.id.txt_round);
        txtScore = findViewById(R.id.txt_score);
        txtAlliance = findViewById(R.id.txt_alliance);
        txtLead = findViewById(R.id.txt_lead);
        txtInfo = findViewById(R.id.txt_info);
        txtResultTitle = findViewById(R.id.txt_result_title);
        txtResultBody = findViewById(R.id.txt_result_body);
        btnPlay = findViewById(R.id.btn_play);
        btnPass = findViewById(R.id.btn_pass);
        btnNext = findViewById(R.id.btn_next);
        btnMute = findViewById(R.id.btn_mute);
        bindButtons();
        gameView.setUiCallbacks(this);
    }

    private void bindButtons() {
        bindScale(findViewById(R.id.btn_start), v -> {
            gameView.getEngine().startMatch();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_how_to_play), v -> helpOverlay.setVisibility(View.VISIBLE));
        bindScale(findViewById(R.id.btn_help_close), v -> helpOverlay.setVisibility(View.GONE));
        bindScale(findViewById(R.id.btn_pause), v -> {
            gameView.getEngine().pause();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_resume), v -> {
            gameView.getEngine().resume();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_restart), v -> {
            gameView.getEngine().restartMatch();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_menu), v -> {
            gameView.getEngine().goToMenu();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(btnPlay, v -> {
            gameView.getEngine().playSelectedCards();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(btnPass, v -> {
            gameView.getEngine().playerPass();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(btnNext, v -> {
            gameView.getEngine().advanceAfterResult();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(btnMute, v -> {
            gameView.getEngine().toggleMuted();
            syncMute();
        });
    }

    private void bindScale(View view, View.OnClickListener listener) {
        view.setOnTouchListener((v, event) -> false);
        view.setOnClickListener(v -> {
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
            listener.onClick(v);
        });
    }

    private void syncMute() {
        btnMute.setImageResource(gameView.getEngine().isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
    }

    @Override
    public void onSnapshot(GameSnapshot snapshot) {
        runOnUiThread(() -> {
            txtRound.setText(snapshot.roundLabel);
            txtScore.setText(snapshot.scoreLabel);
            txtAlliance.setText(snapshot.allianceLabel);
            txtLead.setText(snapshot.leadLabel);
            txtInfo.setText(snapshot.infoLabel);
            txtResultTitle.setText(snapshot.resultTitle);
            txtResultBody.setText(snapshot.resultBody);
            btnPlay.setEnabled(snapshot.playEnabled);
            btnPass.setEnabled(snapshot.passEnabled);
            btnNext.setText(snapshot.actionLabel);
            syncMute();
            menuOverlay.setVisibility(GameEngine.STATE_MENU.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            if (GameEngine.STATE_MENU.equals(snapshot.state)) {
                pauseOverlay.setVisibility(View.GONE);
                resultOverlay.setVisibility(View.GONE);
            }
            if (!GameEngine.STATE_MENU.equals(snapshot.state)) {
                menuOverlay.setVisibility(View.GONE);
            }
            pauseOverlay.setVisibility(GameEngine.STATE_PAUSED.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            resultOverlay.setVisibility(snapshot.showResultOverlay ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResumeView();
    }

    @Override
    protected void onPause() {
        gameView.onPauseView();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        gameView.release();
        super.onDestroy();
    }
}
