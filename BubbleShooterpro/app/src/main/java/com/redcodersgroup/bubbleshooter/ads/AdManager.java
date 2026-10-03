package com.redcodersgroup.bubbleshooter.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdManager {

    private static final String TAG = "AdManager";
    private static volatile AdManager instance;

    // Smart Pacing Configuration
    private static final long INTERSTITIAL_MIN_INTERVAL_MS = 180_000; // 3 minutes between interstitials
    private static final long REWARDED_GRACE_PERIOD_MS = 300_000;     // 5 minutes no interstitials after rewarded ad
    private static final int INTERSTITIAL_LEVEL_INTERVAL = 3;         // Every 3 completed levels

    private Context appContext;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    private boolean isInterstitialLoading = false;
    private boolean isRewardedLoading = false;

    private long lastInterstitialTime = 0;
    private long lastRewardedWatchedTime = 0;
    private int completedLevelsCount = 0;

    public interface RewardCallback {
        void onRewardEarned(int amount, String type);
        void onAdClosed(boolean rewarded);
    }

    private AdManager() {}

    public static AdManager getInstance() {
        if (instance == null) {
            synchronized (AdManager.class) {
                if (instance == null) {
                    instance = new AdManager();
                }
            }
        }
        return instance;
    }

    public void initialize(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        try {
            MobileAds.initialize(context, initializationStatus -> {
                Log.d(TAG, "Google Mobile Ads initialized successfully");
                preloadInterstitial();
                preloadRewarded();
            });
        } catch (Exception e) {
            Log.e(TAG, "MobileAds initialization error", e);
        }
    }

    public String getBannerAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(com.redcodersgroup.bubbleshooter.R.string.admob_banner_unit_id);
            } catch (Exception ignored) {}
        }
        return "ca-app-pub-1147620738869495/2081302570";
    }

    public String getInterstitialAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(com.redcodersgroup.bubbleshooter.R.string.admob_interstitial_unit_id);
            } catch (Exception ignored) {}
        }
        return "ca-app-pub-1147620738869495/1996707949";
    }

    public String getRewardedAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(com.redcodersgroup.bubbleshooter.R.string.admob_rewarded_unit_id);
            } catch (Exception ignored) {}
        }
        return "ca-app-pub-1147620738869495/4622871284";
    }

    // -------------------------------------------------------------
    // INTERSTITIAL ADS (Smart Pacing & Grace Period Protected)
    // -------------------------------------------------------------

    public void preloadInterstitial() {
        if (appContext == null || interstitialAd != null || isInterstitialLoading) return;

        isInterstitialLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(appContext, getInterstitialAdUnitId(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        isInterstitialLoading = false;
                        Log.d(TAG, "Interstitial Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isInterstitialLoading = false;
                        Log.w(TAG, "Interstitial Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    public boolean canShowInterstitial(int currentLevel) {
        // Suppress during introductory levels 1-3
        if (currentLevel <= 3) return false;

        long now = System.currentTimeMillis();
        // Check 5-min grace period if user recently watched a rewarded ad
        if ((now - lastRewardedWatchedTime) < REWARDED_GRACE_PERIOD_MS) {
            Log.d(TAG, "Skipping interstitial: user is in rewarded grace period");
            return false;
        }

        // Check time interval
        if ((now - lastInterstitialTime) < INTERSTITIAL_MIN_INTERVAL_MS) {
            return false;
        }

        return interstitialAd != null;
    }

    public void onLevelCompleted(Activity activity, int currentLevel, @Nullable Runnable onComplete) {
        completedLevelsCount++;
        if (completedLevelsCount % INTERSTITIAL_LEVEL_INTERVAL == 0 && canShowInterstitial(currentLevel)) {
            showInterstitial(activity, onComplete);
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    public void showInterstitial(@NonNull Activity activity, @Nullable Runnable onDismiss) {
        if (interstitialAd == null) {
            preloadInterstitial();
            if (onDismiss != null) onDismiss.run();
            return;
        }

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                lastInterstitialTime = System.currentTimeMillis();
                preloadInterstitial();
                if (onDismiss != null) onDismiss.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                interstitialAd = null;
                preloadInterstitial();
                if (onDismiss != null) onDismiss.run();
            }
        });

        interstitialAd.show(activity);
    }

    // -------------------------------------------------------------
    // REWARDED VIDEO ADS (Opt-in High Yield)
    // -------------------------------------------------------------

    public void preloadRewarded() {
        if (appContext == null || rewardedAd != null || isRewardedLoading) return;

        isRewardedLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(appContext, getRewardedAdUnitId(), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isRewardedLoading = false;
                        Log.d(TAG, "Rewarded Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        isRewardedLoading = false;
                        Log.w(TAG, "Rewarded Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    public boolean isRewardedReady() {
        return rewardedAd != null;
    }

    public void showRewardedVideo(@NonNull Activity activity, @NonNull RewardCallback callback) {
        if (rewardedAd == null) {
            preloadRewarded();
            callback.onAdClosed(false);
            return;
        }

        final boolean[] didEarnReward = {false};

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                lastRewardedWatchedTime = System.currentTimeMillis();
                preloadRewarded();
                callback.onAdClosed(didEarnReward[0]);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                preloadRewarded();
                callback.onAdClosed(false);
            }
        });

        rewardedAd.show(activity, (RewardItem rewardItem) -> {
            didEarnReward[0] = true;
            lastRewardedWatchedTime = System.currentTimeMillis();
            callback.onRewardEarned(rewardItem.getAmount(), rewardItem.getType());
        });
    }

    // -------------------------------------------------------------
    // BANNER ADS (Adaptive Anchored)
    // -------------------------------------------------------------

    public void loadBanner(@NonNull Activity activity, @NonNull FrameLayout container) {
        try {
            AdView adView = new AdView(activity);
            adView.setAdUnitId(getBannerAdUnitId());
            adView.setAdSize(AdSize.BANNER);

            container.removeAllViews();
            container.addView(adView);
            container.setVisibility(View.VISIBLE);

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error loading banner ad", e);
        }
    }
}
