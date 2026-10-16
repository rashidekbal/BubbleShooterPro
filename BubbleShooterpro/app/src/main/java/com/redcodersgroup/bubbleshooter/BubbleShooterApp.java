package com.redcodersgroup.bubbleshooter;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.redcodersgroup.bubbleshooter.audio.MusicManager;
import com.redcodersgroup.bubbleshooter.ui.GameActivity;
import com.redcodersgroup.bubbleshooter.ui.ShopActivity;
import com.redcodersgroup.bubbleshooter.ui.splash.SplashActivity;

public class BubbleShooterApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager analytics =
                com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager.getInstance(this);
        analytics.installCrashHandler(this);
        com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.initialize(this);
        com.redcodersgroup.bubbleshooter.ads.AdManager.getInstance().initialize(this);
        MusicManager musicManager = MusicManager.getInstance(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof GameActivity) {
                    musicManager.setGameScreenActive(true);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activity instanceof GameActivity) {
                    musicManager.setGameScreenActive(true);
                } else if (isMusicEligible(activity)) {
                    musicManager.onMusicActivityStarted();
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
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
    public void onLowMemory() {
        super.onLowMemory();
        com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager.getInstance(this).logMemoryPressure(-1);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager.getInstance(this).logMemoryPressure(level);
    }
}
