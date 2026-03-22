package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void playTap() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
        }
    }

    public void playHeavy() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 90);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}
