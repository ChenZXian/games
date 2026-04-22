package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void button() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
    }

    public void pickup() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 80);
    }

    public void damage() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 110);
    }

    public void clear() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 140);
    }
}
