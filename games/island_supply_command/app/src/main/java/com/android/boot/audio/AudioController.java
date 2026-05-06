package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;

import java.io.IOException;

public class AudioController {
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);
    private MediaPlayer bgm;
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            stopBgm();
        }
    }

    public void click() {
        if (!muted) {
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 45);
        }
    }

    public void confirm() {
        if (!muted) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 80);
        }
    }

    public void startBgm(Context context) {
        if (muted) {
            return;
        }
        if (bgm != null) {
            if (!bgm.isPlaying()) {
                bgm.start();
            }
            return;
        }
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/bgm.wav");
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.prepare();
            player.start();
            bgm = player;
        } catch (IOException ignored) {
            stopBgm();
        }
    }

    public void stopBgm() {
        if (bgm != null) {
            bgm.stop();
            bgm.release();
            bgm = null;
        }
    }
}
