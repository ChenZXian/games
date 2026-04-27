package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.IOException;

public class AudioController {
  private final SoundPool soundPool;
  private int hitSoundId;
  private int heavySoundId;
  private int shootSoundId;
  private int pickupSoundId;
  private int uiClickSoundId;
  private int winSoundId;
  private int failSoundId;
  private boolean muted;
  private boolean loaded;

  public AudioController() {
    AudioAttributes attributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();
    soundPool = new SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(attributes)
        .build();
  }

  public void load(Context context) {
    if (loaded) {
      return;
    }
    hitSoundId = loadSound(context, "audio/sfx_hit.wav");
    heavySoundId = loadSound(context, "audio/sfx_warning.wav");
    shootSoundId = loadSound(context, "audio/sfx_gunshot.wav");
    pickupSoundId = loadSound(context, "audio/sfx_pickup.wav");
    uiClickSoundId = loadSound(context, "audio/sfx_ui_click.wav");
    winSoundId = loadSound(context, "audio/sfx_win.wav");
    failSoundId = loadSound(context, "audio/sfx_fail.wav");
    loaded = true;
  }

  private int loadSound(Context context, String assetPath) {
    try {
      AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath);
      int soundId = soundPool.load(descriptor, 1);
      descriptor.close();
      return soundId;
    } catch (IOException ignored) {
      return 0;
    }
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  public boolean isMuted() {
    return muted;
  }

  public void playUiClick() {
    play(uiClickSoundId, 0.45f, 1f);
  }

  public void playHit() {
    play(hitSoundId, 0.8f, 1f);
  }

  public void playHeavy() {
    play(heavySoundId, 0.75f, 0.92f);
  }

  public void playShoot() {
    play(shootSoundId, 0.65f, 1f);
  }

  public void playPickup() {
    play(pickupSoundId, 0.55f, 1.05f);
  }

  public void playWin() {
    play(winSoundId, 0.7f, 1f);
  }

  public void playFail() {
    play(failSoundId, 0.8f, 0.9f);
  }

  private void play(int soundId, float volume, float rate) {
    if (muted || soundId == 0) {
      return;
    }
    soundPool.play(soundId, volume, volume, 1, 0, rate);
  }

  public void release() {
    soundPool.release();
  }
}
