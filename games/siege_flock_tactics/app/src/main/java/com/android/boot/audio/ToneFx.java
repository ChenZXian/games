package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class ToneFx {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);

    public void playLaunch(boolean muted) {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 60);
        }
    }

    public void playImpact(boolean muted) {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 50);
        }
    }
}
