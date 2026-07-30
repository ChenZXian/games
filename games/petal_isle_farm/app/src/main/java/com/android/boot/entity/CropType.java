package com.android.boot.entity;

public class CropType {
    public final String id;
    public final String displayName;
    public final String category;
    public final int unlockLevel;
    public final int seedCost;
    public final float baseGrowTimeSec;
    public final int waterNeed;
    public final String fertilizerAffinity;
    public final int sellValue;
    public final int xpValue;
    public final String rarity;
    public final float matureBeautyBase;
    public final int storageYield;
    public final String orderTags;
    public final int[] stageColors;
    public final String shapeStyle;

    public CropType(String id, String displayName, String category, int unlockLevel, int seedCost, float baseGrowTimeSec, int waterNeed, String fertilizerAffinity, int sellValue, int xpValue, String rarity, float matureBeautyBase, int storageYield, String orderTags, int[] stageColors, String shapeStyle) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.unlockLevel = unlockLevel;
        this.seedCost = seedCost;
        this.baseGrowTimeSec = baseGrowTimeSec;
        this.waterNeed = waterNeed;
        this.fertilizerAffinity = fertilizerAffinity;
        this.sellValue = sellValue;
        this.xpValue = xpValue;
        this.rarity = rarity;
        this.matureBeautyBase = matureBeautyBase;
        this.storageYield = storageYield;
        this.orderTags = orderTags;
        this.stageColors = stageColors;
        this.shapeStyle = shapeStyle;
    }
}
