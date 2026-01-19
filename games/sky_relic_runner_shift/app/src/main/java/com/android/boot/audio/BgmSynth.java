package com.android.boot.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public final class BgmSynth {
    private AudioTrack track;
    private boolean started;

    public void start() {
        if (started) {
            return;
        }
        started = true;
        track = buildTrack();
        if (track == null) {
            started = false;
            return;
        }
        track.play();
    }

    public void stop() {
        started = false;
        AudioTrack t = track;
        track = null;
        if (t == null) {
            return;
        }
        try {
            t.pause();
            t.flush();
        } catch (Throwable ignored) {
        }
        try {
            t.release();
        } catch (Throwable ignored) {
        }
    }

    private AudioTrack buildTrack() {
        int sampleRate = 22050;
        int channel = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int seconds = 2;
        int frames = sampleRate * seconds;
        short[] pcm = new short[frames];
        fillPcm(pcm, sampleRate);
        int byteCount = pcm.length * 2;

        AudioTrack t;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            t = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(encoding)
                            .setChannelMask(channel)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(byteCount)
                    .build();
        } else {
            t = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channel, encoding, byteCount, AudioTrack.MODE_STATIC);
        }

        int written = t.write(pcm, 0, pcm.length);
        if (written <= 0) {
            try {
                t.release();
            } catch (Throwable ignored) {
            }
            return null;
        }
        float vol = 0.22f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            t.setVolume(vol);
        } else {
            t.setStereoVolume(vol, vol);
        }
        t.setLoopPoints(0, frames, -1);
        return t;
    }

    private void fillPcm(short[] pcm, int sampleRate) {
        double bpm = 108.0;
        double beat = 60.0 / bpm;
        int steps = 16;
        double step = beat * 0.5;
        int framesPerStep = Math.max(1, (int) Math.round(step * sampleRate));
        int[] notes = new int[]{62, 65, 69, 74, 69, 65, 64, 65, 71, 74, 71, 69, 65, 64, 62, 64};
        double a = 0.0;
        double pi2 = Math.PI * 2.0;
        for (int i = 0; i < pcm.length; i++) {
            int s = (i / framesPerStep) % steps;
            int midi = notes[s];
            double freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
            double inc = pi2 * freq / sampleRate;
            a += inc;
            if (a > pi2) {
                a -= pi2;
            }
            int pos = i % framesPerStep;
            int attack = Math.min(framesPerStep / 10, 90);
            int release = Math.min(framesPerStep / 6, 140);
            double env = 1.0;
            if (pos < attack) {
                env = (double) pos / Math.max(1, attack);
            } else if (pos > framesPerStep - release) {
                env = (double) (framesPerStep - pos) / Math.max(1, release);
            }

            double tone = Math.sin(a);
            double bass = Math.sin(a * 0.5) * 0.4;
            double mix = (tone * 0.6 + bass) * env;
            int v = (int) Math.round(mix * 32767.0);
            if (v > 32767) {
                v = 32767;
            } else if (v < -32768) {
                v = -32768;
            }
            pcm[i] = (short) v;
        }
    }
}

