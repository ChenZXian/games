package com.android.boot.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.android.boot.R;
import com.android.boot.core.BattleManager;
import com.android.boot.core.RuneFavor;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class GameHudController {
    private final Activity activity;
    private final View root;
    private final GameView gameView;
    private TextView textPlayerHp;
    private TextView textEnemyHp;
    private TextView textEnergy;
    private TextView textFavor;
    private TextView textHeroCooldown;
    private TextView textPhase;
    private TextView textStatus;

    public GameHudController(Activity activity, View root, GameView gameView) {
        this.activity = activity;
        this.root = root;
        this.gameView = gameView;
    }

    public void bind() {
        textPlayerHp = root.findViewById(R.id.text_player_hp);
        textEnemyHp = root.findViewById(R.id.text_enemy_hp);
        textEnergy = root.findViewById(R.id.text_energy);
        textFavor = root.findViewById(R.id.text_favor);
        textHeroCooldown = root.findViewById(R.id.text_hero_cooldown);
        textPhase = root.findViewById(R.id.text_phase);
        textStatus = root.findViewById(R.id.text_status);
        bindButton(R.id.button_pause, () -> gameView.pauseMatch());
        bindButton(R.id.button_militia, () -> gameView.queueSummon(BattleManager.SUMMON_MILITIA));
        bindButton(R.id.button_archer, () -> gameView.queueSummon(BattleManager.SUMMON_ARCHER));
        bindButton(R.id.button_knight, () -> gameView.queueSummon(BattleManager.SUMMON_KNIGHT));
        bindButton(R.id.button_priest, () -> gameView.queueSummon(BattleManager.SUMMON_PRIEST));
        bindButton(R.id.button_titan, () -> gameView.queueSummon(BattleManager.SUMMON_TITAN));
        bindButton(R.id.button_flame, () -> gameView.selectFavor(RuneFavor.FLAME));
        bindButton(R.id.button_storm, () -> gameView.selectFavor(RuneFavor.STORM));
        bindButton(R.id.button_vital, () -> gameView.selectFavor(RuneFavor.VITAL));
        bindButton(R.id.button_fire_rain, () -> gameView.castSpell(BattleManager.SPELL_FIRE_RAIN));
        bindButton(R.id.button_chain_bolt, () -> gameView.castSpell(BattleManager.SPELL_CHAIN_BOLT));
        bindButton(R.id.button_blessing_wave, () -> gameView.castSpell(BattleManager.SPELL_BLESSING_WAVE));
        bindButton(R.id.button_hero, () -> gameView.triggerHeroAbility());
    }

    private void bindButton(int id, Runnable runnable) {
        MaterialButton button = root.findViewById(id);
        button.setOnClickListener(v -> runnable.run());
    }

    public void update(BattleManager.HudSnapshot snapshot) {
        activity.runOnUiThread(() -> {
            textPlayerHp.setText(snapshot.playerHp + " / " + snapshot.playerMaxHp);
            textEnemyHp.setText(snapshot.enemyHp + " / " + snapshot.enemyMaxHp);
            textEnergy.setText(String.format(Locale.US, "%d / %d", snapshot.energy, snapshot.maxEnergy));
            textFavor.setText(snapshot.favorLabel + "  " + snapshot.favorPercent + "%");
            textHeroCooldown.setText(snapshot.heroCooldownLabel);
            textPhase.setText(snapshot.phaseLabel);
            textStatus.setText(snapshot.statusText + "  Fire " + snapshot.fireCooldown + "  Bolt " + snapshot.chainCooldown + "  Bless " + snapshot.blessingCooldown);
        });
    }
}
