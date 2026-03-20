package com.android.boot.ui;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.boot.core.GameState;
import com.android.boot.core.MatchStats;

public class OverlayController {
    private final View menuOverlay;
    private final View pauseOverlay;
    private final View gameOverOverlay;
    private final View howToPlayOverlay;
    private final TextView gameOverTitle;
    private final TextView summaryUnits;
    private final TextView summaryKills;
    private final TextView summarySpells;
    private final TextView summaryDamage;
    private final Button pauseButton;
    private final View playingHud;

    public OverlayController(View menuOverlay, View pauseOverlay, View gameOverOverlay, View howToPlayOverlay,
                             TextView gameOverTitle, TextView summaryUnits, TextView summaryKills,
                             TextView summarySpells, TextView summaryDamage, Button pauseButton, View playingHud) {
        this.menuOverlay = menuOverlay;
        this.pauseOverlay = pauseOverlay;
        this.gameOverOverlay = gameOverOverlay;
        this.howToPlayOverlay = howToPlayOverlay;
        this.gameOverTitle = gameOverTitle;
        this.summaryUnits = summaryUnits;
        this.summaryKills = summaryKills;
        this.summarySpells = summarySpells;
        this.summaryDamage = summaryDamage;
        this.pauseButton = pauseButton;
        this.playingHud = playingHud;
    }

    public void applyState(GameState state, boolean victory, MatchStats stats) {
        menuOverlay.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        gameOverOverlay.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
        playingHud.setVisibility(state == GameState.PLAYING || state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        pauseButton.setVisibility(state == GameState.PLAYING ? View.VISIBLE : View.GONE);
        if (state == GameState.GAME_OVER && stats != null) {
            gameOverTitle.setText(victory ? "Victory" : "Defeat");
            summaryUnits.setText(String.valueOf(stats.unitsSummoned));
            summaryKills.setText(String.valueOf(stats.enemiesDefeated));
            summarySpells.setText(String.valueOf(stats.spellsCast));
            summaryDamage.setText(String.valueOf(stats.fortressDamage));
        }
    }

    public void showMenu() {
        applyState(GameState.MENU, false, null);
        howToPlayOverlay.setVisibility(View.GONE);
    }

    public void showHowToPlay() {
        howToPlayOverlay.setVisibility(View.VISIBLE);
    }

    public void hideHowToPlay() {
        howToPlayOverlay.setVisibility(View.GONE);
    }
}
