package com.android.boot.fx;

import com.android.boot.entity.Node;

public class Particle {
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float duration;
  public float travel;
  public float radius;
  public int units;
  public int owner;
  public Node target;

  private Particle() {
  }

  public static Particle create(float startX, float startY, Node target, int units, int owner) {
    Particle particle = new Particle();
    particle.x = startX;
    particle.y = startY;
    particle.target = target;
    particle.units = units;
    particle.owner = owner;
    float dx = target.x - startX;
    float dy = target.y - startY;
    float dist = (float) Math.sqrt(dx * dx + dy * dy);
    particle.duration = Math.max(0.2f, dist / 480f);
    particle.vx = dx / particle.duration;
    particle.vy = dy / particle.duration;
    particle.radius = Math.max(2f, target.radius * 0.12f);
    particle.travel = 0f;
    return particle;
  }
}
