package com.android.boot;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class BgmPlayer {
  private AudioTrack track;
  private boolean started = false;
  private boolean muted = false;
  private static final float DEFAULT_VOLUME = 0.3f;

  public void start(android.content.Context context) {
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
    // 4 second loop
    int seconds = 4;
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
    // Use minor scale for tense atmosphere
    // Tempo: ~100 BPM
    double bpm = 100.0;
    double beat = 60.0 / bpm;
    // 4/4 time, 4 beats per bar, 2 steps per beat
    int stepsPerBar = 8;
    int bars = 2; // 2 bars loop
    int totalSteps = stepsPerBar * bars;
    int framesPerStep = (int) Math.max(1, Math.round(beat * 0.5 * sampleRate));
    
    // Melody: Use A minor scale (A, B, C, D, E, F, G)
    // MIDI notes: 57(A3), 59(B3), 60(C4), 62(D4), 64(E4), 65(F4), 67(G4), 69(A4)
    // Create tense bass line and high melody
    int[] bassNotes = {57, 60, 62, 60, 57, 60, 62, 65, 64, 62, 60, 62, 57, 60, 62, 60};
    int[] melodyNotes = {69, 67, 65, 67, 69, 72, 69, 67, 65, 64, 62, 64, 65, 67, 65, 64};
    
    double pi2 = Math.PI * 2.0;
    double bassPhase = 0.0;
    double melodyPhase = 0.0;
    
    for (int i = 0; i < pcm.length; i++) {
      int step = (i / framesPerStep) % totalSteps;
      int stepInStep = i % framesPerStep;
      
      // Bass line
      int bassMidi = bassNotes[step % bassNotes.length];
      double bassFreq = 440.0 * Math.pow(2.0, (bassMidi - 69) / 12.0);
      double bassInc = pi2 * bassFreq / sampleRate;
      bassPhase += bassInc;
      if (bassPhase > pi2) {
        bassPhase -= pi2;
      }
      
      // High melody
      int melodyMidi = melodyNotes[step % melodyNotes.length];
      double melodyFreq = 440.0 * Math.pow(2.0, (melodyMidi - 69) / 12.0);
      double melodyInc = pi2 * melodyFreq / sampleRate;
      melodyPhase += melodyInc;
      if (melodyPhase > pi2) {
        melodyPhase -= pi2;
      }
      
      // Envelope: each note has slight attack and release
      double env = 1.0;
      int attack = Math.min(framesPerStep / 8, 100);
      int release = Math.min(framesPerStep / 6, 150);
      if (stepInStep < attack) {
        env = (double) stepInStep / Math.max(1, attack);
      } else if (stepInStep > framesPerStep - release) {
        env = (double) (framesPerStep - stepInStep) / Math.max(1, release);
      }
      
      // Synthesis: bass uses sine wave, high uses sine wave, add harmonics
      double bassWave = Math.sin(bassPhase);
      // Add bass harmonics
      bassWave += Math.sin(bassPhase * 2.0) * 0.3;
      bassWave += Math.sin(bassPhase * 3.0) * 0.15;
      
      double melodyWave = Math.sin(melodyPhase);
      // Add high harmonics
      melodyWave += Math.sin(melodyPhase * 2.0) * 0.2;
      
      // Mix: bass 60%, high 40%
      double mix = (bassWave * 0.6 + melodyWave * 0.4) * env;
      
      // Add slight vibrato effect
      double vibrato = Math.sin(i * pi2 * 5.0 / sampleRate) * 0.05;
      mix *= (1.0 + vibrato);
      
      // Limit volume and convert to short
      int v = (int) Math.round(mix * 32767.0 * 0.4); // Reduce total volume
      if (v > 32767) {
        v = 32767;
      } else if (v < -32768) {
        v = -32768;
      }
      pcm[i] = (short) v;
    }
  }
}

