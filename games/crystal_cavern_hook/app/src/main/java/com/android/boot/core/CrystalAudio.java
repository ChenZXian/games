package com.android.boot.core;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;

public class CrystalAudio {
    private final Context context;
    private final SoundPool soundPool;
    private int sfxUiClick;
    private int sfxLaunch;
    private int sfxCollect;
    private int sfxTool;
    private int sfxHazard;
    private int sfxPrism;
    private int sfxWin;
    private int sfxFail;
    private MediaPlayer bgmPlayer;
    private boolean muted;
    private boolean bgmPaused;

    public CrystalAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attributes).build();
        loadSfx();
        playMenuBgm();
    }

    public void playMenuBgm() {
        playBgm("audio/bgm_menu.wav", true);
    }

    public void playGameBgm() {
        playBgm("audio/bgm.wav", true);
    }

    public void playWinBgm() {
        playBgm("audio/bgm_win.wav", false);
        playSound(sfxWin);
    }

    public void playFailBgm() {
        playBgm("audio/bgm_fail.wav", false);
        playSound(sfxFail);
    }

    public void playUiClick() {
        playSound(sfxUiClick);
    }

    public void playLaunch() {
        playSound(sfxLaunch);
    }

    public void playCollect() {
        playSound(sfxCollect);
    }

    public void playTool() {
        playSound(sfxTool);
    }

    public void playHazard() {
        playSound(sfxHazard);
    }

    public void playPrism() {
        playSound(sfxPrism);
    }

    public void playFail() {
        playSound(sfxFail);
    }

    public void pauseBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
            bgmPaused = true;
        }
    }

    public void resumeBgm() {
        if (!muted && bgmPlayer != null && bgmPaused) {
            bgmPlayer.start();
            bgmPaused = false;
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            pauseBgm();
        } else {
            resumeBgm();
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void release() {
        stopBgm();
        soundPool.release();
    }

    private void loadSfx() {
        sfxUiClick = load("audio/sfx_ui_click.wav");
        sfxLaunch = load("audio/sfx_launch.wav");
        sfxCollect = load("audio/sfx_collect.wav");
        sfxTool = load("audio/sfx_tool.wav");
        sfxHazard = load("audio/sfx_hazard.wav");
        sfxPrism = load("audio/sfx_prism.wav");
        sfxWin = load("audio/sfx_win.wav");
        sfxFail = load("audio/sfx_fail.wav");
    }

    private int load(String path) {
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(path);
            int id = soundPool.load(descriptor, 1);
            descriptor.close();
            return id;
        } catch (IOException e) {
            return 0;
        }
    }

    private void playSound(int soundId) {
        if (!muted && soundId != 0) {
            soundPool.play(soundId, 0.85f, 0.85f, 1, 0, 1f);
        }
    }

    private void playBgm(String path, boolean loop) {
        stopBgm();
        if (muted) {
            return;
        }
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(path);
            bgmPlayer = new MediaPlayer();
            bgmPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            bgmPlayer.setLooping(loop);
            bgmPlayer.setVolume(0.85f, 0.85f);
            bgmPlayer.prepare();
            bgmPlayer.start();
            bgmPaused = false;
        } catch (IOException e) {
            stopBgm();
        }
    }

    private void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }
        bgmPaused = false;
    }
}
