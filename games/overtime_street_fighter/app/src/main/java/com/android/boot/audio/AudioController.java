package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class AudioController {
  private final ToneGenerator toneGenerator;
  private final ToneGenerator bgMusicGenerator;
  private boolean muted;
  private boolean musicPlaying = false;
  private Handler musicHandler;
  private Runnable musicRunnable;
  private int musicNoteIndex = 0;
  private final int[] musicNotes = {
    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
    ToneGenerator.TONE_PROP_BEEP,
    ToneGenerator.TONE_PROP_ACK,
    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
  };

  public AudioController() {
    toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
    bgMusicGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);
    musicHandler = new Handler(Looper.getMainLooper());
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
    if (muted) {
      stopBackgroundMusic();
    } else if (musicPlaying) {
      startBackgroundMusic();
    }
  }

  public boolean isMuted() {
    return muted;
  }

  public void startBackgroundMusic() {
    if (muted || musicPlaying) {
      return;
    }
    musicPlaying = true;
    playMusicLoop();
  }

  private void playMusicLoop() {
    if (!musicPlaying || muted) {
      return;
    }
    try {
      int note = musicNotes[musicNoteIndex % musicNotes.length];
      bgMusicGenerator.startTone(note, 200);
      musicNoteIndex++;
      
      musicRunnable = () -> {
        if (musicPlaying && !muted) {
          playMusicLoop();
        }
      };
      musicHandler.postDelayed(musicRunnable, 400);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stopBackgroundMusic() {
    musicPlaying = false;
    if (musicRunnable != null) {
      musicHandler.removeCallbacks(musicRunnable);
      musicRunnable = null;
    }
  }

  public void pauseBackgroundMusic() {
    musicPlaying = false;
    if (musicRunnable != null) {
      musicHandler.removeCallbacks(musicRunnable);
      musicRunnable = null;
    }
  }

  public void resumeBackgroundMusic() {
    if (!muted && !musicPlaying) {
      startBackgroundMusic();
    }
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

  public void playShoot() {
    if (muted) {
      return;
    }
    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 60);
  }

  public void release() {
    stopBackgroundMusic();
    if (bgMusicGenerator != null) {
      bgMusicGenerator.release();
    }
    toneGenerator.release();
  }
}
