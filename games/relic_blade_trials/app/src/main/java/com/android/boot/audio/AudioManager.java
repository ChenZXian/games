package com.android.boot.audio;

import android.media.ToneGenerator;

public class AudioManager {
    private final ToneGenerator tone = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 60);

    public void hit() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 30);
    }
}
