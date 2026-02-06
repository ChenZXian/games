package com.android.boot.entity;

import com.android.boot.core.Direction;

public class WalkerEnemy extends Enemy {
  public Direction desired = Direction.DOWN;
  public float decisionTimer = 0f;
}
