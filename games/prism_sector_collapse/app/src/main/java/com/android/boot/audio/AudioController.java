package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AudioController {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> sounds = new HashMap<>();
    private MediaPlayer menuPlayer;
    private MediaPlayer playPlayer;
    private boolean muted;

    public AudioController(Context context) {
        this.context = context.getApplicationContext();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build())
                .build();
        loadSound("click", "audio/sfx_click.wav");
        loadSound("collect", "audio/sfx_collect.wav");
        loadSound("warning", "audio/sfx_warning.wav");
        loadSound("win", "audio/sfx_win.wav");
        loadSound("fail", "audio/sfx_fail.wav");
    }

    public void playMenu() {
        if (muted) {
            return;
        }
        stopPlayer(playPlayer);
        if (menuPlayer == null) {
            menuPlayer = createPlayer("audio/bgm_menu.wav");
        }
        startPlayer(menuPlayer);
    }

    public void playGameplay() {
        if (muted) {
            return;
        }
        stopPlayer(menuPlayer);
        if (playPlayer == null) {
            playPlayer = createPlayer("audio/bgm_play.wav");
        }
        startPlayer(playPlayer);
    }

    public void stopMusic() {
        stopPlayer(menuPlayer);
        stopPlayer(playPlayer);
    }

    public void release() {
        stopPlayer(menuPlayer);
        stopPlayer(playPlayer);
        if (menuPlayer != null) {
            menuPlayer.release();
            menuPlayer = null;
        }
        if (playPlayer != null) {
            playPlayer.release();
            playPlayer = null;
        }
        soundPool.release();
    }

    public void playEffect(String key) {
        if (muted) {
            return;
        }
        Integer soundId = sounds.get(key);
        if (soundId != null) {
            soundPool.play(soundId, 0.9f, 0.9f, 1, 0, 1f);
        }
    }

    private void loadSound(String key, String assetPath) {
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath);
            int soundId = soundPool.load(descriptor, 1);
            sounds.put(key, soundId);
            descriptor.close();
        } catch (IOException ignored) {
        }
    }

    private MediaPlayer createPlayer(String assetPath) {
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath);
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build());
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            player.setLooping(true);
            player.setVolume(0.7f, 0.7f);
            player.prepare();
            return player;
        } catch (IOException exception) {
            return null;
        }
    }

    private void startPlayer(MediaPlayer player) {
        if (player == null) {
            return;
        }
        if (!player.isPlaying()) {
            player.seekTo(0);
            player.start();
        }
    }

    private void stopPlayer(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            if (player.isPlaying()) {
                player.pause();
            }
        } catch (RuntimeException ignored) {
        }
    }
}
