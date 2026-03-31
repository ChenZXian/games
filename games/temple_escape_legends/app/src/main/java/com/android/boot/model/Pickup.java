package com.android.boot.model;

public class Pickup {
    public static final int COIN = 0;
    public static final int SHIELD = 1;
    public static final int SLOW = 2;
    public static final int MAGNET = 3;
    public static final int REVIVE = 4;
    public boolean active;
    public int lane;
    public int type;
    public float z;
}
