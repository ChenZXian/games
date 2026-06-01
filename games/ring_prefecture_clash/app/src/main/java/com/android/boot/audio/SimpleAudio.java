package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleAudio {
  private final Context context;
  private final SoundPool soundPool;
  private final Map<String, Integer> soundMap = new HashMap<>();
  private MediaPlayer bgmPlayer;
  private boolean muted;

  public SimpleAudio(Context context) {
    this.context = context.getApplicationContext();
    soundPool = new SoundPool.Builder()
      .setMaxStreams(8)
      .setAudioAttributes(new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build())
      .build();
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
    if (bgmPlayer != null) {
      float volume = muted ? 0f : 0.55f;
      bgmPlayer.setVolume(volume, volume);
    }
  }

  public boolean isMuted() {
    return muted;
  }

  public void playBgm(String assetName) {
    if (assetName == null || assetName.isEmpty()) {
      return;
    }
    stopBgm();
    try {
      AssetFileDescriptor afd = context.getAssets().openFd("audio/" + assetName);
      bgmPlayer = new MediaPlayer();
      bgmPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
      afd.close();
      bgmPlayer.setLooping(true);
      bgmPlayer.prepare();
      float volume = muted ? 0f : 0.55f;
      bgmPlayer.setVolume(volume, volume);
      bgmPlayer.start();
    } catch (IOException ignored) {
    }
  }

  public void stopBgm() {
    if (bgmPlayer != null) {
      try {
        bgmPlayer.stop();
      } catch (IllegalStateException ignored) {
      }
      bgmPlayer.release();
      bgmPlayer = null;
    }
  }

  public void playSfx(String assetName) {
    if (muted || assetName == null || assetName.isEmpty()) {
      return;
    }
    Integer soundId = soundMap.get(assetName);
    if (soundId == null) {
      try {
        AssetFileDescriptor afd = context.getAssets().openFd("audio/" + assetName);
        soundId = soundPool.load(afd, 1);
        afd.close();
        soundMap.put(assetName, soundId);
      } catch (IOException ignored) {
        return;
      }
    }
    soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1f);
  }

  public void release() {
    stopBgm();
    soundPool.release();
  }
}
