package com.android.boot.entity;

public class Animal {
    public enum Mood {
        IDLE,
        MOVE,
        READY,
        HAPPY,
        NEGLECTED
    }

    public final AnimalSpecies species;
    public Mood mood = Mood.IDLE;
    public float x;
    public float y;
    public float blinkTimer = 1.2f;
    public float bobTime;
    public float moveTimer;
    public float happyTimer;

    public Animal(AnimalSpecies species, float x, float y) {
        this.species = species;
        this.x = x;
        this.y = y;
    }

    public void update(float dt, boolean ready, boolean neglected) {
        bobTime += dt * (0.8f + species.moveFlavor * 0.2f);
        blinkTimer -= dt;
        moveTimer -= dt;
        if (blinkTimer <= 0f) {
            blinkTimer = 1.6f + (species.ordinal() % 4) * 0.3f;
        }
        if (happyTimer > 0f) {
            happyTimer -= dt;
            mood = Mood.HAPPY;
            return;
        }
        if (neglected) {
            mood = Mood.NEGLECTED;
            return;
        }
        if (ready) {
            mood = Mood.READY;
            return;
        }
        if (moveTimer <= 0f) {
            moveTimer = 0.6f + (species.ordinal() % 5) * 0.15f;
            mood = mood == Mood.MOVE ? Mood.IDLE : Mood.MOVE;
        }
    }

    public void onTap() {
        happyTimer = 0.8f;
    }
}
