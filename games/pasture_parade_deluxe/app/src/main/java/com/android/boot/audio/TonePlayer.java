package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void tap() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
    }

    public void collect() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 60);
    }

    public void warning() {
        if (!muted) toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 80);
    }
}
