package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public final class ToneFx {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void playDeal() {
        play(ToneGenerator.TONE_PROP_ACK, 70);
    }

    public void playSelect() {
        play(ToneGenerator.TONE_PROP_BEEP2, 45);
    }

    public void playCard() {
        play(ToneGenerator.TONE_PROP_BEEP, 80);
    }

    public void playPass() {
        play(ToneGenerator.TONE_PROP_NACK, 55);
    }

    public void playScore() {
        play(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING, 130);
    }

    public void playResult() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180);
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
