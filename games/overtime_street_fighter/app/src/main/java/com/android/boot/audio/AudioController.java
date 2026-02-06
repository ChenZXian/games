package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class AudioController {
  private final ToneGenerator toneGenerator;
  private boolean muted;

  public AudioController() {
    toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  public boolean isMuted() {
    return muted;
  }

  public void playHit() {
    if (muted) {
      return;
    }
    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 80);
  }

  public void playSpecial() {
    if (muted) {
      return;
    }
    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
  }

  public void release() {
    toneGenerator.release();
  }
}
