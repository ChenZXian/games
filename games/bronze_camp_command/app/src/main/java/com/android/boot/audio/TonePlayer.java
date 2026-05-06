package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import com.android.boot.core.BronzeState;

public class TonePlayer implements MediaPlayer.OnCompletionListener {
    private final SoundPool pool;
    private final Context context;
    private final MediaPlayer menuPlayer;
    private final MediaPlayer playPlayer;
    private final MediaPlayer climaxPlayer;
    private int clickId;
    private int collectId;
    private int warningId;
    private MediaPlayer activePlayer;
    private boolean muted;

    public TonePlayer(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setAudioAttributes(attrs)
                .setMaxStreams(6)
                .build();
        clickId = loadSound("audio/sfx_ui_click.wav");
        collectId = loadSound("audio/sfx_collect.wav");
        warningId = loadSound("audio/sfx_warning.wav");
        menuPlayer = createLoopPlayer("audio/bgm_menu.wav");
        playPlayer = createLoopPlayer("audio/bgm.wav");
        climaxPlayer = createLoopPlayer("audio/bgm_climax.wav");
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            pauseAll();
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void click() {
        if (!muted) {
            playSample(clickId, 0.82f);
        }
    }

    public void collect() {
        if (!muted) {
            playSample(collectId, 0.88f);
        }
    }

    public void warning() {
        if (!muted) {
            playSample(warningId, 0.95f);
        }
    }

    public void syncState(BronzeState state, float matchTime) {
        if (muted) {
            pauseAll();
            return;
        }
        MediaPlayer desired = null;
        if (state == BronzeState.MENU || state == BronzeState.GAME_OVER || state == BronzeState.VICTORY) {
            desired = menuPlayer;
        } else if (state == BronzeState.PLAYING) {
            desired = matchTime >= 360f ? climaxPlayer : playPlayer;
        }
        if (desired == activePlayer) {
            if (desired != null && !desired.isPlaying()) {
                desired.start();
            }
            return;
        }
        if (activePlayer != null && activePlayer.isPlaying()) {
            activePlayer.pause();
        }
        activePlayer = desired;
        if (activePlayer != null) {
            if (state == BronzeState.MENU) {
                activePlayer.seekTo(0);
            }
            activePlayer.start();
        }
    }

    public void pauseAll() {
        if (menuPlayer != null && menuPlayer.isPlaying()) {
            menuPlayer.pause();
        }
        if (playPlayer != null && playPlayer.isPlaying()) {
            playPlayer.pause();
        }
        if (climaxPlayer != null && climaxPlayer.isPlaying()) {
            climaxPlayer.pause();
        }
    }

    public void release() {
        pauseAll();
        if (menuPlayer != null) {
            menuPlayer.release();
        }
        if (playPlayer != null) {
            playPlayer.release();
        }
        if (climaxPlayer != null) {
            climaxPlayer.release();
        }
        pool.release();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            if (!muted && mediaPlayer == activePlayer) {
                mediaPlayer.start();
            }
        }
    }

    private int loadSound(String assetPath) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            int sampleId = pool.load(afd, 1);
            afd.close();
            return sampleId;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private MediaPlayer createLoopPlayer(String assetPath) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.setVolume(0.48f, 0.48f);
            player.prepare();
            player.setOnCompletionListener(this);
            return player;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void playSample(int sampleId, float volume) {
        if (sampleId != 0) {
            pool.play(sampleId, volume, volume, 1, 0, 1f);
        }
    }
}
