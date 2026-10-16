package com.redcodersgroup.bubbleshooter.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;

public class AnalyticsManager {

    private static final String TAG = "AnalyticsManager";
    private static volatile AnalyticsManager instance;

    private final FirebaseAnalytics firebaseAnalytics;
    private final FirebaseCrashlytics crashlytics;
    private final Context appContext;
    private boolean isCrashHandlerInstalled = false;

    private AnalyticsManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
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

    @Nullable
    public static AnalyticsManager getInstance() {
        return instance;
    }

    // =========================================================================
    // ROBUST CRASHLYTICS & ERROR TELEMETRY
    // =========================================================================

    /**
     * Installs a global uncaught exception handler wrapper that dumps active
     * game context and player state into Crashlytics right before termination.
     */
    public synchronized void installCrashHandler(@NonNull Context context) {
        if (isCrashHandlerInstalled) return;
        isCrashHandlerInstalled = true;

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                logCrashMessage("FATAL UNCAUGHT EXCEPTION on thread [" + (thread != null ? thread.getName() : "unknown") + "]");
                if (throwable != null) {
                    setCrashCustomKey("fatal_exception_class", throwable.getClass().getName());
                    setCrashCustomKey("fatal_exception_msg", throwable.getMessage() != null ? throwable.getMessage() : "null");
                }

                // Dump current player economy & level status into custom keys
                try {
                    PreferencesManager prefs = new PreferencesManager(context);
                    setCrashCustomKey("crash_highest_unlocked", prefs.getHighestUnlockedLevel());
                    setCrashCustomKey("crash_current_lives", prefs.getLives());
                    setCrashCustomKey("crash_current_diamonds", prefs.getDiamonds());
                    setCrashCustomKey("crash_bomb_boosters", prefs.getBombBoosters());
                    setCrashCustomKey("crash_fireball_boosters", prefs.getFireballBoosters());
                    setCrashCustomKey("crash_rainbow_boosters", prefs.getRainbowBoosters());
                    setCrashCustomKey("crash_lightning_boosters", prefs.getLightningBoosters());
                } catch (Throwable ignored) {}

                // Capture memory diagnostic metrics
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / (1024 * 1024);
                long totalMemory = runtime.totalMemory() / (1024 * 1024);
                long freeMemory = runtime.freeMemory() / (1024 * 1024);
                long usedMemory = totalMemory - freeMemory;
                setCrashCustomKey("mem_used_mb", (int) usedMemory);
                setCrashCustomKey("mem_max_mb", (int) maxMemory);

                if (crashlytics != null && throwable != null) {
                    crashlytics.recordException(throwable);
                }
            } catch (Throwable ignored) {
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
        Log.i(TAG, "Robust Crashlytics uncaught exception handler registered");
    }

    public void setUserId(@NonNull String userId) {
        if (crashlytics != null) {
            try {
                crashlytics.setUserId(userId);
            } catch (Exception ignored) {}
        }
        if (firebaseAnalytics != null) {
            try {
                firebaseAnalytics.setUserId(userId);
            } catch (Exception ignored) {}
        }
    }

    public void logCrashMessage(@NonNull String message) {
        if (crashlytics != null) {
            try {
                crashlytics.log(message);
            } catch (Exception ignored) {}
        }
    }

    public void setCrashCustomKey(@NonNull String key, @NonNull String value) {
        if (crashlytics != null) {
            try {
                crashlytics.setCustomKey(key, value);
            } catch (Exception ignored) {}
        }
    }

    public void setCrashCustomKey(@NonNull String key, int value) {
        if (crashlytics != null) {
            try {
                crashlytics.setCustomKey(key, value);
            } catch (Exception ignored) {}
        }
    }

    public void setCrashCustomKey(@NonNull String key, boolean value) {
        if (crashlytics != null) {
            try {
                crashlytics.setCustomKey(key, value);
            } catch (Exception ignored) {}
        }
    }

    public void setCrashCustomKey(@NonNull String key, double value) {
        if (crashlytics != null) {
            try {
                crashlytics.setCustomKey(key, value);
            } catch (Exception ignored) {}
        }
    }

    public void recordException(@NonNull Throwable throwable) {
        if (crashlytics != null) {
            try {
                crashlytics.recordException(throwable);
            } catch (Exception ignored) {}
        }
    }

    public void recordNonFatal(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        logCrashMessage("[" + tag + "] Non-Fatal: " + message);
        if (throwable != null && crashlytics != null) {
            try {
                crashlytics.recordException(throwable);
            } catch (Exception ignored) {}
        }
    }

    public void updateUserState(int highestLevel, int diamonds, int lives, @Nullable String playerName) {
        setCrashCustomKey("highest_level", highestLevel);
        setCrashCustomKey("current_diamonds", diamonds);
        setCrashCustomKey("current_lives", lives);
        if (playerName != null) {
            setCrashCustomKey("player_name", playerName);
        }
    }

