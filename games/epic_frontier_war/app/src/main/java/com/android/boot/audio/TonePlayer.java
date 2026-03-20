package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class TonePlayer {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);
    private boolean muted;

    public void playTap() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
        }
    }

    public void playImpact() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 90);
        }
    }

    public void playSpell() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 120);
        }
    }

    public void toggleMute() {
        muted = !muted;
    }

    public boolean isMuted() {
        return muted;
    }
}
