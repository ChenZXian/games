package com.android.boot.ui;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;

import java.util.Locale;

public class UiOverlayController {
    private final TextView commandValue;
    private final TextView statusValue;
    private final TextView timerValue;
    private final TextView menuBody;
    private final TextView resultTitle;
    private final TextView resultBody;
    private final View menuOverlay;
    private final View pauseOverlay;
    private final View resultOverlay;
    private final Button startButton;
    private final Button resultPrimaryButton;

    public UiOverlayController(
            TextView commandValue,
            TextView statusValue,
            TextView timerValue,
            TextView menuBody,
            TextView resultTitle,
            TextView resultBody,
            View menuOverlay,
            View pauseOverlay,
            View resultOverlay,
            Button startButton,
            Button resultPrimaryButton
    ) {
        this.commandValue = commandValue;
        this.statusValue = statusValue;
        this.timerValue = timerValue;
        this.menuBody = menuBody;
        this.resultTitle = resultTitle;
        this.resultBody = resultBody;
        this.menuOverlay = menuOverlay;
        this.pauseOverlay = pauseOverlay;
        this.resultOverlay = resultOverlay;
        this.startButton = startButton;
        this.resultPrimaryButton = resultPrimaryButton;
    }

    public void sync(GameSession session) {
        commandValue.setText(String.valueOf(session.commandPoints));
        statusValue.setText(String.format(Locale.US, "Stage %d  Ports %d", session.getDisplayStage(), session.getPlayerOwnedCount()));
        int seconds = Math.max(0, (int) session.stageTimeLeft);
        timerValue.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
        menuBody.setText(String.format(Locale.US,
                "Build convoy routes across bright island harbors, keep your flagship stocked, and break the enemy anchor line.\n\nUnlocked stage %d  Total stars %d",
                session.highestUnlockedStage,
                session.totalStars));
        resultTitle.setText(session.resultTitle.isEmpty() ? "Mission Report" : session.resultTitle);
        resultBody.setText(session.resultBody);
        startButton.setText(String.format(Locale.US, "Start Stage %d", session.getStartStageNumber()));
        resultPrimaryButton.setText(session.lastStageWon && session.canAdvanceStage() ? "Next Stage" : "Restart");
        menuOverlay.setVisibility(session.state == GameState.MENU ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(session.state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        resultOverlay.setVisibility(session.state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    }
}
