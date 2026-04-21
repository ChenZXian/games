package com.android.boot.fx;

public class ParticleSystem {
    public final float[] x = new float[64];
    public final float[] y = new float[64];
    public final float[] vx = new float[64];
    public final float[] vy = new float[64];
    public final float[] life = new float[64];

    public void burst(float px, float py) {
        for (int i = 0; i < life.length; i++) {
            if (life[i] <= 0f) {
                x[i] = px;
                y[i] = py;
                vx[i] = (i % 5 - 2) * 25f;
                vy[i] = -30f - (i % 3) * 20f;
                life[i] = 0.7f;
            }
        }
    }

    public void update(float dt) {
        for (int i = 0; i < life.length; i++) {
            if (life[i] > 0f) {
                life[i] -= dt;
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
                vy[i] += 90f * dt;
            }
        }
    }
}
