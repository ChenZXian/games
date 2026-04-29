package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameAudio {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> soundIds = new HashMap<>();
    private MediaPlayer bgmPlayer;
    private boolean muted;
    private String currentBgm = "";

    public GameAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(6)
                .build();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(muted ? 0f : 0.65f, muted ? 0f : 0.65f);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void playMenuLoop() {
        playBgm("bgm_menu.wav");
    }

    public void playGameplayLoop() {
        playBgm("bgm_gameplay.wav");
    }

    public void playClimaxLoop() {
        playBgm("bgm_climax.wav");
    }

    public void stopBgm() {
        currentBgm = "";
        if (bgmPlayer != null) {
            try {
                bgmPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            bgmPlayer.release();
            bgmPlayer = null;
        }
    }

    public void playUiClick() {
        playSfx("sfx_ui_click.wav");
    }

    public void playMove() {
        playSfx("sfx_move.wav");
    }

    public void playAttack() {
        playSfx("sfx_attack.wav");
    }

    public void playCapture() {
        playSfx("sfx_capture.wav");
    }

    public void playHeal() {
        playSfx("sfx_heal.wav");
    }

    public void playWarning() {
        playSfx("sfx_warning.wav");
    }

    public void playVictory() {
        playSfx("sfx_win.wav");
    }

    public void playDefeat() {
        playSfx("sfx_fail.wav");
    }

    public void release() {
        stopBgm();
        soundPool.release();
    }

    private void playBgm(String name) {
        if (muted) {
            currentBgm = name;
            return;
        }
        if (name.equals(currentBgm) && bgmPlayer != null) {
            return;
        }
        stopBgm();
        currentBgm = name;
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd("audio/" + name);
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            player.setLooping(true);
            player.prepare();
            player.setVolume(0.65f, 0.65f);
            player.start();
            bgmPlayer = player;
        } catch (IOException ignored) {
        }
    }

    private void playSfx(String name) {
        if (muted) {
            return;
        }
        Integer existing = soundIds.get(name);
        if (existing == null) {
            try {
                AssetFileDescriptor descriptor = context.getAssets().openFd("audio/" + name);
                int soundId = soundPool.load(descriptor, 1);
                descriptor.close();
                soundIds.put(name, soundId);
                existing = soundId;
            } catch (IOException ignored) {
                return;
            }
        }
        soundPool.play(existing, 1f, 1f, 1, 0, 1f);
    }
}
