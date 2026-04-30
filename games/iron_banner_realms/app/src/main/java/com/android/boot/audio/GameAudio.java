package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;

public class GameAudio {
  private final Context context;
  private final SoundPool soundPool;
  private final HashMap<String, Integer> soundIds = new HashMap<>();
  private MediaPlayer mediaPlayer;
  private String currentRole = "";
  private boolean paused;

  public GameAudio(Context context) {
    this.context = context.getApplicationContext();
    AudioAttributes attrs = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();
    soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build();
    loadSound("ui_click", "audio/sfx_ui_click.wav");
    loadSound("dispatch", "audio/sfx_move.wav");
    loadSound("battle", "audio/sfx_attack.wav");
    loadSound("capture", "audio/sfx_capture.wav");
    loadSound("repair", "audio/sfx_build.wav");
    loadSound("win", "audio/sfx_win.wav");
    loadSound("fail", "audio/sfx_fail.wav");
  }

  private void loadSound(String key, String assetPath) {
    try {
      AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
      soundIds.put(key, soundPool.load(afd, 1));
    } catch (IOException ignored) {
    }
  }

  private void playLoop(String assetPath, String role) {
    if (currentRole.equals(role) && mediaPlayer != null && mediaPlayer.isPlaying()) {
      return;
    }
    releasePlayer();
    try {
      AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
      mediaPlayer = new MediaPlayer();
      mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
      mediaPlayer.setLooping(true);
      mediaPlayer.setVolume(0.5f, 0.5f);
      mediaPlayer.prepare();
      if (!paused) {
        mediaPlayer.start();
      }
      currentRole = role;
    } catch (IOException ignored) {
      releasePlayer();
    }
  }

  private void playSound(String key) {
    Integer soundId = soundIds.get(key);
    if (soundId != null) {
      soundPool.play(soundId, 0.72f, 0.72f, 1, 0, 1f);
    }
  }

  public void playMenu() {
    playLoop("audio/bgm_menu.wav", "menu");
  }

  public void playGameplay() {
    playLoop("audio/bgm.wav", "play");
  }

  public void playClick() {
    playSound("ui_click");
  }

  public void playDispatch() {
    playSound("dispatch");
  }

  public void playBattle() {
    playSound("battle");
  }

  public void playCapture() {
    playSound("capture");
  }

  public void playRepair() {
    playSound("repair");
  }

  public void playWin() {
    playSound("win");
  }

  public void playFail() {
    playSound("fail");
  }

  public void onPause() {
    paused = true;
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
    }
  }

  public void onResume() {
    paused = false;
    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
      mediaPlayer.start();
    }
  }

  private void releasePlayer() {
    if (mediaPlayer != null) {
      mediaPlayer.stop();
      mediaPlayer.release();
      mediaPlayer = null;
    }
    currentRole = "";
  }
}
