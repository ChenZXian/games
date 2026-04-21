package com.android.boot.entity;

public class Pen {
    public final int id;
    public final Animal animal;
    public boolean unlocked;
    public int level;
    public float cleanliness;
    public float feed;
    public float produce;
    public int storedGoods;
    public int capacity;
    public float speedBonus;
    public float valueBonus;
    public float autoClean;
    public float feedDuration;
    public float x;
    public float y;
    public float w;
    public float h;

    public Pen(int id, Animal animal, boolean unlocked, float x, float y, float w, float h) {
        this.id = id;
        this.animal = animal;
        this.unlocked = unlocked;
        this.level = 1;
        this.cleanliness = 1f;
        this.feed = 1f;
        this.produce = 0f;
        this.storedGoods = 0;
        this.capacity = 3;
        this.speedBonus = 1f;
        this.valueBonus = 1f;
        this.autoClean = 0f;
        this.feedDuration = 1f;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void update(float dt, float adjacencySpeed, float adjacencyValue) {
        if (!unlocked) {
            return;
        }
        feed -= dt * animal.species.hungerRate / feedDuration;
        cleanliness -= dt * animal.species.dirtRate * (1f - autoClean);
        if (feed < 0f) {
            feed = 0f;
        }
        if (cleanliness < 0f) {
            cleanliness = 0f;
        }
        float speed = speedBonus * adjacencySpeed;
        produce += dt * speed;
        if (produce >= animal.species.productionSeconds && storedGoods < capacity) {
            produce -= animal.species.productionSeconds;
            storedGoods++;
        }
        animal.update(dt, storedGoods > 0, isNeglected());
    }

    public boolean isNeglected() {
        return feed < 0.2f || cleanliness < 0.2f || storedGoods >= capacity;
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px <= x + w && py <= y + h;
    }
}
