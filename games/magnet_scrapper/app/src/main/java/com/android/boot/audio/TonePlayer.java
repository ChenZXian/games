package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
  private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
  private boolean enabled = true;

  public boolean toggle() {
    enabled = !enabled;
    return enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void pulse() {
    if (enabled) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
    }
  }

  public void hit() {
    if (enabled) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 80);
    }
  }

  public void blast() {
    if (enabled) {
      toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 90);
    }
  }

  public void release() {
    toneGenerator.release();
  }
}
