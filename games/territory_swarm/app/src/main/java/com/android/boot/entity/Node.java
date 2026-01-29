package com.android.boot.entity;

public class Node {
  public float x;
  public float y;
  public float radius;
  public int owner;
  public int units;
  public float growthRate;
  public float growthBuffer;
  public boolean selected;

  public Node(float x, float y, float radius) {
    this.x = x;
    this.y = y;
    this.radius = radius;
  }
}
