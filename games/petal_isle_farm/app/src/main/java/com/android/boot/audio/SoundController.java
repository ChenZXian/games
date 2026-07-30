package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;

public class SoundController {
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 30);
    private final Context context;
    private MediaPlayer bgmPlayer;
    private boolean muted;

    public SoundController(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(muted ? 0f : 0.32f, muted ? 0f : 0.32f);
        }
    }

    public void click() {
        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_ACK, 40);
    }

    public void resumeBgm() {
        if (muted) return;
        if (bgmPlayer == null) {
            bgmPlayer = createBgmPlayer();
        }
        if (bgmPlayer == null) return;
        if (!bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    public void pauseBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void release() {
        pauseBgm();
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }
        tone.release();
    }

    private MediaPlayer createBgmPlayer() {
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd("audio/bgm.wav");
            MediaPlayer player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            player.setLooping(true);
            player.setVolume(0.32f, 0.32f);
            player.prepare();
            return player;
        } catch (Exception ignored) {
            return null;
        }
    }
}
