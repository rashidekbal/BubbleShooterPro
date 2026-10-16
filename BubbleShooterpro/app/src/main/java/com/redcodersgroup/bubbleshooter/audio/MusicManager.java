package com.redcodersgroup.bubbleshooter.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;

public class MusicManager {
    private static final String TAG = "MusicManager";
    private static MusicManager instance;
    private final Context context;
    private final PreferencesManager prefs;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int activeMusicActivities = 0;
    private boolean isGameScreenActive = false;

    private MusicManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new PreferencesManager(context);
    }

    public static synchronized MusicManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicManager(context);
        }
        return instance;
    }

    public synchronized void setGameScreenActive(boolean active) {
        this.isGameScreenActive = active;
        if (active) {
            pauseMusic();
        } else if (activeMusicActivities > 0 && prefs.isMusicEnabled()) {
            startMusic();
        }
    }

    public synchronized boolean isGameScreenActive() {
        return isGameScreenActive;
    }

    public synchronized void onMusicActivityStarted() {
        activeMusicActivities++;
        if (activeMusicActivities == 1 && !isGameScreenActive && prefs.isMusicEnabled()) {
            startMusic();
        }
    }

    public synchronized void onMusicActivityStopped() {
        activeMusicActivities = Math.max(0, activeMusicActivities - 1);
        if (activeMusicActivities == 0) {
            pauseMusic();
        }
    }

    public synchronized boolean isMusicAllowed() {
        return !isGameScreenActive && activeMusicActivities > 0;
    }

    public synchronized void startMusic() {
        if (isGameScreenActive || !prefs.isMusicEnabled()) {
            return;
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.bgm);
                if (mediaPlayer == null) {
                    Log.e(TAG, "Failed to create MediaPlayer for bgm");
                    return;
                }
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0.60f, 0.60f);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                    releaseMediaPlayer();
                    return true;
                });
            }

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                isPlaying = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting background music", e);
            releaseMediaPlayer();
        }
    }

    public synchronized void pauseMusic() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            } catch (Exception ignored) {}
        }
        isPlaying = false;
    }

    public synchronized void resumeMusic() {
        if (!isGameScreenActive && activeMusicActivities > 0 && prefs.isMusicEnabled()) {
            startMusic();
        }
    }

    public synchronized void stopMusic() {
        releaseMediaPlayer();
        isPlaying = false;
    }

    public synchronized void setMusicEnabled(boolean enabled) {
        prefs.setMusicEnabled(enabled);
        if (enabled) {
            if (!isGameScreenActive && activeMusicActivities > 0) {
                startMusic();
            }
        } else {
            pauseMusic();
        }
    }

    public boolean isMusicEnabled() {
        return prefs.isMusicEnabled();
    }

    // Aliases for compatibility
    public synchronized void setLobbyActive(boolean active) {
        setGameScreenActive(!active);
    }

    public synchronized boolean isLobbyActive() {
        return !isGameScreenActive;
    }

    public synchronized void setBgmAllowed(boolean allowed) {
        setGameScreenActive(!allowed);
    }

    public synchronized boolean isBgmAllowed() {
        return !isGameScreenActive;
    }

    @Deprecated
    public synchronized void onActivityStarted() {
        onMusicActivityStarted();
    }

    @Deprecated
    public synchronized void onActivityStopped() {
        onMusicActivityStopped();
    }

    private synchronized void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {}
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPlaying = false;
    }
}
