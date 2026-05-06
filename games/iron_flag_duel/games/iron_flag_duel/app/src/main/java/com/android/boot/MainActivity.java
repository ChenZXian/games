package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.BgmPlayer;
import com.android.boot.core.GameManager;
import com.android.boot.core.GameState;
import com.android.boot.entity.Side;
import com.android.boot.ui.GameSurfaceView;

public class MainActivity extends AppCompatActivity {
    private GameManager gameManager;
    private GameSurfaceView gameView;
    private TextView txtPhase;
    private TextView txtTurn;
    private TextView txtAi;
    private TextView txtStatus;
    private TextView txtResultTitle;
    private TextView txtResultDetail;
    private LinearLayout panelMenu;
    private LinearLayout panelPause;
    private LinearLayout panelResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameManager = new GameManager();
        bindViews();
        wireEvents();
        refreshUi();
    }

    private void bindViews() {
        gameView = findViewById(R.id.game_view);
        txtPhase = findViewById(R.id.txt_phase);
        txtTurn = findViewById(R.id.txt_turn);
        txtAi = findViewById(R.id.txt_ai);
        txtStatus = findViewById(R.id.txt_status);
        txtResultTitle = findViewById(R.id.txt_result_title);
        txtResultDetail = findViewById(R.id.txt_result_detail);
        panelMenu = findViewById(R.id.panel_menu);
        panelPause = findViewById(R.id.panel_pause);
        panelResult = findViewById(R.id.panel_result);
        gameView.bind(gameManager, (row, col) -> {
            gameManager.onCellTapped(row, col);
            refreshUi();
        });
    }

    private void wireEvents() {
        findViewById(R.id.btn_start).setOnClickListener(v -> {
            gameManager.startMatch();
            refreshUi();
        });
        findViewById(R.id.btn_help).setOnClickListener(v -> txtStatus.setText("Flip one hidden piece each turn or move one revealed movable piece. Capture the enemy flag or leave the enemy with no legal action."));
        ImageButton btnPause = findViewById(R.id.btn_pause);
        btnPause.setOnClickListener(v -> {
            gameManager.pause();
            refreshUi();
        });
        findViewById(R.id.btn_resume).setOnClickListener(v -> {
            gameManager.resume();
            refreshUi();
        });
        findViewById(R.id.btn_restart).setOnClickListener(v -> {
            gameManager.startMatch();
            refreshUi();
        });
        findViewById(R.id.btn_menu).setOnClickListener(v -> {
            gameManager.backToMenu();
            refreshUi();
        });
        findViewById(R.id.btn_new_match).setOnClickListener(v -> {
            gameManager.startMatch();
            refreshUi();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BgmPlayer.playLoop(this, R.raw.bgm, 0.42f);
        refreshUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BgmPlayer.pause();
        gameView.onHostPause();
        refreshUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            BgmPlayer.release();
        }
    }

    private void refreshUi() {
        GameState state = gameManager.getState();
        txtPhase.setText("Phase: " + state.name());
        txtTurn.setText("Turn: " + (gameManager.getTurn() == Side.PLAYER ? "Player" : "AI"));
        txtAi.setText(gameManager.isAiThinking() ? "AI Thinking" : "AI Idle");
        txtStatus.setText(gameManager.getStatus());
        panelMenu.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
        panelPause.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        panelResult.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
        String result = gameManager.getResult();
        boolean victory = result.startsWith("You") || result.startsWith("AI has no legal") || result.startsWith("Player has no legal");
        txtResultTitle.setText(victory ? "Victory" : "Defeat");
        txtResultDetail.setText(gameManager.getResult().isEmpty() ? "No result yet" : gameManager.getResult());
        gameView.invalidate();
    }
}
