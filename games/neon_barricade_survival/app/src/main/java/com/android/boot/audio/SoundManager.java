package com.android.boot.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SoundManager {
    private final SoundPool soundPool;
    private int shootId;
    private int hitId;
    private int pickupId;
    private int skillId;
    private boolean muted;

    public SoundManager(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(6).build();
        shootId = loadTone(context, 880, 0.06f);
        hitId = loadTone(context, 220, 0.08f);
        pickupId = loadTone(context, 640, 0.1f);
        skillId = loadTone(context, 120, 0.2f);
        muted = false;
    }

    public void setMuted(boolean value) {
        muted = value;
    }

    public void playShoot() {
        play(shootId, 0.6f);
    }

    public void playHit() {
        play(hitId, 0.7f);
    }

    public void playPickup() {
        play(pickupId, 0.7f);
    }

    public void playSkill() {
        play(skillId, 0.8f);
    }

    private void play(int soundId, float volume) {
        if (!muted && soundId != 0) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
        }
    }

    private int loadTone(Context context, int frequency, float duration) {
        int sampleRate = 44100;
        int totalSamples = (int) (duration * sampleRate);
        byte[] pcm = new byte[totalSamples * 2];
        for (int i = 0; i < totalSamples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            short value = (short) (Math.sin(angle) * 32767);
            pcm[i * 2] = (byte) (value & 0xff);
            pcm[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }
        byte[] wav = buildWav(pcm, sampleRate);
        File file = new File(context.getCacheDir(), "tone_" + frequency + "_" + duration + ".wav");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(wav);
            output.flush();
        } catch (IOException ignored) {
            return 0;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
        return soundPool.load(file.getAbsolutePath(), 1);
    }

    private byte[] buildWav(byte[] pcm, int sampleRate) {
        int totalDataLen = pcm.length + 36;
        int byteRate = sampleRate * 2;
        byte[] header = new byte[44 + pcm.length];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        writeInt(header, 4, totalDataLen + 8);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        writeInt(header, 16, 16);
        writeShort(header, 20, (short) 1);
        writeShort(header, 22, (short) 1);
        writeInt(header, 24, sampleRate);
        writeInt(header, 28, byteRate);
        writeShort(header, 32, (short) 2);
        writeShort(header, 34, (short) 16);
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        writeInt(header, 40, pcm.length);
        System.arraycopy(pcm, 0, header, 44, pcm.length);
        return header;
    }

    private void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
        data[offset + 2] = (byte) ((value >> 16) & 0xff);
        data[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private void writeShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
    }
}
