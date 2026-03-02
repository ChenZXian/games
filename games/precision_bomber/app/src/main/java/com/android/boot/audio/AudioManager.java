package com.android.boot.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.android.boot.R;

public class AudioManager {
  private final SoundPool soundPool;
  private final int sPlaceBomb;
  private final int sExplosion;
  private final int sPickup;
  private final int sHit;
  private boolean muted;

  public AudioManager(Context context) {
    AudioAttributes attrs = new AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build();
    soundPool = new SoundPool.Builder()
      .setAudioAttributes(attrs)
      .setMaxStreams(4)
      .build();
    sPlaceBomb = 0;
    sExplosion = 0;
    sPickup = 0;
    sHit = 0;
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  private void play(int soundId) {
    if (muted || soundId == 0) {
      return;
    }
    soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
  }

  public void playPlaceBomb() {
    play(sPlaceBomb);
  }

  public void playExplosion() {
    play(sExplosion);
  }

  public void playPickup() {
    play(sPickup);
  }

  public void playHit() {
    play(sHit);
  }
}
