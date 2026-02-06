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

  public void resetActions() {
    light = false;
    heavy = false;
    kick = false;
    dash = false;
    special = false;
  }
}
