package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class ToneFx {
    private final ToneGenerator toneGenerator;
    private boolean muted;

    public ToneFx() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
    }

    public void setMuted(boolean value) {
        muted = value;
    }

    public boolean isMuted() {
        return muted;
    }

    public void playSpray() {
        play(ToneGenerator.TONE_PROP_BEEP2, 50);
    }

    public void playKick() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 60);
    }

    public void playClear() {
        play(ToneGenerator.TONE_CDMA_ABBR_ALERT, 140);
    }

    private void play(int tone, int duration) {
        if (!muted) {
            toneGenerator.startTone(tone, duration);
        }
    }
}
