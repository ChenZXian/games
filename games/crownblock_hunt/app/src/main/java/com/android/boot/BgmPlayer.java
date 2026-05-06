package com.android.boot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

public class BgmPlayer {
  private static final float DEFAULT_VOLUME = 0.32f;
  private MediaPlayer player;
  private boolean muted;

  public void start(Context context) {
    if (player != null) {
      return;
    }
    String[] audioFiles = {
        "audio/bgm_gameplay.wav",
        "audio/bgm_menu.wav",
        "audio/bgm.wav",
        "audio/bgm.ogg"
    };
    for (String audioFile : audioFiles) {
      try {
        AssetFileDescriptor descriptor = context.getAssets().openFd(audioFile);
        player = new MediaPlayer();
        player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        player.setLooping(true);
        float volume = muted ? 0f : DEFAULT_VOLUME;
        player.setVolume(volume, volume);
        player.prepare();
        player.start();
        descriptor.close();
        return;
      } catch (IOException ignored) {
        if (player != null) {
          try {
            player.release();
          } catch (Throwable ignoredRelease) {
          }
          player = null;
        }
      }
    }
  }

  public void stop() {
    if (player == null) {
      return;
    }
    try {
      if (player.isPlaying()) {
        player.stop();
      }
      player.release();
    } catch (Throwable ignored) {
    }
    player = null;
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
    if (player != null) {
      float volume = muted ? 0f : DEFAULT_VOLUME;
      player.setVolume(volume, volume);
    }
  }

  public boolean isMuted() {
    return muted;
  }
}
