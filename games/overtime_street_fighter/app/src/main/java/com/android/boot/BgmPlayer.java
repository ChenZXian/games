package com.android.boot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import java.io.IOException;

public class BgmPlayer {
  private MediaPlayer player;
  private boolean muted = false;
  private static final float DEFAULT_VOLUME = 0.3f;

  public void start(Context context) {
    if (player != null) {
      return;
    }
    String[] audioFiles = {"audio/bgm.ogg", "audio/bgm.wav"};
    for (String audioFile : audioFiles) {
      try {
        AssetFileDescriptor afd = context.getAssets().openFd(audioFile);
        player = new MediaPlayer();
        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        player.setLooping(true);
        player.setVolume(muted ? 0f : DEFAULT_VOLUME, muted ? 0f : DEFAULT_VOLUME);
        player.setOnCompletionListener(mp -> {
          if (mp != null && mp.isLooping()) {
            mp.seekTo(0);
          }
        });
        player.prepare();
        player.start();
        afd.close();
        return;
      } catch (IOException ignored) {
        if (player != null) {
          try {
            player.release();
          } catch (Throwable ignored2) {
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

  public void setMuted(boolean m) {
    muted = m;
    if (player != null) {
      float vol = muted ? 0f : DEFAULT_VOLUME;
      player.setVolume(vol, vol);
    }
  }

  public boolean isMuted() {
    return muted;
  }
}


