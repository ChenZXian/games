package com.android.boot.ui;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.boot.R;
import com.android.boot.core.GameState;
import com.android.boot.core.MatchStats;
import com.google.android.material.button.MaterialButton;

public class OverlayController {
    public interface Listener {
        void onStartBattle();
        void onResumeBattle();
        void onRestartBattle();
        void onReturnToMenu();
        void onToggleMute();
        void onShowHelp(boolean show);
    }

    private final Activity activity;
    private final View root;
    private final Listener listener;
    private LinearLayout menuOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout gameOverOverlay;
    private LinearLayout helpOverlay;
    private MaterialButton menuMute;
    private MaterialButton pauseMute;
    private TextView gameOverTitle;
    private TextView summaryUnits;
    private TextView summaryEnemies;
    private TextView summarySpells;
    private TextView summaryDamage;

    public OverlayController(Activity activity, View root, Listener listener) {
        this.activity = activity;
        this.root = root;
        this.listener = listener;
    }

    public void bind() {
        menuOverlay = root.findViewById(R.id.menu_overlay);
        pauseOverlay = root.findViewById(R.id.pause_overlay);
        gameOverOverlay = root.findViewById(R.id.game_over_overlay);
        helpOverlay = root.findViewById(R.id.help_overlay);
        menuMute = root.findViewById(R.id.button_menu_mute);
        pauseMute = root.findViewById(R.id.button_pause_mute);
        gameOverTitle = root.findViewById(R.id.text_game_over_title);
        summaryUnits = root.findViewById(R.id.text_summary_units);
        summaryEnemies = root.findViewById(R.id.text_summary_enemies);
        summarySpells = root.findViewById(R.id.text_summary_spells);
        summaryDamage = root.findViewById(R.id.text_summary_damage);
        root.findViewById(R.id.button_start).setOnClickListener(v -> listener.onStartBattle());
        root.findViewById(R.id.button_help).setOnClickListener(v -> listener.onShowHelp(true));
        root.findViewById(R.id.button_help_close).setOnClickListener(v -> listener.onShowHelp(false));
        menuMute.setOnClickListener(v -> listener.onToggleMute());
        root.findViewById(R.id.button_resume).setOnClickListener(v -> listener.onResumeBattle());
        root.findViewById(R.id.button_pause_restart).setOnClickListener(v -> listener.onRestartBattle());
        root.findViewById(R.id.button_pause_menu).setOnClickListener(v -> listener.onReturnToMenu());
        pauseMute.setOnClickListener(v -> listener.onToggleMute());
        root.findViewById(R.id.button_game_over_restart).setOnClickListener(v -> listener.onRestartBattle());
        root.findViewById(R.id.button_game_over_menu).setOnClickListener(v -> listener.onReturnToMenu());
    }

    public void refreshMute(boolean muted) {
        activity.runOnUiThread(() -> {
            menuMute.setText(muted ? "Unmute" : activity.getString(R.string.btn_mute));
            menuMute.setIconResource(muted ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
            pauseMute.setText(muted ? "Unmute" : activity.getString(R.string.btn_mute));
            pauseMute.setIconResource(muted ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
        });
    }

    public void showHelp(boolean show) {
        activity.runOnUiThread(() -> helpOverlay.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    public void showState(GameState state, MatchStats stats, boolean victory) {
        activity.runOnUiThread(() -> {
            menuOverlay.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
            pauseOverlay.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
            gameOverOverlay.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
            if (state == GameState.GAME_OVER && stats != null) {
                gameOverTitle.setText(victory ? R.string.game_over_victory : R.string.game_over_defeat);
                summaryUnits.setText(activity.getString(R.string.summary_units) + ": " + stats.getUnitsSummoned());
                summaryEnemies.setText(activity.getString(R.string.summary_enemies) + ": " + stats.getEnemiesDefeated());
                summarySpells.setText(activity.getString(R.string.summary_spells) + ": " + stats.getSpellsCast());
                summaryDamage.setText(activity.getString(R.string.summary_damage) + ": " + stats.getFortressDamage());
            }
        });
    }
}
