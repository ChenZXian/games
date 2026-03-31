package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class ToneHelper {
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 35);

    public void hit() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
    }

    public void coin() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 35);
    }

    public void release() {
        tone.release();
    }
}
