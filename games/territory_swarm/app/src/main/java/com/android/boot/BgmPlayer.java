package com.android.boot;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class BgmPlayer {
  private AudioTrack track;
  private boolean started = false;
  private boolean muted = false;
  private static final float DEFAULT_VOLUME = 0.25f;

  public void start(Context context) {
    if (started && track != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
      return;
    }
    if (track != null) {
      stop();
    }
    started = true;
    track = buildTrack();
    if (track != null) {
      track.play();
    }
  }

  public void stop() {
    started = false;
    AudioTrack t = track;
    track = null;
    if (t == null) {
      return;
    }
    try {
      if (t.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
        t.pause();
        t.flush();
      }
    } catch (Throwable ignored) {
    }
    try {
      t.release();
    } catch (Throwable ignored) {
    }
  }

  public void setMuted(boolean m) {
    muted = m;
    if (track != null) {
      float vol = muted ? 0f : DEFAULT_VOLUME;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        track.setVolume(vol);
      } else {
        track.setStereoVolume(vol, vol);
      }
    }
  }

  public boolean isMuted() {
    return muted;
  }

  private AudioTrack buildTrack() {
    int sampleRate = 22050;
    int channelMask = AudioFormat.CHANNEL_OUT_MONO;
    int encoding = AudioFormat.ENCODING_PCM_16BIT;
    // 8 second loop for epic war music
    int seconds = 8;
    int frames = sampleRate * seconds;
    short[] pcm = new short[frames];
    fillPcm(pcm, sampleRate);
    int byteCount = pcm.length * 2;
    AudioTrack t;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        t = new AudioTrack.Builder()
          .setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build())
          .setAudioFormat(new AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(encoding)
            .setChannelMask(channelMask)
            .build())
          .setBufferSizeInBytes(byteCount)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build();
      } else {
        t = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMask, encoding, byteCount, AudioTrack.MODE_STATIC);
      }
      int written = t.write(pcm, 0, pcm.length);
      if (written <= 0) {
        try {
          t.release();
        } catch (Throwable ignored) {
        }
        return null;
      }
      float vol = muted ? 0f : DEFAULT_VOLUME;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        t.setVolume(vol);
      } else {
        t.setStereoVolume(vol, vol);
      }
      t.setLoopPoints(0, frames, -1);
      return t;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private void fillPcm(short[] pcm, int sampleRate) {
    // Epic war music using D minor scale
    // Tempo: ~120 BPM for energetic feel
    double bpm = 120.0;
    double beat = 60.0 / bpm;
    // 4/4 time, 4 beats per bar, 2 steps per beat
    int stepsPerBar = 8;
    int bars = 4; // 4 bars loop
    int totalSteps = stepsPerBar * bars;
    int framesPerStep = (int) Math.max(1, Math.round(beat * 0.5 * sampleRate));
    
    // Melody: Use D minor scale (D, E, F, G, A, Bb, C)
    // MIDI notes: 62(D4), 64(E4), 65(F4), 67(G4), 69(A4), 70(Bb4), 72(C5), 74(D5)
    // Create epic bass line and heroic melody
    int[] bassNotes = {62, 65, 67, 65, 62, 65, 67, 69, 67, 65, 62, 65, 67, 69, 70, 69};
    int[] melodyNotes = {74, 72, 70, 72, 74, 77, 74, 72, 70, 69, 67, 69, 70, 72, 70, 69};
    int[] harmonyNotes = {65, 67, 69, 67, 65, 67, 69, 70, 69, 67, 65, 67, 69, 70, 72, 70};
    
    double pi2 = Math.PI * 2.0;
    double bassPhase = 0.0;
    double melodyPhase = 0.0;
    double harmonyPhase = 0.0;
    
    for (int i = 0; i < pcm.length; i++) {
      int step = (i / framesPerStep) % totalSteps;
      int stepInStep = i % framesPerStep;
      
      // Bass line (low frequency, strong)
      int bassMidi = bassNotes[step % bassNotes.length];
      double bassFreq = 440.0 * Math.pow(2.0, (bassMidi - 69) / 12.0);
      double bassInc = pi2 * bassFreq / sampleRate;
      bassPhase += bassInc;
      if (bassPhase > pi2) {
        bassPhase -= pi2;
      }
      
      // High melody (heroic theme)
      int melodyMidi = melodyNotes[step % melodyNotes.length];
      double melodyFreq = 440.0 * Math.pow(2.0, (melodyMidi - 69) / 12.0);
      double melodyInc = pi2 * melodyFreq / sampleRate;
      melodyPhase += melodyInc;
      if (melodyPhase > pi2) {
        melodyPhase -= pi2;
      }
      
      // Harmony (middle voice)
      int harmonyMidi = harmonyNotes[step % harmonyNotes.length];
      double harmonyFreq = 440.0 * Math.pow(2.0, (harmonyMidi - 69) / 12.0);
      double harmonyInc = pi2 * harmonyFreq / sampleRate;
      harmonyPhase += harmonyInc;
      if (harmonyPhase > pi2) {
        harmonyPhase -= pi2;
      }
      
      // Envelope: each note has attack and release
      double env = 1.0;
      int attack = Math.min(framesPerStep / 10, 120);
      int release = Math.min(framesPerStep / 8, 180);
      if (stepInStep < attack) {
        env = (double) stepInStep / Math.max(1, attack);
      } else if (stepInStep > framesPerStep - release) {
        env = (double) (framesPerStep - stepInStep) / Math.max(1, release);
      }
      
      // Mix: bass (0.4), melody (0.35), harmony (0.25)
      double bassWave = Math.sin(bassPhase) * 0.4;
      double melodyWave = Math.sin(melodyPhase) * 0.35;
      double harmonyWave = Math.sin(harmonyPhase) * 0.25;
      
      // Add some harmonics for richer sound
      bassWave += Math.sin(bassPhase * 2.0) * 0.1;
      melodyWave += Math.sin(melodyPhase * 2.0) * 0.08;
      
      double mixed = (bassWave + melodyWave + harmonyWave) * env;
      
      // Convert to 16-bit PCM
      int sample = (int) (mixed * 16383.0);
      sample = Math.max(-32768, Math.min(32767, sample));
      pcm[i] = (short) sample;
    }
  }
}

