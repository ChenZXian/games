package com.android.boot.core;

import com.android.boot.entity.Enemy;
import java.util.ArrayList;
import java.util.List;

public class WaveScript {
    public static class Entry {
        public Enemy.Type type;
        public int count;
        public float interval;

        public Entry(Enemy.Type type, int count, float interval) {
            this.type = type;
            this.count = count;
            this.interval = interval;
        }
    }

    public final List<Entry> entries = new ArrayList<>();
}
