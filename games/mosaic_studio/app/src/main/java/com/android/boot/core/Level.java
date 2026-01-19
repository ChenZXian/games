package com.android.boot.core;

public class Level {
    public final int size;
    public final int[] palette;
    public final int[] target;
    public final int maxMoves;
    public final int maxPaint;
    public final int timeLimitSec;

    public Level(int size, int[] palette, int[] target, int maxMoves, int maxPaint, int timeLimitSec) {
        this.size = size;
        this.palette = palette;
        this.target = target;
        this.maxMoves = maxMoves;
        this.maxPaint = maxPaint;
        this.timeLimitSec = timeLimitSec;
    }
}
