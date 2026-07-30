package com.android.boot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RanchVetAudio {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> sounds = new HashMap<>();
    private MediaPlayer bgm;
    private String currentBgm = "";
    private boolean enabled = true;

    public RanchVetAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build();
        loadSound("ui_click", "audio/sfx_ui_click.wav");
        loadSound("animal_select", "audio/sfx_animal_select.wav");
        loadSound("assign", "audio/sfx_assign.wav");
        loadSound("good_treatment", "audio/sfx_good_treatment.wav");
        loadSound("bad_treatment", "audio/sfx_bad_treatment.wav");
        loadSound("release", "audio/sfx_release.wav");
        loadSound("stress_warning", "audio/sfx_stress_warning.wav");
        loadSound("day_clear", "audio/sfx_day_clear.wav");
    }

    public void playMenu() {
        playBgm("audio/bgm_menu.wav", true);
    }

    public void playGame() {
        playBgm("audio/bgm.wav", true);
    }

    public void playWin() {
        playBgm("audio/bgm_win.wav", false);
    }

    public void playFail() {
        playBgm("audio/bgm_fail.wav", false);
    }

    public void playSound(String role) {
        if (!enabled) {
            return;
        }
        Integer id = sounds.get(role);
        if (id != null && id > 0) {
            soundPool.play(id, 0.85f, 0.85f, 1, 0, 1f);
        }
    }

    public void pause() {
        if (bgm != null && bgm.isPlaying()) {
            bgm.pause();
        }
    }

    public void resume() {
        if (enabled && bgm != null && !bgm.isPlaying()) {
            bgm.start();
        }
    }

    public void release() {
        if (bgm != null) {
            bgm.release();
            bgm = null;
        }
        soundPool.release();
    }

    private void loadSound(String role, String path) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(path);
            int id = soundPool.load(afd, 1);
            afd.close();
            sounds.put(role, id);
        } catch (Exception ex) {
            sounds.put(role, 0);
        }
    }

    private void playBgm(String path, boolean loop) {
        if (!enabled || path.equals(currentBgm)) {
            if (bgm != null && !bgm.isPlaying()) {
                bgm.start();
            }
            return;
        }
        currentBgm = path;
        if (bgm != null) {
            bgm.release();
            bgm = null;
        }
        AssetFileDescriptor afd = null;
        MediaPlayer player = null;
        try {
            afd = context.getAssets().openFd(path);
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.setLooping(loop);
            player.setVolume(0.46f, 0.46f);
            player.prepare();
            player.start();
            bgm = player;
        } catch (Exception ex) {
            currentBgm = "";
            if (player != null) {
                player.release();
            }
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}
