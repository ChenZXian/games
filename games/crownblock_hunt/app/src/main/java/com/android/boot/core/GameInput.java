package com.android.boot.core;

public class GameInput {
  public boolean left;
  public boolean right;
  public boolean jump;
  public boolean light;
  public boolean heavy;
  public boolean kick;
  public boolean guard;
  public boolean dash;
  public boolean special;
  public boolean shoot;
  public boolean interact;

  public void clear() {
    left = false;
    right = false;
    jump = false;
    light = false;
    heavy = false;
    kick = false;
    guard = false;
    dash = false;
    special = false;
    shoot = false;
    interact = false;
  }
}
