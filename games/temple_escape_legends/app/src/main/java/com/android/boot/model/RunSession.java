package com.android.boot.model;

import com.android.boot.engine.GameMode;

public class RunSession {
    public GameMode mode = GameMode.ENDLESS;
    public int stage = 0;
    public float score;
    public float distance;
    public int coinsRun;
    public int speedTier;
    public float bossPressure;
    public boolean shield;
    public float slowTimer;
    public float magnetTimer;
    public boolean reviveToken;
    public boolean revived;
}
