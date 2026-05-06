package com.android.boot.core;

public enum CampStructureType {
    SHELTER("Shelter", 4, 1, 0, 0, 0),
    FENCE("Fence", 3, 2, 0, 0, 0),
    COLLECTOR("Rain Collector", 2, 0, 0, 2, 0),
    INFIRMARY("Infirmary", 3, 1, 2, 0, 0),
    WORKSHOP("Workshop", 4, 1, 2, 0, 1),
    BEACON("Beacon", 6, 0, 3, 0, 4);

    public final String label;
    public final int woodCost;
    public final int foodCost;
    public final int herbCost;
    public final int waterCost;
    public final int scrapCost;

    CampStructureType(String label, int woodCost, int foodCost, int herbCost, int waterCost, int scrapCost) {
        this.label = label;
        this.woodCost = woodCost;
        this.foodCost = foodCost;
        this.herbCost = herbCost;
        this.waterCost = waterCost;
        this.scrapCost = scrapCost;
    }
}
