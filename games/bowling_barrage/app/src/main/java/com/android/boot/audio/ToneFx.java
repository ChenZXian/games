package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public final class ToneFx {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void playArm() {
        play(ToneGenerator.TONE_PROP_BEEP, 60);
    }

    public void playLaunch() {
        play(ToneGenerator.TONE_PROP_BEEP2, 70);
    }

    public void playHit() {
        play(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 55);
    }

    public void playBlast() {
        play(ToneGenerator.TONE_SUP_RADIO_ACK, 95);
    }

    public void playDanger() {
        play(ToneGenerator.TONE_SUP_ERROR, 110);
    }

    public void playSuccess() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150);
    }

    private void play(int tone, int durationMs) {
        if (!muted) {
            toneGenerator.startTone(tone, durationMs);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}
