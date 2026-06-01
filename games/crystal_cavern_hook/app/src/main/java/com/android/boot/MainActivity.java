package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.boot.core.CrystalAudio;
import com.android.boot.ui.CrystalGameView;

public class MainActivity extends Activity implements CrystalGameView.GameListener {
    private CrystalGameView gameView;
    private CrystalAudio audio;
    private LinearLayout menuPanel;
    private LinearLayout pausePanel;
    private LinearLayout resultPanel;
    private TextView scoreText;
    private TextView timerText;
    private TextView stageText;
    private TextView orderText;
    private TextView resultTitle;
    private TextView resultBody;
    private Button dynamiteButton;
    private Button magnetButton;
    private Button boostButton;
    private Button lensButton;
    private Button shieldButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audio = new CrystalAudio(this);
        gameView = findViewById(R.id.game_view);
        gameView.setGameListener(this);
        gameView.setAudio(audio);
        menuPanel = findViewById(R.id.menu_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);
        scoreText = findViewById(R.id.hud_score);
        timerText = findViewById(R.id.hud_timer);
        stageText = findViewById(R.id.hud_stage);
        orderText = findViewById(R.id.order_text);
        resultTitle = findViewById(R.id.result_title);
        resultBody = findViewById(R.id.result_body);
        dynamiteButton = findViewById(R.id.tool_dynamite);
        magnetButton = findViewById(R.id.tool_magnet);
        boostButton = findViewById(R.id.tool_boost);
        lensButton = findViewById(R.id.tool_lens);
        shieldButton = findViewById(R.id.tool_shield);
        bindButtons();
        showMenu();
    }

    private void bindButtons() {
        findViewById(R.id.start_button).setOnClickListener(v -> startShift());
        findViewById(R.id.how_button).setOnClickListener(v -> showHelp());
        findViewById(R.id.pause_button).setOnClickListener(v -> {
            if (gameView.isPlaying()) {
                gameView.pauseGame();
                showPause();
            }
        });
        findViewById(R.id.resume_button).setOnClickListener(v -> {
            hidePanels();
            gameView.resumeGame();
            audio.resumeBgm();
        });
        findViewById(R.id.menu_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.restart_button).setOnClickListener(v -> startShift());
        dynamiteButton.setOnClickListener(v -> useTool(CrystalGameView.TOOL_DYNAMITE));
        magnetButton.setOnClickListener(v -> useTool(CrystalGameView.TOOL_MAGNET));
        boostButton.setOnClickListener(v -> useTool(CrystalGameView.TOOL_BOOST));
        lensButton.setOnClickListener(v -> useTool(CrystalGameView.TOOL_LENS));
        shieldButton.setOnClickListener(v -> useTool(CrystalGameView.TOOL_SHIELD));
    }

    private void useTool(int tool) {
        if (gameView.useTool(tool)) {
            refreshTools();
        } else {
            audio.playFail();
        }
    }

    private void startShift() {
        hidePanels();
        audio.playGameBgm();
        gameView.startShift();
        refreshTools();
    }

    private void showHelp() {
        audio.playUiClick();
        resultTitle.setText("How To Play");
        resultBody.setText("Tap the cave to fire the claw. Prism gates bend the route. Fill the order rail before time ends. Use tools to remove hazards, pull crystals, boost the cable, bend harder, or block a blast.");
        menuPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
    }

    private void showMenu() {
        gameView.stopToMenu();
        audio.playMenuBgm();
        menuPanel.setVisibility(View.VISIBLE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        refreshTools();
    }

    private void showPause() {
        audio.pauseBgm();
        menuPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.VISIBLE);
        resultPanel.setVisibility(View.GONE);
    }

    private void hidePanels() {
        menuPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
    }

    private void refreshTools() {
        dynamiteButton.setText("Dynamite " + gameView.getToolCount(CrystalGameView.TOOL_DYNAMITE));
        magnetButton.setText("Magnet " + gameView.getToolCount(CrystalGameView.TOOL_MAGNET));
        boostButton.setText("Boost " + gameView.getToolCount(CrystalGameView.TOOL_BOOST));
        lensButton.setText("Lens " + gameView.getToolCount(CrystalGameView.TOOL_LENS));
        shieldButton.setText("Shield " + gameView.getToolCount(CrystalGameView.TOOL_SHIELD));
    }

    @Override
    public void onHudChanged(String score, String timer, String stage, String orders) {
        scoreText.setText(score);
        timerText.setText(timer);
        stageText.setText(stage);
        orderText.setText(orders);
        refreshTools();
    }

    @Override
    public void onShiftEnded(boolean won, String summary) {
        if (won) {
            audio.playWinBgm();
        } else {
            audio.playFailBgm();
        }
        resultTitle.setText(won ? "Order Complete" : "Shift Failed");
        resultBody.setText(summary);
        hidePanels();
        resultPanel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
        audio.pauseBgm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (menuPanel != null && pausePanel != null && resultPanel != null && menuPanel.getVisibility() != View.VISIBLE && pausePanel.getVisibility() != View.VISIBLE && resultPanel.getVisibility() != View.VISIBLE) {
            gameView.resumeGame();
            audio.resumeBgm();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameView.shutdown();
        audio.release();
    }
}
