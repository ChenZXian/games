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

    public void playDrop() {
        play(ToneGenerator.TONE_PROP_BEEP, 70);
    }

    public void playHit() {
        play(ToneGenerator.TONE_CDMA_ABBR_ALERT, 60);
    }

    public void playBlast() {
        play(ToneGenerator.TONE_SUP_RADIO_ACK, 90);
    }

    public void playSuccess() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 160);
    }

    public void playFail() {
        play(ToneGenerator.TONE_SUP_ERROR, 140);
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
