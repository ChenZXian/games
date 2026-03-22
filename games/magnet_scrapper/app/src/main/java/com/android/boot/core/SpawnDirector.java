package com.android.boot.core;

import android.util.DisplayMetrics;
import com.android.boot.entity.EnemyDrone;
import java.util.Random;

public class SpawnDirector {
  private final Random random = new Random();
  private float waveTimer;
  private float spawnTimer;
  private int wave = 1;

  public void reset() {
    waveTimer = 0f;
    spawnTimer = 1.2f;
    wave = 1;
  }

  public int getWave() {
    return wave;
  }

  public void update(float delta) {
    waveTimer += delta;
    while (waveTimer >= 20f) {
      waveTimer -= 20f;
      wave++;
    }
    spawnTimer -= delta;
  }

  public boolean shouldSpawn(int enemyCount) {
    float targetDelay = Math.max(0.45f, 1.5f - (wave - 1) * 0.08f);
    if (spawnTimer <= 0f && enemyCount < 8 + wave * 3) {
      spawnTimer = targetDelay;
      return true;
    }
    return false;
  }

  public void populateEnemy(EnemyDrone enemy, float width, float height, DisplayMetrics metrics) {
    int edge = random.nextInt(4);
    float margin = metrics.density * 24f;
    float x;
    float y;
    if (edge == 0) {
      x = -margin;
      y = random.nextFloat() * height;
    } else if (edge == 1) {
      x = width + margin;
      y = random.nextFloat() * height;
    } else if (edge == 2) {
      x = random.nextFloat() * width;
      y = -margin;
    } else {
      x = random.nextFloat() * width;
      y = height + margin;
    }
    int pick = random.nextInt(100);
    int type;
    if (pick < 45) {
      type = EnemyDrone.TYPE_SCOUT;
    } else if (pick < 72) {
      type = EnemyDrone.TYPE_LEECH;
    } else {
      type = EnemyDrone.TYPE_TANK;
    }
    enemy.activate(type, x, y, wave);
  }
}
