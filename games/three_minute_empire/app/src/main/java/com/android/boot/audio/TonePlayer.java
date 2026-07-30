package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
    private final ToneGenerator generator;
    private boolean muted;

    public TonePlayer() {
        generator = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void select() {
        play(ToneGenerator.TONE_PROP_BEEP, 45);
    }

    public void dispatch() {
        play(ToneGenerator.TONE_PROP_ACK, 70);
    }

    public void capture() {
        play(ToneGenerator.TONE_PROP_PROMPT, 90);
    }

    public void surge() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 120);
    }

    public void result(boolean win) {
        play(win ? ToneGenerator.TONE_CDMA_CONFIRM : ToneGenerator.TONE_CDMA_ABBR_ALERT, 180);
    }

    private void play(int tone, int duration) {
        if (!muted) {
            generator.startTone(tone, duration);
        }
    }

    public void release() {
        generator.release();
    }
}
