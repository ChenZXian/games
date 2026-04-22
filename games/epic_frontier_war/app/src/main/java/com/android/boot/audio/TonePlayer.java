package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void playSummon() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 70);
        }
    }

    public void playSpell() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 110);
        }
    }

    public void playImpact() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 55);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}
