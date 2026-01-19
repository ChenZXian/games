package com.android.boot.core;

import java.util.Random;

public class RandomUtil {
  private final Random random;

  public RandomUtil(long seed) {
    random = new Random(seed);
  }

  public int nextInt(int bound) {
    return random.nextInt(bound);
  }

  public float nextFloat() {
    return random.nextFloat();
  }

  public float range(float min, float max) {
    return min + random.nextFloat() * (max - min);
  }

  public boolean chance(float odds) {
    return random.nextFloat() < odds;
  }
}
