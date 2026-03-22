package com.android.boot.ui;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.boot.core.BattleManager;
import com.android.boot.core.RoyalOath;

public class GameHudController {
    private final BattleManager battleManager;
    private final TextView playerHp;
    private final TextView enemyHp;
    private final TextView phase;
    private final TextView energy;
    private final TextView oathPower;
    private final TextView activeOath;
    private final TextView status;
    private final ProgressBar energyMeter;
    private final ProgressBar oathMeter;
    private final Button meteor;
    private final Button chain;
    private final Button holy;
    private final Button hero;

    public GameHudController(BattleManager battleManager, TextView playerHp, TextView enemyHp, TextView phase, TextView energy, TextView oathPower, TextView activeOath, TextView status, ProgressBar energyMeter, ProgressBar oathMeter, Button meteor, Button chain, Button holy, Button hero) {
        this.battleManager = battleManager;
        this.playerHp = playerHp;
        this.enemyHp = enemyHp;
        this.phase = phase;
        this.energy = energy;
        this.oathPower = oathPower;
        this.activeOath = activeOath;
        this.status = status;
        this.energyMeter = energyMeter;
        this.oathMeter = oathMeter;
        this.meteor = meteor;
        this.chain = chain;
        this.holy = holy;
        this.hero = hero;
    }

    public void refresh() {
        playerHp.setText("Castle " + battleManager.getPlayerHp() + "/" + battleManager.getPlayerMaxHp());
        enemyHp.setText("Stronghold " + battleManager.getEnemyHp() + "/" + battleManager.getEnemyMaxHp());
        phase.setText(battleManager.getBattlePhaseLabel());
        energy.setText("Energy " + battleManager.getEnergy());
        oathPower.setText("Oath Power " + battleManager.getOathPercent());
        activeOath.setText(battleManager.getActiveOath().displayName());
        status.setText(battleManager.getStatusText());
        energyMeter.setProgress(battleManager.getEnergy());
        oathMeter.setProgress(battleManager.getOathPercent());
        meteor.setText("Meteor " + cooldownText(battleManager.getMeteorCooldown()));
        chain.setText("Chain " + cooldownText(battleManager.getChainCooldown()));
        holy.setText("Surge " + cooldownText(battleManager.getHolyCooldown()));
        hero.setText(heroText(battleManager.getActiveOath(), battleManager.getHeroSkillCooldown()));
    }

    private String cooldownText(int cooldown) {
        return cooldown <= 0 ? "Ready" : cooldown + "s";
    }

    private String heroText(RoyalOath oath, int cooldown) {
        return oath.displayName().split(" ")[0] + " " + cooldownText(cooldown);
    }
}
