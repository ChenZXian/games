package com.android.boot.entity;

public enum WeatherState {
    CLEAR(1f, 1f, 1f),
    CLOUDY(1f, 0.95f, 1f),
    LIGHT_RAIN(1.2f, 1.05f, 1.02f),
    SUNNY_BURST(1.15f, 1.2f, 1f),
    RAINBOW_DEW(1.25f, 1.3f, 1.15f),
    GOLDEN_SUN(1.2f, 1.35f, 1.2f),
    MOONLIT_MIST(1.1f, 1.25f, 1.1f),
    PETAL_WIND(1.12f, 1.22f, 1.18f);

    public final float growthBoost;
    public final float beautyBoost;
    public final float rareBoost;

    WeatherState(float growthBoost, float beautyBoost, float rareBoost) {
        this.growthBoost = growthBoost;
        this.beautyBoost = beautyBoost;
        this.rareBoost = rareBoost;
    }
}
