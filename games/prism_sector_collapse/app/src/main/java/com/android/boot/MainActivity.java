package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.AudioController;
import com.android.boot.core.GameState;
import com.android.boot.core.PrismStage;
import com.android.boot.ui.GameView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements GameView.Listener {
    private AudioController audioController;
    private GameView boardView;
    private View screenMenu;
    private View screenMap;
    private View screenGuide;
    private View overlayPause;
    private View overlayResult;
    private View chainMeterFill;
    private TextView chapterLabel;
    private TextView stageLabel;
    private TextView objectiveText;
    private TextView scoreValue;
    private TextView movesValue;
    private TextView comboValue;
    private TextView resultTitle;
    private TextView resultBody;
    private Button nextStageButton;
    private List<PrismStage> campaign;
    private int currentStageIndex;
    private GameState gameState = GameState.MENU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioController = new AudioController(this);
        campaign = PrismStage.createCampaign();
        boardView = findViewById(R.id.board_view);
        boardView.setListener(this);
        chainMeterFill = findViewById(R.id.chain_meter_fill);
        chapterLabel = findViewById(R.id.chapter_label);
        stageLabel = findViewById(R.id.stage_label);
        objectiveText = findViewById(R.id.objective_text);
        scoreValue = findViewById(R.id.score_value);
        movesValue = findViewById(R.id.moves_value);
        comboValue = findViewById(R.id.combo_value);
        resultTitle = findViewById(R.id.result_title);
        resultBody = findViewById(R.id.result_body);
        nextStageButton = findViewById(R.id.btn_next_stage);
        screenMenu = findViewById(R.id.screen_menu);
        screenMap = findViewById(R.id.screen_map);
        screenGuide = findViewById(R.id.screen_guide);
        overlayPause = findViewById(R.id.overlay_pause);
        overlayResult = findViewById(R.id.overlay_result);
        bindButtons();
        updateStageHeader(campaign.get(0));
        showMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameState == GameState.PLAYING) {
            boardView.onHostResume();
            audioController.playGameplay();
        } else {
            audioController.playMenu();
        }
    }

    @Override
    protected void onPause() {
        boardView.onHostPause();
        audioController.stopMusic();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        audioController.release();
        super.onDestroy();
    }

    private void bindButtons() {
        findViewById(R.id.btn_start_game).setOnClickListener(v -> {
            currentStageIndex = 0;
            startStage(currentStageIndex);
        });
        findViewById(R.id.btn_chapter_map).setOnClickListener(v -> showMap());
        findViewById(R.id.btn_guide).setOnClickListener(v -> showGuide());
        findViewById(R.id.btn_close_guide).setOnClickListener(v -> closeOverlayToMenu());
        findViewById(R.id.btn_close_map).setOnClickListener(v -> closeOverlayToMenu());
        findViewById(R.id.btn_pause).setOnClickListener(v -> pauseGame());
        findViewById(R.id.btn_resume_game).setOnClickListener(v -> resumeGame());
        findViewById(R.id.btn_restart_game).setOnClickListener(v -> restartStage());
        findViewById(R.id.btn_quit_to_menu).setOnClickListener(v -> showMenu());
        findViewById(R.id.btn_result_menu).setOnClickListener(v -> showMenu());
        findViewById(R.id.btn_retry_stage).setOnClickListener(v -> restartStage());
        nextStageButton.setOnClickListener(v -> advanceStage());
        bindChapterButton(R.id.chapter_one, 0);
        bindChapterButton(R.id.chapter_two, 1);
        bindChapterButton(R.id.chapter_three, 2);
        bindChapterButton(R.id.chapter_four, 3);
        bindChapterButton(R.id.chapter_five, 4);
        bindChapterButton(R.id.chapter_six, 5);
    }

    private void bindChapterButton(int viewId, int stageIndex) {
        findViewById(viewId).setOnClickListener(v -> {
            currentStageIndex = stageIndex;
            startStage(stageIndex);
        });
    }

    private void startStage(int stageIndex) {
        hideAllOverlays();
        PrismStage stage = campaign.get(stageIndex);
        updateStageHeader(stage);
        boardView.loadStage(stage);
        boardView.onHostResume();
        audioController.playGameplay();
        gameState = GameState.PLAYING;
    }

    private void restartStage() {
        startStage(currentStageIndex);
    }

    private void advanceStage() {
        if (currentStageIndex < campaign.size() - 1) {
            currentStageIndex++;
            startStage(currentStageIndex);
        } else {
            showMenu();
        }
    }

    private void pauseGame() {
        if (gameState != GameState.PLAYING) {
            return;
        }
        boardView.pauseBoard();
        overlayPause.setVisibility(View.VISIBLE);
        gameState = GameState.PAUSED;
    }

    private void resumeGame() {
        overlayPause.setVisibility(View.GONE);
        boardView.resumeBoard();
        gameState = GameState.PLAYING;
    }

    private void showMenu() {
        boardView.onHostPause();
        hideAllOverlays();
        screenMenu.setVisibility(View.VISIBLE);
        audioController.playMenu();
        gameState = GameState.MENU;
    }

    private void showMap() {
        hideAllOverlays();
        screenMap.setVisibility(View.VISIBLE);
        boardView.onHostPause();
        audioController.playMenu();
        gameState = GameState.MAP;
    }

    private void showGuide() {
        hideAllOverlays();
        screenGuide.setVisibility(View.VISIBLE);
        boardView.onHostPause();
        audioController.playMenu();
        gameState = GameState.GUIDE;
    }

    private void closeOverlayToMenu() {
        screenMap.setVisibility(View.GONE);
        screenGuide.setVisibility(View.GONE);
        screenMenu.setVisibility(View.VISIBLE);
        gameState = GameState.MENU;
        audioController.playMenu();
    }

    private void hideAllOverlays() {
        screenMenu.setVisibility(View.GONE);
        screenMap.setVisibility(View.GONE);
        screenGuide.setVisibility(View.GONE);
        overlayPause.setVisibility(View.GONE);
        overlayResult.setVisibility(View.GONE);
    }

    private void updateStageHeader(PrismStage stage) {
        chapterLabel.setText(String.valueOf(stage.sectorIndex));
        stageLabel.setText(stage.code);
        objectiveText.setText(boardView.getObjectiveDescription(stage));
        scoreValue.setText("0");
        movesValue.setText(String.valueOf(stage.moves));
        comboValue.setText("0");
        updateChargeMeter(0);
    }

    private void updateChargeMeter(int charge) {
        chainMeterFill.post(() -> {
            View parent = (View) chainMeterFill.getParent();
            int parentHeight = parent.getHeight();
            if (parentHeight <= 0) {
                return;
            }
            int height = Math.max((int) (parentHeight * 0.08f), (int) (parentHeight * (charge / 100f)));
            ViewGroup.LayoutParams params = chainMeterFill.getLayoutParams();
            params.height = height;
            chainMeterFill.setLayoutParams(params);
        });
    }

    @Override
    public void onHudUpdated(PrismStage stage, int score, int movesLeft, int chain, int charge, String objectiveLabel) {
        chapterLabel.setText(String.valueOf(stage.sectorIndex));
        stageLabel.setText(stage.code);
        objectiveText.setText(objectiveLabel);
        scoreValue.setText(String.valueOf(score));
        movesValue.setText(String.valueOf(movesLeft));
        comboValue.setText(String.valueOf(chain));
        updateChargeMeter(charge);
    }

    @Override
    public void onPlayEffect(String effectKey) {
        audioController.playEffect(effectKey);
    }

    @Override
    public void onStageResolved(boolean cleared, String message) {
        overlayPause.setVisibility(View.GONE);
        overlayResult.setVisibility(View.VISIBLE);
        resultTitle.setText(cleared ? "Sector Stabilized" : "Sector Lost");
        resultBody.setText(message);
        nextStageButton.setVisibility(cleared ? View.VISIBLE : View.GONE);
        audioController.playEffect(cleared ? "win" : "fail");
        audioController.playMenu();
        gameState = GameState.RESULT;
    }
}
