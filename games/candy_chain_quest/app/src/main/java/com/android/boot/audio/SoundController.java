package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundController {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
    private boolean muted;

    public void toggleMute() {
        muted = !muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void playSwap() {
        play(ToneGenerator.TONE_PROP_BEEP, 40);
    }

    public void playInvalid() {
        play(ToneGenerator.TONE_PROP_NACK, 80);
    }

    public void playClear() {
        play(ToneGenerator.TONE_PROP_ACK, 70);
    }

    public void playCascade() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 80);
    }

    public void playResult(boolean win) {
        play(win ? ToneGenerator.TONE_CDMA_HIGH_PBX_L : ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 150);
    }

    private void play(int tone, int duration) {
        if (!muted) {
            toneGenerator.startTone(tone, duration);
        }
    }
}
