package com.android.boot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

public class BgmPlayer {
  private MediaPlayer player;
  private boolean muted = false;
  private static final float DEFAULT_VOLUME = 0.35f;

  public void start(Context context) {
    if (player != null) return;
    String[] candidates = new String[] {
        "audio/bgm.mp3",
        "audio/bgm.ogg",
        "audio/bgm.wav"
    };
    for (String path : candidates) {
      try {
        AssetFileDescriptor afd = context.getAssets().openFd(path);
        MediaPlayer p = new MediaPlayer();
        p.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        p.setLooping(true);
        float vol = muted ? 0f : DEFAULT_VOLUME;
        p.setVolume(vol, vol);
        p.prepare();
        p.start();
        afd.close();
        player = p;
        return;
      } catch (IOException ignored) {
        if (player != null) {
          try { player.release(); } catch (Throwable ignored2) {}
          player = null;
        }
      }
    }
  }

  public void stop() {
    if (player == null) return;
    try {
      if (player.isPlaying()) player.stop();
      player.release();
    } catch (Throwable ignored) {
    }
    player = null;
  }

  public void pause() {
    if (player != null && player.isPlaying()) {
      try { player.pause(); } catch (Throwable ignored) {}
    }
  }

  public void resume() {
    if (player != null && !player.isPlaying() && !muted) {
      try { player.start(); } catch (Throwable ignored) {}
    }
  }

  public void setMuted(boolean m) {
    muted = m;
    if (player != null) {
      float vol = muted ? 0f : DEFAULT_VOLUME;
      player.setVolume(vol, vol);
      if (muted && player.isPlaying()) {
        pause();
      } else if (!muted && !player.isPlaying()) {
        resume();
      }
    }
  }

  public boolean isMuted() {
    return muted;
  }
}

