package com.android.boot.fx;

public class ParticleSystem {
    private final Particle[] particles;
    private final FloatingText[] texts;

    public ParticleSystem(int particleCount, int textCount) {
        particles = new Particle[particleCount];
        texts = new FloatingText[textCount];
        for (int i = 0; i < particleCount; i++) {
            particles[i] = new Particle();
        }
        for (int i = 0; i < textCount; i++) {
            texts[i] = new FloatingText();
        }
    }

    public void spawnBurst(float x, float y, int color, int count, float speed) {
        for (int i = 0; i < count; i++) {
            Particle particle = nextParticle();
            if (particle == null) {
                return;
            }
            float angle = (float) ((Math.PI * 2d * i) / Math.max(1, count));
            particle.set(x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed - speed * 0.3f, 6f + (i % 3), color, 0.45f + (i % 4) * 0.05f);
        }
    }

    public void spawnText(float x, float y, String text, int color) {
        FloatingText floatingText = nextText();
        if (floatingText != null) {
            floatingText.set(x, y, text, color);
        }
    }

    public void update(float dt) {
        for (Particle particle : particles) {
            if (particle.active) {
                particle.update(dt);
            }
        }
        for (FloatingText text : texts) {
            if (text.active) {
                text.update(dt);
            }
        }
    }

    public Particle[] getParticles() {
        return particles;
    }

    public FloatingText[] getTexts() {
        return texts;
    }

    private Particle nextParticle() {
        for (Particle particle : particles) {
            if (!particle.active) {
                return particle;
            }
        }
        return null;
    }

    private FloatingText nextText() {
        for (FloatingText text : texts) {
            if (!text.active) {
                return text;
            }
        }
        return null;
    }
}
