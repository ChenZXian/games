package com.android.boot.entity;

public class DeliveryOrder {
    public AnimalSpecies species;
    public int quantity;
    public int rewardCoins;
    public int rewardXp;
    public float streakBonus;
    public float urgency;
    public boolean urgent;

    public DeliveryOrder(AnimalSpecies species, int quantity, int rewardCoins, int rewardXp, float streakBonus, float urgency, boolean urgent) {
        this.species = species;
        this.quantity = quantity;
        this.rewardCoins = rewardCoins;
        this.rewardXp = rewardXp;
        this.streakBonus = streakBonus;
        this.urgency = urgency;
        this.urgent = urgent;
    }
}
