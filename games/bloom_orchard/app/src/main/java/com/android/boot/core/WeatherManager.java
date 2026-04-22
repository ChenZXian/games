package com.android.boot.core;

import com.android.boot.entity.WeatherState;

import java.util.Random;

public class WeatherManager {
    private final Random random = new Random();
    public WeatherState state = WeatherState.CLEAR;
    private float timer;

    public void update(float dt) {
        timer += dt;
        if (timer > 20f) {
            timer = 0f;
            int roll = random.nextInt(100);
            if (roll < 30) state = WeatherState.CLEAR;
            else if (roll < 50) state = WeatherState.CLOUDY;
            else if (roll < 70) state = WeatherState.LIGHT_RAIN;
            else if (roll < 84) state = WeatherState.SUNNY_BURST;
            else if (roll < 90) state = WeatherState.RAINBOW_DEW;
            else if (roll < 95) state = WeatherState.GOLDEN_SUN;
            else if (roll < 98) state = WeatherState.MOONLIT_MIST;
            else state = WeatherState.PETAL_WIND;
        }
    }
}
