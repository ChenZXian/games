package com.android.boot.core;

public class UpgradeSystem {
    public float comboRetention;
    public float neglectResistance;
    public float deliveryBonus;
    public float premiumChance;
    public float careEfficiency;

    public void buyGlobal(int type) {
        if (type == 0) comboRetention += 0.08f;
        if (type == 1) neglectResistance += 0.06f;
        if (type == 2) deliveryBonus += 0.08f;
        if (type == 3) premiumChance += 0.05f;
        if (type == 4) careEfficiency += 0.1f;
    }
}
