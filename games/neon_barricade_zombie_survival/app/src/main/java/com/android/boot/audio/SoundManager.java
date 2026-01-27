package com.android.boot.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SoundManager {
  private final SoundPool soundPool;
  private final int shootId;
  private final int hitId;
  private final int pickupId;
  private final int shockId;
  private boolean enabled = true;

  public SoundManager(Context context) {
    AudioAttributes attributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();
    soundPool = new SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(attributes)
        .build();
    shootId = loadTone(context, 880f, 60);
    hitId = loadTone(context, 220f, 80);
    pickupId = loadTone(context, 660f, 120);
    shockId = loadTone(context, 110f, 200);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean toggle() {
    enabled = !enabled;
    return enabled;
  }

  public void playShoot() {
    play(shootId, 0.4f);
  }

  public void playHit() {
    play(hitId, 0.5f);
  }

  public void playPickup() {
    play(pickupId, 0.6f);
  }

  public void playShock() {
    play(shockId, 0.7f);
  }

  private void play(int id, float volume) {
    if (!enabled) {
      return;
    }
    soundPool.play(id, volume, volume, 1, 0, 1f);
  }

  private int loadTone(Context context, float frequency, int durationMs) {
    byte[] data = ToneGeneratorUtil.createWav(frequency, durationMs);
    File file = new File(context.getCacheDir(), "tone_" + (int) frequency + "_" + durationMs + ".wav");
    try (FileOutputStream stream = new FileOutputStream(file)) {
      stream.write(data);
    } catch (IOException ignored) {
    }
    return soundPool.load(file.getAbsolutePath(), 1);
  }

  public void release() {
    soundPool.release();
  }

  private static class ToneGeneratorUtil {
    private static byte[] createWav(float frequency, int durationMs) {
      int sampleRate = 22050;
      int totalSamples = (int) ((durationMs / 1000f) * sampleRate);
      int dataSize = totalSamples * 2;
      int totalSize = 44 + dataSize;
      byte[] buffer = new byte[totalSize];
      writeString(buffer, 0, "RIFF");
      writeInt(buffer, 4, totalSize - 8);
      writeString(buffer, 8, "WAVE");
      writeString(buffer, 12, "fmt ");
      writeInt(buffer, 16, 16);
      writeShort(buffer, 20, (short) 1);
      writeShort(buffer, 22, (short) 1);
      writeInt(buffer, 24, sampleRate);
      writeInt(buffer, 28, sampleRate * 2);
      writeShort(buffer, 32, (short) 2);
      writeShort(buffer, 34, (short) 16);
      writeString(buffer, 36, "data");
      writeInt(buffer, 40, dataSize);
      double step = 2.0 * Math.PI * frequency / sampleRate;
      int offset = 44;
      for (int i = 0; i < totalSamples; i++) {
        short sample = (short) (Math.sin(i * step) * 32767 * 0.35f);
        buffer[offset++] = (byte) (sample & 0xff);
        buffer[offset++] = (byte) ((sample >> 8) & 0xff);
      }
      return buffer;
    }

    private static void writeString(byte[] buffer, int offset, String value) {
      byte[] bytes = value.getBytes();
      System.arraycopy(bytes, 0, buffer, offset, bytes.length);
    }

    private static void writeInt(byte[] buffer, int offset, int value) {
      buffer[offset] = (byte) (value & 0xff);
      buffer[offset + 1] = (byte) ((value >> 8) & 0xff);
      buffer[offset + 2] = (byte) ((value >> 16) & 0xff);
      buffer[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private static void writeShort(byte[] buffer, int offset, short value) {
      buffer[offset] = (byte) (value & 0xff);
      buffer[offset + 1] = (byte) ((value >> 8) & 0xff);
    }
  }
}
