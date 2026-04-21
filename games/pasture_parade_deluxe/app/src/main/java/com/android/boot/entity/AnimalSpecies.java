package com.android.boot.entity;

public enum AnimalSpecies {
    CHICKEN(8f, 8, 24, 0.30f, 0.25f, 0.95f, 0xFFFEE07A),
    DUCK(10f, 9, 30, 0.33f, 0.25f, 0.92f, 0xFFE8FF7A),
    GOOSE(12f, 10, 34, 0.35f, 0.28f, 0.91f, 0xFFFFFFFF),
    RABBIT(11f, 12, 40, 0.40f, 0.22f, 1.15f, 0xFFFFF2F2),
    SHEEP(14f, 14, 48, 0.28f, 0.34f, 0.82f, 0xFFF6F9FF),
    GOAT(16f, 16, 56, 0.34f, 0.35f, 1.01f, 0xFFDCD9CF),
    PIG(18f, 18, 64, 0.42f, 0.42f, 0.84f, 0xFFFFB6CC),
    COW(20f, 22, 78, 0.45f, 0.45f, 0.75f, 0xFFF7F5EE),
    ALPACA(21f, 24, 92, 0.31f, 0.35f, 0.94f, 0xFFF7EFE2),
    TURKEY(17f, 21, 86, 0.37f, 0.37f, 0.88f, 0xFFB86A59),
    PEACOCK(19f, 26, 112, 0.36f, 0.39f, 0.98f, 0xFF45BBA9),
    MINI_DONKEY(23f, 30, 134, 0.48f, 0.36f, 1.30f, 0xFFC8C0B6);

    public final float productionSeconds;
    public final int baseValue;
    public final int unlockLevel;
    public final float hungerRate;
    public final float dirtRate;
    public final float moveFlavor;
    public final int color;

    AnimalSpecies(float productionSeconds, int baseValue, int unlockLevel, float hungerRate, float dirtRate, float moveFlavor, int color) {
        this.productionSeconds = productionSeconds;
        this.baseValue = baseValue;
        this.unlockLevel = unlockLevel;
        this.hungerRate = hungerRate;
        this.dirtRate = dirtRate;
        this.moveFlavor = moveFlavor;
        this.color = color;
    }

    public boolean isBird() {
        return this == CHICKEN || this == DUCK || this == GOOSE || this == TURKEY || this == PEACOCK;
    }

    public boolean isWool() {
        return this == SHEEP || this == GOAT || this == ALPACA;
    }

    public boolean isPremium() {
        return ordinal() >= ALPACA.ordinal();
    }
}
