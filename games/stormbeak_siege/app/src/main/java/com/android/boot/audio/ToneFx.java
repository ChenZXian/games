package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class ToneFx {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);

    public void playLaunch(boolean muted) {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 70);
        }
    }

    public void playSkill(boolean muted) {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 90);
        }
    }

    public void playImpact(boolean muted) {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 80);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}
