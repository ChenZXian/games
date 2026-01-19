package com.android.boot.core;

import java.util.Random;

public class RandomUtil {
    private final Random random;

    public RandomUtil(long seed) {
        random = new Random(seed);
    }

    public float nextFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public int nextInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public boolean chance(float value) {
        return random.nextFloat() < value;
    }
}
