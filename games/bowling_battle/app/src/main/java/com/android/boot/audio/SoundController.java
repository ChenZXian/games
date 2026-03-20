package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundController {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
    private boolean muted;

    public void setMuted(boolean value) {
        muted = value;
    }

    public boolean isMuted() {
        return muted;
    }

    public void toggleMuted() {
        muted = !muted;
    }

    public void playDrop() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
        }
    }

    public void playHit() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 45);
        }
    }

    public void playClear() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}
