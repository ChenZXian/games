package com.android.boot.core;

public class StatsSnapshot {
  public final int stage;
  public final int score;
  public final int bombs;
  public final float speed;
  public final int shield;
  public final int timeSeconds;

  public StatsSnapshot(int stage, int score, int bombs, float speed, int shield, int timeSeconds) {
    this.stage = stage;
    this.score = score;
    this.bombs = bombs;
    this.speed = speed;
    this.shield = shield;
    this.timeSeconds = timeSeconds;
  }
}
