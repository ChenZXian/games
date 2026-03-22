package com.android.boot.core;

public class MatchStats {
    private int unitsSummoned;
    private int enemiesDefeated;
    private int spellsCast;
    private int fortressDamage;

    public void reset() {
        unitsSummoned = 0;
        enemiesDefeated = 0;
        spellsCast = 0;
        fortressDamage = 0;
    }

    public void addUnitSummoned() {
        unitsSummoned++;
    }

    public void addEnemyDefeated() {
        enemiesDefeated++;
    }

    public void addSpellCast() {
        spellsCast++;
    }

    public void addFortressDamage(int amount) {
        fortressDamage += Math.max(0, amount);
    }

    public int getUnitsSummoned() {
        return unitsSummoned;
    }

    public int getEnemiesDefeated() {
        return enemiesDefeated;
    }

    public int getSpellsCast() {
        return spellsCast;
    }

    public int getFortressDamage() {
        return fortressDamage;
    }
}
