package com.android.boot.entity;

public class Ingredient {
    public final String id;
    public final String name;
    public final String category;
    public final boolean cuttable;
    public final boolean cookable;
    public final int quality;

    public Ingredient(String id, String name, String category, boolean cuttable, boolean cookable, int quality) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.cuttable = cuttable;
        this.cookable = cookable;
        this.quality = quality;
    }
}