    public void updateGameMatchContext(int level, @NonNull String mode, int score, int shotsRemaining, int bubblesRemaining) {
        setCrashCustomKey("current_level", level);
        setCrashCustomKey("game_mode", mode);
        setCrashCustomKey("current_score", score);
        setCrashCustomKey("shots_remaining", shotsRemaining);
        setCrashCustomKey("bubbles_remaining", bubblesRemaining);
    }

    public void logMemoryPressure(int trimLevel) {
        String msg = (trimLevel >= 0) ? ("System trimMemory level: " + trimLevel) : "System onLowMemory event triggered";
        logCrashMessage("[MemoryPressure] " + msg);
        setCrashCustomKey("last_memory_trim_level", trimLevel);
        Bundle bundle = new Bundle();
        bundle.putInt("trim_level", trimLevel);
        logEvent("memory_pressure_warning", bundle);
    }

    // =========================================================================
    // GENERAL PRODUCT & GAMEPLAY ANALYTICS
    // =========================================================================

    public void logScreenView(@NonNull String screenName, @NonNull String screenClass) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        setCrashCustomKey("current_screen", screenName);
        logCrashMessage("[ScreenView] " + screenName);
    }

    public void logLevelStart(int level, @Nullable String worldName) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle);
        setCrashCustomKey("current_level", level);
        setCrashCustomKey("game_mode", "standard");
        logCrashMessage("[Level] Start Level " + level + " (" + (worldName != null ? worldName : "World") + ")");
    }

    public void logLevelComplete(int level, @Nullable String worldName, int score, int stars) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        bundle.putInt("score", score);
        bundle.putInt("stars", stars);
        logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle);
        logEvent("level_victory", bundle);
        logCrashMessage("[Level] Victory Level " + level + " | Score=" + score + " | Stars=" + stars);
    }

    public void logLevelFail(int level, @Nullable String worldName, int score, @Nullable String reason) {
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
        logCrashMessage("[Level] Fail Level " + level + " | Reason=" + (reason != null ? reason : "out_of_shots"));
    }

    public void logLevelQuit(int level, @Nullable String worldName, int score, @NonNull String reason) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        bundle.putInt("score", score);
        bundle.putString("reason", reason);
        logEvent("level_quit", bundle);
        logCrashMessage("[Level] Quit Level " + level + " | Reason=" + reason);
    }

    public void logLevelRetry(int level, @Nullable String worldName) {
        Bundle bundle = new Bundle();
        bundle.putInt("level_number", level);
        if (worldName != null) {
            bundle.putString("world_name", worldName);
        }
        logEvent("level_retry", bundle);
    }

    public void logEndlessStart() {
        Bundle bundle = new Bundle();
        bundle.putString("mode", "endless");
        logEvent("endless_start", bundle);
        setCrashCustomKey("game_mode", "endless");
        logCrashMessage("[Endless] Endless Mode Started");
    }

    public void logEndlessGameOver(int score, int highScore) {
        Bundle bundle = new Bundle();
        bundle.putInt("score", score);
        bundle.putInt("high_score", highScore);
        bundle.putBoolean("is_new_best", score > highScore && highScore > 0);
        logEvent("endless_game_over", bundle);
        logCrashMessage("[Endless] Game Over | Score=" + score + " | HighScore=" + highScore);
    }

    public void logDiamondsEarned(int amount, @NonNull String source, int newBalance) {
        Bundle bundle = new Bundle();
        bundle.putInt("amount", amount);
        bundle.putString("source", source);
        bundle.putInt("new_balance", newBalance);
        logEvent("diamonds_earned", bundle);
        setCrashCustomKey("current_diamonds", newBalance);
    }

    public void logDiamondsSpent(int amount, @NonNull String itemType, @NonNull String itemId, int newBalance) {
        Bundle bundle = new Bundle();
        bundle.putInt("amount", amount);
        bundle.putString("item_type", itemType);
        bundle.putString("item_id", itemId);
        bundle.putInt("new_balance", newBalance);
        logEvent("diamonds_spent", bundle);
        setCrashCustomKey("current_diamonds", newBalance);
    }

    public void logLifeConsumed(@NonNull String source, int remainingLives) {
        Bundle bundle = new Bundle();
        bundle.putString("source", source);
        bundle.putInt("remaining_lives", remainingLives);
        logEvent("life_consumed", bundle);
        setCrashCustomKey("current_lives", remainingLives);
    }

    public void logHeartRefilled(@NonNull String method, int amount) {
        Bundle bundle = new Bundle();
        bundle.putString("method", method);
        bundle.putInt("amount", amount);
        logEvent("heart_refilled", bundle);
    }

    public void logHeartRefilled(@NonNull String method, int amount, int newLives) {
        Bundle bundle = new Bundle();
        bundle.putString("method", method);
        bundle.putInt("amount", amount);
        bundle.putInt("new_lives", newLives);
        logEvent("heart_refilled", bundle);
        setCrashCustomKey("current_lives", newLives);
    }

    public void logBoosterUsed(@NonNull String boosterType, int level, int bubblesImpacted) {
        Bundle bundle = new Bundle();
        bundle.putString("booster_type", boosterType);
        bundle.putInt("level_number", level);
        bundle.putInt("bubbles_impacted", bubblesImpacted);
        logEvent("booster_used", bundle);
        logCrashMessage("[Booster] Detonated " + boosterType + " on level " + level + " | Cleared=" + bubblesImpacted);
    }

    public void logBoosterEquipped(@NonNull String boosterType, int level) {
        Bundle bundle = new Bundle();
        bundle.putString("booster_type", boosterType);
        bundle.putInt("level_number", level);
        logEvent("booster_equipped", bundle);
    }

    public void logBoosterPurchased(@NonNull String boosterType, int count, int costDiamonds) {
        Bundle bundle = new Bundle();
        bundle.putString("booster_type", boosterType);
        bundle.putInt("count", count);
        bundle.putInt("cost_diamonds", costDiamonds);
        logEvent("booster_purchased", bundle);
    }

    public void logStoreOpened(@NonNull String storeType) {
        Bundle bundle = new Bundle();
        bundle.putString("store_type", storeType);
        logEvent("store_opened", bundle);
    }

    public void logIapPurchaseInitiated(@NonNull String sku) {
        Bundle bundle = new Bundle();
        bundle.putString("sku", sku);
        logEvent("iap_purchase_initiated", bundle);
        logCrashMessage("[IAP] Initiated checkout for SKU: " + sku);
    }

    public void logIapPurchaseSuccess(@NonNull String sku, int diamondsAdded, @NonNull String formattedPrice) {
        Bundle bundle = new Bundle();
        bundle.putString("sku", sku);
        bundle.putInt("diamonds_added", diamondsAdded);
        bundle.putString("formatted_price", formattedPrice);
        logEvent("iap_purchase_success", bundle);
        logCrashMessage("[IAP] Purchase success: " + sku + " (+" + diamondsAdded + " diamonds)");
    }

    public void logIapPurchaseFailed(@NonNull String sku, @NonNull String errorMessage) {
        Bundle bundle = new Bundle();
        bundle.putString("sku", sku);
        bundle.putString("error_message", errorMessage);
        logEvent("iap_purchase_failed", bundle);
        logCrashMessage("[IAP] Purchase failed for " + sku + ": " + errorMessage);
    }

    public void logAdImpression(@NonNull String adType, @NonNull String placement) {
        Bundle bundle = new Bundle();
        bundle.putString("ad_type", adType);
        bundle.putString("placement", placement);
        logEvent("ad_impression", bundle);
    }

    public void logRewardedAdWatched(@NonNull String placement, @NonNull String rewardType, int rewardAmount) {
        Bundle bundle = new Bundle();
        bundle.putString("placement", placement);
        bundle.putString("reward_type", rewardType);
        bundle.putInt("reward_amount", rewardAmount);
        logEvent("ad_rewarded_watched", bundle);
        logCrashMessage("[Ad] Rewarded video finished for " + placement + " -> +" + rewardAmount + " " + rewardType);
    }

    public void logAdFailedToShow(@NonNull String adType, @NonNull String placement, @NonNull String errorCode) {
        Bundle bundle = new Bundle();
        bundle.putString("ad_type", adType);
        bundle.putString("placement", placement);
        bundle.putString("error_code", errorCode);
        logEvent("ad_failed_to_show", bundle);
        logCrashMessage("[Ad] Failed to show " + adType + " (" + placement + ") -> " + errorCode);
    }

    public void logCloudSaveSync(boolean success, @NonNull String action, @Nullable String detail) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("success", success);
        bundle.putString("action", action);
        if (detail != null) {
            bundle.putString("detail", detail);
        }
        logEvent("cloud_save_sync", bundle);
        logCrashMessage("[CloudSave] Action=" + action + " Success=" + success + " Detail=" + (detail != null ? detail : "ok"));
    }

    public void logPlayGamesSignIn(boolean success, @Nullable String playerId, @Nullable String errorMessage) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("success", success);
        if (playerId != null) {
            bundle.putString("player_id", playerId);
        }
        if (errorMessage != null) {
            bundle.putString("error_message", errorMessage);
        }
        logEvent("play_games_auth", bundle);
        if (success && playerId != null) {
            setUserId(playerId);
        }
    }

    public void logGiftChestClaimed(int worldId, int offset, int diamonds) {
        Bundle bundle = new Bundle();
        bundle.putInt("world_id", worldId);
        bundle.putInt("level_offset", offset);
        bundle.putInt("diamonds_awarded", diamonds);
        logEvent("gift_chest_claimed", bundle);
    }

    public void logStarChestOpened(int chestLevel, int starsRequired, int diamondsAwarded) {
        Bundle bundle = new Bundle();
        bundle.putInt("chest_level", chestLevel);
        bundle.putInt("stars_required", starsRequired);
        bundle.putInt("diamonds_awarded", diamondsAwarded);
        logEvent("star_chest_opened", bundle);
    }

    public void logEvent(@NonNull String eventName, @Nullable Bundle params) {
        if (firebaseAnalytics != null) {
            try {
                firebaseAnalytics.logEvent(eventName, params);
            } catch (Exception e) {
                Log.w(TAG, "Error logging event: " + eventName, e);
            }
        }
    }
}
