package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ParticleSystem {
    private final Particle[] particles;
    private final FloatingText[] floatingTexts;

    public ParticleSystem(int particleCount, int textCount) {
        particles = new Particle[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particles[i] = new Particle();
        }
        floatingTexts = new FloatingText[textCount];
        for (int i = 0; i < textCount; i++) {
            floatingTexts[i] = new FloatingText();
        }
    }

    public void clear() {
        for (Particle particle : particles) {
            particle.active = false;
        }
        for (FloatingText floatingText : floatingTexts) {
            floatingText.active = false;
        }
    }

    public void emit(float x, float y, float vx, float vy, float life, float size, int color, boolean ring) {
        for (Particle particle : particles) {
            if (!particle.active) {
                particle.init(x, y, vx, vy, life, size, color, ring);
                return;
            }
        }
    }

    public void text(float x, float y, String label, int color) {
        for (FloatingText floatingText : floatingTexts) {
            if (!floatingText.active) {
                floatingText.init(x, y, label, color);
                return;
            }
        }
    }

    public void update(float dt) {
        for (Particle particle : particles) {
            if (!particle.active) {
                continue;
            }
            particle.life -= dt;
            if (particle.life <= 0f) {
                particle.active = false;
                continue;
            }
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += dt * 18f;
        }
        for (FloatingText floatingText : floatingTexts) {
            if (!floatingText.active) {
                continue;
            }
            floatingText.life -= dt;
            if (floatingText.life <= 0f) {
                floatingText.active = false;
                continue;
            }
            floatingText.y += floatingText.vy * dt;
        }
    }

    public void draw(Canvas canvas, Paint particlePaint, Paint textPaint) {
        for (Particle particle : particles) {
            if (!particle.active) {
                continue;
            }
            float alpha = particle.life / particle.maxLife;
            particlePaint.setColor(particle.color);
            particlePaint.setAlpha((int) (255 * alpha));
            particlePaint.setStyle(particle.ring ? Paint.Style.STROKE : Paint.Style.FILL);
            particlePaint.setStrokeWidth(Math.max(2f, particle.size * 0.15f));
            if (particle.ring) {
                canvas.drawOval(particle.x - particle.size, particle.y - particle.size * 0.55f, particle.x + particle.size, particle.y + particle.size * 0.55f, particlePaint);
            } else {
                canvas.drawOval(particle.x - particle.size, particle.y - particle.size, particle.x + particle.size, particle.y + particle.size, particlePaint);
            }
        }
        textPaint.setStyle(Paint.Style.FILL);
        for (FloatingText floatingText : floatingTexts) {
            if (!floatingText.active) {
                continue;
            }
            textPaint.setColor(floatingText.color);
            textPaint.setAlpha((int) (255 * (floatingText.life / floatingText.maxLife)));
            canvas.drawText(floatingText.text, floatingText.x, floatingText.y, textPaint);
        }
    }
}
