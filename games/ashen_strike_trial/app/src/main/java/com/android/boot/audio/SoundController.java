package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundController {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void attack() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
        }
    }

    public void hit() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 50);
        }
    }

    public void clear() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}
