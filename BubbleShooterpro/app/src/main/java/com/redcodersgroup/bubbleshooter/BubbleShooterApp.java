package com.redcodersgroup.bubbleshooter;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.redcodersgroup.bubbleshooter.ads.AdManager;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.MusicManager;
import com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager;
import com.redcodersgroup.bubbleshooter.ui.GameActivity;
import com.redcodersgroup.bubbleshooter.ui.ShopActivity;
import com.redcodersgroup.bubbleshooter.ui.splash.SplashActivity;

public class BubbleShooterApp extends Application implements LifecycleEventObserver {

    private static final String TAG = "BubbleShooterApp";
    private Activity currentActivity;
    private int startedActivityCount = 0;
    private boolean isAppInBackground = false;
    private boolean isColdStart = true;

    @Override
    public void onCreate() {
        super.onCreate();
        AnalyticsManager analytics = AnalyticsManager.getInstance(this);
        analytics.installCrashHandler(this);
        PlayGamesAuthManager.initialize(this);
        AdManager.getInstance().initialize(this);
        MusicManager musicManager = MusicManager.getInstance(this);

        // Register process lifecycle observer for App Open ads on resume from background
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private boolean isAdActivity(Activity activity) {
                return activity != null && (activity.getClass().getName().contains("AdActivity") ||
                        activity.getClass().getName().startsWith("com.google.android.gms.ads."));
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (isAdActivity(activity)) return;
                currentActivity = activity;
                if (activity instanceof GameActivity) {
                    musicManager.setGameScreenActive(true);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (isAdActivity(activity)) return;
                currentActivity = activity;
                startedActivityCount++;

                // Direct background-to-foreground check
                if (startedActivityCount == 1 && isAppInBackground) {
                    isAppInBackground = false;
                    Log.d(TAG, "App returned to foreground from background via ActivityLifecycle (activity: " + activity.getClass().getSimpleName() + ")");
                    onAppForegrounded(activity);
                }

                if (activity instanceof GameActivity) {
                    musicManager.setGameScreenActive(true);
                } else if (isMusicEligible(activity)) {
                    musicManager.onMusicActivityStarted();
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (isAdActivity(activity)) return;
                currentActivity = activity;
                analytics.logScreenView(activity.getClass().getSimpleName(), activity.getClass().getName());
                if (activity instanceof GameActivity) {
                    musicManager.setGameScreenActive(true);
                } else if (isMusicEligible(activity)) {
                    musicManager.setGameScreenActive(false);
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (isAdActivity(activity)) return;
                startedActivityCount--;
                if (startedActivityCount <= 0) {
                    startedActivityCount = 0;
                    isAppInBackground = true;
                    Log.d(TAG, "App transitioned to background (all activities stopped)");
                    // Preload App Open ad while in background so it's fresh and ready immediately upon return
                    AdManager.getInstance().preloadAppOpenAd();
                }
                if (activity instanceof GameActivity) {
                    if (activity.isFinishing()) {
                        musicManager.setGameScreenActive(false);
                    }
                } else if (isMusicEligible(activity)) {
                    musicManager.onMusicActivityStopped();
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (isAdActivity(activity)) return;
                if (currentActivity == activity) {
                    currentActivity = null;
                }
                if (activity instanceof GameActivity) {
                    musicManager.setGameScreenActive(false);
                }
            }

            private boolean isMusicEligible(Activity activity) {
                return (activity instanceof MainActivity ||
                        activity instanceof SplashActivity ||
                        activity instanceof ShopActivity) &&
                        !(activity instanceof GameActivity);
            }
        });
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_START) {
            if (!isColdStart) {
                Log.d(TAG, "ProcessLifecycleOwner ON_START received");
                if (currentActivity != null && !(currentActivity instanceof SplashActivity)) {
                    onAppForegrounded(currentActivity);
                }
            }
            isColdStart = false;
        } else if (event == Lifecycle.Event.ON_STOP) {
            isAppInBackground = true;
            Log.d(TAG, "ProcessLifecycleOwner ON_STOP received");
            AdManager.getInstance().preloadAppOpenAd();
        }
    }

    private void onAppForegrounded(@NonNull Activity activity) {
        if (activity instanceof SplashActivity) return;
        AdManager.getInstance().showAppOpenAdIfAvailable(activity, null);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        AnalyticsManager.getInstance(this).logMemoryPressure(-1);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        AnalyticsManager.getInstance(this).logMemoryPressure(level);
    }
}
