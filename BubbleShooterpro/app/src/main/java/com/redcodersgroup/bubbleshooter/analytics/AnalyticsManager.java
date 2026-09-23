package com.redcodersgroup.bubbleshooter.analytics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class AnalyticsManager {

    private static final String TAG = "AnalyticsManager";
    private static volatile AnalyticsManager instance;

    private final FirebaseAnalytics firebaseAnalytics;
    private final FirebaseCrashlytics crashlytics;

    private AnalyticsManager(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        FirebaseAnalytics fa = null;
        FirebaseCrashlytics fc = null;
        try {
            fa = FirebaseAnalytics.getInstance(appContext);
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize FirebaseAnalytics", e);
        }
        try {
            fc = FirebaseCrashlytics.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize FirebaseCrashlytics", e);
        }
        this.firebaseAnalytics = fa;
        this.crashlytics = fc;
    }

    public static AnalyticsManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AnalyticsManager.class) {
                if (instance == null) {
                    instance = new AnalyticsManager(context);
                }
            }
        }
        return instance;
    }

    // --- Custom Crashlytics Helpers ---

    public void logCrashMessage(@NonNull String message) {
        if (crashlytics != null) {
            crashlytics.log(message);
        }
    }

    public void setCrashCustomKey(@NonNull String key, @NonNull String value) {
        if (crashlytics != null) {
            crashlytics.setCustomKey(key, value);
        }
    }

    public void recordException(@NonNull Throwable throwable) {
        if (crashlytics != null) {
            crashlytics.recordException(throwable);
        }
    }

    // --- Normal Analytics Events ---

    public void logLevelStart(int level, String worldName) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle);
        setCrashCustomKey("current_level", String.valueOf(level));
    }

    public void logLevelComplete(int level, String worldName, int score, int stars) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        bundle.putInt("score", score);
        bundle.putInt("stars", stars);
        logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle);
        logEvent("level_victory", bundle);
    }

    public void logLevelFail(int level, String worldName, int score, String reason) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        bundle.putInt("score", score);
        if (reason != null) {
            bundle.putString("reason", reason);
        }
        logEvent("level_fail", bundle);
    }

    public void logEndlessStart() {
        Bundle bundle = new Bundle();
        bundle.putString("mode", "endless");
        logEvent("endless_start", bundle);
        setCrashCustomKey("game_mode", "endless");
    }

    public void logEndlessGameOver(int score, int highScore) {
        Bundle bundle = new Bundle();
        bundle.putInt("score", score);
        bundle.putInt("high_score", highScore);
        bundle.putBoolean("is_new_best", score > highScore && highScore > 0);
        logEvent("endless_game_over", bundle);
    }

    public void logGiftChestClaimed(int worldId, int offset, int diamonds) {
        Bundle bundle = new Bundle();
        bundle.putInt("world_id", worldId);
        bundle.putInt("level_offset", offset);
        bundle.putInt("diamonds_awarded", diamonds);
        logEvent("gift_chest_claimed", bundle);
    }

    public void logStoreOpened(String storeType) {
        Bundle bundle = new Bundle();
        bundle.putString("store_type", storeType);
        logEvent("store_opened", bundle);
    }

    public void logHeartRefilled(String method, int amount) {
        Bundle bundle = new Bundle();
        bundle.putString("method", method); // "ad", "diamond", "timer"
        bundle.putInt("amount", amount);
        logEvent("heart_refilled", bundle);
    }

    public void logEvent(@NonNull String eventName, Bundle params) {
        if (firebaseAnalytics != null) {
            try {
                firebaseAnalytics.logEvent(eventName, params);
            } catch (Exception e) {
                Log.w(TAG, "Error logging event: " + eventName, e);
            }
        }
    }
}
