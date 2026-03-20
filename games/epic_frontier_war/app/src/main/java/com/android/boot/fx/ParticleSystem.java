package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ParticleSystem {
    private final Particle[] particles;

    public ParticleSystem(int capacity) {
        particles = new Particle[capacity];
        for (int i = 0; i < capacity; i++) {
            particles[i] = new Particle();
        }
    }

    public void burst(float x, float y, int color, int count, float speed, boolean ring) {
        for (int i = 0; i < particles.length && count > 0; i++) {
            Particle particle = particles[i];
            if (!particle.active) {
                float angle = (float) (Math.PI * 2d * (count * 0.17d + i * 0.11d));
                float vx = (float) Math.cos(angle) * speed;
                float vy = (float) Math.sin(angle) * speed - speed * 0.3f;
                particle.init(x, y, vx, vy, ring ? 0.55f : 0.75f, ring ? 24f : 7f + (i % 3) * 3f, color, ring);
                count--;
            }
        }
    }

    public void update(float dt) {
        for (Particle particle : particles) {
            particle.update(dt);
        }
    }

    public void render(Canvas canvas, Paint paint) {
        for (Particle particle : particles) {
            particle.render(canvas, paint);
        }
    }
}
