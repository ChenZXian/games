package com.android.boot.core;

public class MatchStats {
    public int unitsSummoned;
    public int enemiesDefeated;
    public int spellsCast;
    public int strongholdDamage;

    public void reset() {
        unitsSummoned = 0;
        enemiesDefeated = 0;
        spellsCast = 0;
        strongholdDamage = 0;
    }
}
