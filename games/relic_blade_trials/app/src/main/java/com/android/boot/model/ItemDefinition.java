package com.android.boot.model;

import com.android.boot.model.Enums.Rarity;
import com.android.boot.model.Enums.SlotType;

public class ItemDefinition {
    public final String id;
    public final String name;
    public final SlotType slot;
    public final Rarity rarity;
    public final int stageTier;
    public final int power;
    public final int hp;
    public final int attack;
    public final int defense;
    public final float moveSpeed;
    public final float critChance;
    public final float critDamage;
    public final int sellValue;
    public final String description;

    public ItemDefinition(String id, String name, SlotType slot, Rarity rarity, int stageTier, int power, int hp, int attack, int defense, float moveSpeed, float critChance, float critDamage, int sellValue, String description) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.rarity = rarity;
        this.stageTier = stageTier;
        this.power = power;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.moveSpeed = moveSpeed;
        this.critChance = critChance;
        this.critDamage = critDamage;
        this.sellValue = sellValue;
        this.description = description;
    }
}
