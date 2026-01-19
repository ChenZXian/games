package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {
  private final ToneGenerator toneGenerator;
  private boolean muted;

  public SoundManager() {
    toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  public boolean isMuted() {
    return muted;
  }

  public void playJump() {
    if (!muted) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90);
    }
  }

  public void playDash() {
    if (!muted) {
      toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 120);
    }
  }

  public void playCollect() {
    if (!muted) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 80);
    }
  }

  public void playRune() {
    if (!muted) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT, 140);
    }
  }

  public void playHit() {
    if (!muted) {
      toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 160);
    }
  }

  public void playGameOver() {
    if (!muted) {
      toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 240);
    }
  }
}
