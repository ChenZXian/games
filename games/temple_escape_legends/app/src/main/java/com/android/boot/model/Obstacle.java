package com.android.boot.model;

public class Obstacle {
    public static final int LOW = 0;
    public static final int HIGH = 1;
    public static final int OVERHEAD = 2;
    public static final int GAP = 3;
    public static final int ROLLING = 4;
    public static final int SIDE = 5;
    public static final int JUMP_CHAIN = 6;
    public static final int SLIDE_CHAIN = 7;
    public static final int NARROW = 8;
    public static final int FINISH = 9;
    public boolean active;
    public int lane;
    public int type;
    public float z;
    public float xDrift;
}
