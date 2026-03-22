package com.android.boot.fx;

public class ParticleBurst {
  public static final int MAX_PARTICLES = 96;
  public final boolean[] active = new boolean[MAX_PARTICLES];
  public final float[] x = new float[MAX_PARTICLES];
  public final float[] y = new float[MAX_PARTICLES];
  public final float[] vx = new float[MAX_PARTICLES];
  public final float[] vy = new float[MAX_PARTICLES];
  public final float[] life = new float[MAX_PARTICLES];
  public final float[] size = new float[MAX_PARTICLES];
  public final int[] color = new int[MAX_PARTICLES];

  public void emit(float cx, float cy, int tint, float speed, float particleSize, int count) {
    for (int i = 0; i < MAX_PARTICLES && count > 0; i++) {
      if (!active[i]) {
        double angle = i * 0.6544984695 + cx * 0.01 + cy * 0.008;
        active[i] = true;
        x[i] = cx;
        y[i] = cy;
        vx[i] = (float) Math.cos(angle) * speed * (0.55f + (i % 5) * 0.12f);
        vy[i] = (float) Math.sin(angle) * speed * (0.55f + (i % 7) * 0.09f);
        life[i] = 0.35f + (i % 4) * 0.08f;
        size[i] = particleSize * (0.8f + (i % 3) * 0.3f);
        color[i] = tint;
        count--;
      }
    }
  }

  public void update(float delta) {
    for (int i = 0; i < MAX_PARTICLES; i++) {
      if (active[i]) {
        life[i] -= delta;
        if (life[i] <= 0f) {
          active[i] = false;
        } else {
          x[i] += vx[i] * delta;
          y[i] += vy[i] * delta;
          vx[i] *= 0.97f;
          vy[i] *= 0.97f;
        }
      }
    }
  }
}
