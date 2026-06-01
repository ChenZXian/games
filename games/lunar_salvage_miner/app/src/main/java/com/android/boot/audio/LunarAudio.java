package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LunarAudio {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> sounds = new HashMap<>();
    private MediaPlayer player;
    private String currentBgm = "";
    private boolean enabled = true;

    public LunarAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(6)
                .build();
        loadSound("ui", "audio/sfx_ui_click.wav");
        loadSound("scan", "audio/sfx_ui_click.wav");
        loadSound("launch", "audio/sfx_launch.wav");
        loadSound("collect", "audio/sfx_collect.wav");
        loadSound("repair", "audio/sfx_repair.wav");
        loadSound("warning", "audio/sfx_warning.wav");
        loadSound("boost", "audio/sfx_boost.wav");
        loadSound("win", "audio/sfx_win.wav");
        loadSound("fail", "audio/sfx_fail.wav");
    }

    public void playMenu() {
        playBgm("audio/bgm_menu.wav", 0.42f);
    }

    public void playGame() {
        playBgm("audio/bgm.wav", 0.46f);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            pause();
        } else if (player != null) {
            player.start();
        } else if (currentBgm.length() > 0) {
            playBgm(currentBgm, currentBgm.contains("menu") ? 0.42f : 0.46f);
        }
    }

    public void playCue(String cue) {
        if (!enabled) {
            return;
        }
        Integer soundId = sounds.get(cue);
        if (soundId != null && soundId > 0) {
            soundPool.play(soundId, 0.85f, 0.85f, 1, 0, 1f);
        }
    }

    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        soundPool.release();
    }

    private void loadSound(String cue, String path) {
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(path);
            int id = soundPool.load(descriptor, 1);
            descriptor.close();
            sounds.put(cue, id);
        } catch (IOException ignored) {
            sounds.put(cue, 0);
        }
    }

    private void playBgm(String path, float volume) {
        if (!enabled) {
            if (!path.equals(currentBgm) && player != null) {
                player.release();
                player = null;
            }
            currentBgm = path;
            return;
        }
        if (path.equals(currentBgm) && player != null) {
            if (!player.isPlaying()) {
                player.start();
            }
            return;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        currentBgm = path;
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(path);
            MediaPlayer next = new MediaPlayer();
            next.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            next.setLooping(true);
            next.setVolume(volume, volume);
            next.prepare();
            next.start();
            player = next;
        } catch (IOException ignored) {
            player = null;
        }
    }
}
