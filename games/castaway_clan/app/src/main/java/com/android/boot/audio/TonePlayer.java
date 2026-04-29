package com.android.boot.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class TonePlayer {
    private final SoundPool pool;
    private boolean muted;

    public TonePlayer(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setAudioAttributes(attrs)
                .setMaxStreams(4)
                .build();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void click() {
        if (muted) {
            return;
        }
    }

    public void collect() {
        if (muted) {
            return;
        }
    }

    public void warning() {
        if (muted) {
            return;
        }
    }

    public void release() {
        pool.release();
    }
}
