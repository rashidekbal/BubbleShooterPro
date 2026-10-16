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
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;

public class AdManager {

    private static final String TAG = "AdManager";
    private static volatile AdManager instance;

    // Interstitial Configuration: Show strictly on Boss Level completion (every 5 levels: 5, 10, 15, etc.)
    public static final int BOSS_LEVEL_INTERVAL = 5;

    private Context appContext;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    private boolean isInterstitialLoading = false;
    private boolean isRewardedLoading = false;

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
            // Google Play Families Policy compliance: Enforce child-directed treatment & G rating
            RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .build();
            MobileAds.setRequestConfiguration(requestConfiguration);

            MobileAds.initialize(context, initializationStatus -> {
                Log.d(TAG, "Google Mobile Ads initialized successfully with Families compliance");
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
        return "ca-app-pub-3940256099942544/6300978111";
    }

    public String getInterstitialAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(com.redcodersgroup.bubbleshooter.R.string.admob_interstitial_unit_id);
            } catch (Exception ignored) {}
        }
        return "ca-app-pub-3940256099942544/1033173712";
    }

    public String getRewardedAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(com.redcodersgroup.bubbleshooter.R.string.admob_rewarded_unit_id);
            } catch (Exception ignored) {}
        }
        return "ca-app-pub-3940256099942544/5224354917";
    }

    // -------------------------------------------------------------
    // INTERSTITIAL ADS (Triggered on Boss Level Completion: 5, 10, 15, etc.)
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

    public boolean isBossLevel(int level) {
        return level > 0 && (level % BOSS_LEVEL_INTERVAL == 0);
    }

    public boolean canShowInterstitial(int currentLevel) {
        return isBossLevel(currentLevel) && interstitialAd != null;
    }

    public void onLevelCompleted(Activity activity, int currentLevel, @Nullable Runnable onComplete) {
        if (isBossLevel(currentLevel)) {
            showInterstitial(activity, onComplete, "boss_level_completion");
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    public void onEndlessModeQuit(Activity activity, @Nullable Runnable onComplete) {
        showInterstitial(activity, onComplete, "endless_mode_quit");
    }

    public void showInterstitial(@NonNull Activity activity, @Nullable Runnable onDismiss) {
        showInterstitial(activity, onDismiss, "boss_level_completion");
    }

    public void showInterstitial(@NonNull Activity activity, @Nullable Runnable onDismiss, @NonNull String placement) {
        if (interstitialAd == null) {
            preloadInterstitial();
            if (onDismiss != null) onDismiss.run();
            return;
        }

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                AnalyticsManager.getInstance(activity).logAdImpression("interstitial", placement);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                preloadInterstitial();
                if (onDismiss != null) onDismiss.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                interstitialAd = null;
                AnalyticsManager.getInstance(activity).logAdFailedToShow("interstitial", placement, adError.getMessage());
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
            public void onAdShowedFullScreenContent() {
                AnalyticsManager.getInstance(activity).logAdImpression("rewarded", "reward_placement");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                preloadRewarded();
                callback.onAdClosed(didEarnReward[0]);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                AnalyticsManager.getInstance(activity).logAdFailedToShow("rewarded", "reward_placement", adError.getMessage());
                preloadRewarded();
                callback.onAdClosed(false);
            }
        });

        rewardedAd.show(activity, (RewardItem rewardItem) -> {
            didEarnReward[0] = true;
            AnalyticsManager.getInstance(activity).logRewardedAdWatched("reward_placement", rewardItem.getType(), rewardItem.getAmount());
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
