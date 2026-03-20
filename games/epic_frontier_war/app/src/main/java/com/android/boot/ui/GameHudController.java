package com.android.boot.ui;

import android.app.Activity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.boot.R;
import com.android.boot.audio.TonePlayer;
import com.android.boot.core.BattleManager;
import com.android.boot.core.RuneFavor;

public class GameHudController {
    private final Activity activity;
    private final GameView gameView;
    private final OverlayController overlayController;
    private final TonePlayer tonePlayer;
    private final TextView playerHpValue;
    private final TextView enemyHpValue;
    private final TextView energyValue;
    private final TextView favorValue;
    private final TextView favorMeterValue;
    private final TextView heroCooldownValue;
    private final TextView fireCooldownValue;
    private final TextView chainCooldownValue;
    private final TextView blessingCooldownValue;
    private final TextView phaseValue;
    private final TextView statusValue;
    private final ProgressBar playerHpBar;
    private final ProgressBar enemyHpBar;
    private final ProgressBar energyBar;
    private final ImageButton menuMute;
    private final ImageButton pauseMute;

    public GameHudController(Activity activity, GameView gameView, OverlayController overlayController, TonePlayer tonePlayer) {
        this.activity = activity;
        this.gameView = gameView;
        this.overlayController = overlayController;
        this.tonePlayer = tonePlayer;
        playerHpValue = activity.findViewById(R.id.player_hp_value);
        enemyHpValue = activity.findViewById(R.id.enemy_hp_value);
        energyValue = activity.findViewById(R.id.energy_value);
        favorValue = activity.findViewById(R.id.favor_value);
        favorMeterValue = activity.findViewById(R.id.favor_meter_value);
        heroCooldownValue = activity.findViewById(R.id.hero_cooldown_value);
        fireCooldownValue = activity.findViewById(R.id.fire_cooldown_value);
        chainCooldownValue = activity.findViewById(R.id.chain_cooldown_value);
        blessingCooldownValue = activity.findViewById(R.id.blessing_cooldown_value);
        phaseValue = activity.findViewById(R.id.phase_value);
        statusValue = activity.findViewById(R.id.status_value);
        playerHpBar = activity.findViewById(R.id.player_hp_bar);
        enemyHpBar = activity.findViewById(R.id.enemy_hp_bar);
        energyBar = activity.findViewById(R.id.energy_bar);
        menuMute = activity.findViewById(R.id.btn_menu_mute);
        pauseMute = activity.findViewById(R.id.btn_pause_mute);
        refreshMuteButtons();
    }

    public void bindBattleButtons() {
        bindSummon(R.id.btn_summon_militia, BattleManager.SUMMON_MILITIA);
        bindSummon(R.id.btn_summon_archer, BattleManager.SUMMON_ARCHER);
        bindSummon(R.id.btn_summon_knight, BattleManager.SUMMON_KNIGHT);
        bindSummon(R.id.btn_summon_priest, BattleManager.SUMMON_PRIEST);
        bindSummon(R.id.btn_summon_titan, BattleManager.SUMMON_TITAN);
        bindFavor(R.id.btn_favor_flame, RuneFavor.FLAME_FAVOR);
        bindFavor(R.id.btn_favor_storm, RuneFavor.STORM_FAVOR);
        bindFavor(R.id.btn_favor_vital, RuneFavor.VITAL_FAVOR);
        bindAction(R.id.btn_spell_fire, BattleManager.ACTION_FIRE_RAIN);
        bindAction(R.id.btn_spell_chain, BattleManager.ACTION_CHAIN_BOLT);
        bindAction(R.id.btn_spell_blessing, BattleManager.ACTION_BLESSING_WAVE);
        bindAction(R.id.btn_hero_ability, BattleManager.ACTION_HERO_ABILITY);
    }

    private void bindSummon(int id, int type) {
        Button button = activity.findViewById(id);
        button.setOnClickListener(v -> {
            if (gameView.performAction(type)) {
                tonePlayer.playTap();
            }
        });
    }

    private void bindFavor(int id, RuneFavor favor) {
        Button button = activity.findViewById(id);
        button.setOnClickListener(v -> {
            gameView.selectFavor(favor);
            tonePlayer.playTap();
        });
    }

    private void bindAction(int id, int action) {
        Button button = activity.findViewById(id);
        button.setOnClickListener(v -> {
            if (gameView.performAction(action)) {
                tonePlayer.playTap();
            }
        });
    }

    public void renderSnapshot(BattleManager.HudSnapshot snapshot) {
        activity.runOnUiThread(() -> {
            playerHpValue.setText(snapshot.playerHpText);
            enemyHpValue.setText(snapshot.enemyHpText);
            energyValue.setText(snapshot.energyText);
            favorValue.setText(snapshot.activeFavorText);
            favorMeterValue.setText(snapshot.favorMeterText);
            heroCooldownValue.setText(snapshot.heroCooldownText);
            fireCooldownValue.setText(snapshot.fireCooldownText);
            chainCooldownValue.setText(snapshot.chainCooldownText);
            blessingCooldownValue.setText(snapshot.blessingCooldownText);
            phaseValue.setText(snapshot.phaseText);
            statusValue.setText(snapshot.statusText);
            playerHpBar.setProgress(snapshot.playerHpPercent);
            enemyHpBar.setProgress(snapshot.enemyHpPercent);
            energyBar.setProgress(snapshot.energyPercent);
            overlayController.applyState(snapshot.state, snapshot.victory, snapshot.stats);
        });
    }

    public void refreshMuteButtons() {
        int icon = tonePlayer.isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on;
        menuMute.setImageResource(icon);
        pauseMute.setImageResource(icon);
    }
}
