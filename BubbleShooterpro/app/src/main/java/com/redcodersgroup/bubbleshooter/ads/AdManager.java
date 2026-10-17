package com.redcodersgroup.bubbleshooter.ads;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;

public class AdManager {

    private static final String TAG = "AdManager";
    private static volatile AdManager instance;

    // Interstitial Configuration: Show strictly on Boss Level completion (every 5 levels: 5, 10, 15, etc.)
    public static final int BOSS_LEVEL_INTERVAL = 5;

    private Context appContext;

    // App Open Ad
    private AppOpenAd appOpenAd;
    private boolean isAppOpenLoading = false;
    private boolean isShowingAppOpenAd = false;
    private long appOpenLoadTime = 0;

    // Interstitial Ads
    private InterstitialAd interstitialBossAd;
    private boolean isBossInterstitialLoading = false;

    private InterstitialAd interstitialNormalHomeAd;
    private boolean isNormalHomeLoading = false;

    private InterstitialAd interstitialEndlessEnterAd;
    private boolean isEndlessEnterLoading = false;

    private InterstitialAd interstitialEndlessExitAd;
    private boolean isEndlessExitLoading = false;

    // Rewarded Video Ad
    private RewardedAd rewardedAd;
    private boolean isRewardedLoading = false;

    // Fullscreen Ad Presentation State Tracking
    private boolean isShowingFullscreenAd = false;

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
            // Google Play Families Policy compliance:
            // 1. Tag for child-directed treatment (COPPA)
            // 2. Tag for under age of consent (GDPR-K)
            // 3. Restrict content rating to General Audiences (G)
            RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                    .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .build();
            MobileAds.setRequestConfiguration(requestConfiguration);

            MobileAds.initialize(context, initializationStatus -> {
                Log.d(TAG, "Google Mobile Ads initialized successfully with full Families Policy compliance");
                preloadAppOpenAd();
                preloadAllInterstitials();
                preloadRewarded();
            });
        } catch (Exception e) {
            Log.e(TAG, "MobileAds initialization error", e);
        }
    }

    // -------------------------------------------------------------
    // AD UNIT ID ACCESSORS (Production IDs active)
    // -------------------------------------------------------------

    public String getAppOpenAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_app_open_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/9257395921
        // Prod ID: ca-app-pub-1147620738869495/3776361306
        return "ca-app-pub-1147620738869495/3776361306";
    }

    public String getBannerAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_banner_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/6300978111
        // Prod ID: ca-app-pub-1147620738869495/2081302570
        return "ca-app-pub-1147620738869495/2081302570";
    }

    public String getNormalLevelHomeInterstitialAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_interstitial_normal_level_home_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/1033173712
        // Prod ID: ca-app-pub-1147620738869495/5089442971
        return "ca-app-pub-1147620738869495/5089442971";
    }

    public String getEndlessEnterInterstitialAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_interstitial_endless_enter_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/1033173712
        // Prod ID: ca-app-pub-1147620738869495/4316996361
        return "ca-app-pub-1147620738869495/4316996361";
    }

    public String getEndlessExitInterstitialAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_interstitial_endless_exit_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/1033173712
        // Prod ID: ca-app-pub-1147620738869495/1054829190
        return "ca-app-pub-1147620738869495/1054829190";
    }

    public String getInterstitialAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_interstitial_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/1033173712
        // Prod ID: ca-app-pub-1147620738869495/1996707949
        return "ca-app-pub-1147620738869495/1996707949";
    }

    public String getRewardedAdUnitId() {
        if (appContext != null) {
            try {
                return appContext.getString(R.string.admob_rewarded_unit_id);
            } catch (Exception ignored) {}
        }
        // Test ID: ca-app-pub-3940256099942544/5224354917
        // Prod ID: ca-app-pub-1147620738869495/4622871284
        return "ca-app-pub-1147620738869495/4622871284";
    }

    // -------------------------------------------------------------
    // APP OPEN ADS (Triggered on App Reopen from Background)
    // -------------------------------------------------------------

    private Activity pendingAppOpenActivity = null;
    private long pendingAppOpenRequestTime = 0;

    public void preloadAppOpenAd() {
        if (appContext == null || isAppOpenLoading || isAppOpenAdAvailable()) return;

        isAppOpenLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
                appContext,
                getAppOpenAdUnitId(),
                request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        isAppOpenLoading = false;
                        appOpenLoadTime = System.currentTimeMillis();
                        Log.d(TAG, "App Open Ad loaded successfully");

                        if (pendingAppOpenActivity != null &&
                                !pendingAppOpenActivity.isFinishing() &&
                                !pendingAppOpenActivity.isDestroyed() &&
                                (System.currentTimeMillis() - pendingAppOpenRequestTime) < 5000) {
                            Activity act = pendingAppOpenActivity;
                            pendingAppOpenActivity = null;
                            showAppOpenAdIfAvailable(act, null);
                        } else {
                            pendingAppOpenActivity = null;
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        appOpenAd = null;
                        isAppOpenLoading = false;
                        pendingAppOpenActivity = null;
                        Log.w(TAG, "App Open Ad failed to load: " + loadAdError.getMessage() + " (code: " + loadAdError.getCode() + ")");
                    }
                }
        );
    }

    public boolean isAppOpenAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (System.currentTimeMillis() - appOpenLoadTime);
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public boolean isShowingAppOpenAd() {
        return isShowingAppOpenAd;
    }

    public boolean isShowingFullscreenAd() {
        return isShowingAppOpenAd || isShowingFullscreenAd;
    }

    public void showAppOpenAdIfAvailable(@Nullable Activity activity, @Nullable Runnable onComplete) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (isShowingAppOpenAd) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (!isAppOpenAdAvailable()) {
            pendingAppOpenActivity = activity;
            pendingAppOpenRequestTime = System.currentTimeMillis();
            if (!isAppOpenLoading) {
                preloadAppOpenAd();
            }
            if (onComplete != null) onComplete.run();
            return;
        }

        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed() || !isAppOpenAdAvailable() || isShowingAppOpenAd) {
                if (onComplete != null) onComplete.run();
                return;
            }

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAppOpenAd = true;
                    AnalyticsManager.getInstance(activity).logAdImpression("app_open", "app_reopen");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isShowingAppOpenAd = false;
                    preloadAppOpenAd();
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    appOpenAd = null;
                    isShowingAppOpenAd = false;
                    AnalyticsManager.getInstance(activity).logAdFailedToShow("app_open", "app_reopen", adError.getMessage());
                    preloadAppOpenAd();
                    if (onComplete != null) onComplete.run();
                }
            });

            try {
                isShowingAppOpenAd = true;
                appOpenAd.show(activity);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying App Open Ad", e);
                isShowingAppOpenAd = false;
                appOpenAd = null;
                preloadAppOpenAd();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    // -------------------------------------------------------------
    // INTERSTITIAL ADS
    // -------------------------------------------------------------

    public void preloadAllInterstitials() {
        preloadInterstitial();
        preloadNormalLevelHomeInterstitial();
        preloadEndlessEnterInterstitial();
        preloadEndlessExitInterstitial();
    }

    public void preloadInterstitial() {
        if (appContext == null || interstitialBossAd != null || isBossInterstitialLoading) return;

        isBossInterstitialLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(appContext, getInterstitialAdUnitId(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialBossAd = ad;
                        isBossInterstitialLoading = false;
                        Log.d(TAG, "Boss Interstitial Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialBossAd = null;
                        isBossInterstitialLoading = false;
                        Log.w(TAG, "Boss Interstitial Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    public void preloadNormalLevelHomeInterstitial() {
        if (appContext == null || interstitialNormalHomeAd != null || isNormalHomeLoading) return;

        isNormalHomeLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(appContext, getNormalLevelHomeInterstitialAdUnitId(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialNormalHomeAd = ad;
                        isNormalHomeLoading = false;
                        Log.d(TAG, "Normal Level Home Interstitial Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialNormalHomeAd = null;
                        isNormalHomeLoading = false;
                        Log.w(TAG, "Normal Level Home Interstitial Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    public void preloadEndlessEnterInterstitial() {
        if (appContext == null || interstitialEndlessEnterAd != null || isEndlessEnterLoading) return;

        isEndlessEnterLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(appContext, getEndlessEnterInterstitialAdUnitId(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialEndlessEnterAd = ad;
                        isEndlessEnterLoading = false;
                        Log.d(TAG, "Endless Enter Interstitial Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialEndlessEnterAd = null;
                        isEndlessEnterLoading = false;
                        Log.w(TAG, "Endless Enter Interstitial Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    public void preloadEndlessExitInterstitial() {
        if (appContext == null || interstitialEndlessExitAd != null || isEndlessExitLoading) return;

        isEndlessExitLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(appContext, getEndlessExitInterstitialAdUnitId(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialEndlessExitAd = ad;
                        isEndlessExitLoading = false;
                        Log.d(TAG, "Endless Exit Interstitial Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialEndlessExitAd = null;
                        isEndlessExitLoading = false;
                        Log.w(TAG, "Endless Exit Interstitial Ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    public boolean isBossLevel(int level) {
        return level > 0 && (level % BOSS_LEVEL_INTERVAL == 0);
    }

    public boolean canShowInterstitial(int currentLevel) {
        return isBossLevel(currentLevel) && interstitialBossAd != null;
    }

    public void onLevelCompleted(Activity activity, int currentLevel, @Nullable Runnable onComplete) {
        if (isBossLevel(currentLevel)) {
            showInterstitial(activity, onComplete, "boss_level_completion");
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    /**
     * Triggered when Home button is clicked in normal level mode (Pause, Game Over, Victory).
     */
    public void showNormalLevelHomeInterstitial(@Nullable Activity activity, @Nullable Runnable onComplete) {
        showCustomInterstitial(activity, interstitialNormalHomeAd, "normal_level_home", () -> {
            interstitialNormalHomeAd = null;
            preloadNormalLevelHomeInterstitial();
            if (onComplete != null) onComplete.run();
        });
    }

    /**
     * Triggered when Endless Mode button is clicked on the main menu.
     */
    public void showEndlessEnterInterstitial(@Nullable Activity activity, @Nullable Runnable onComplete) {
        showCustomInterstitial(activity, interstitialEndlessEnterAd, "endless_mode_enter", () -> {
            interstitialEndlessEnterAd = null;
            preloadEndlessEnterInterstitial();
            if (onComplete != null) onComplete.run();
        });
    }

    /**
     * Triggered when exiting Endless Mode.
     */
    public void showEndlessExitInterstitial(@Nullable Activity activity, @Nullable Runnable onComplete) {
        showCustomInterstitial(activity, interstitialEndlessExitAd, "endless_mode_exit", () -> {
            interstitialEndlessExitAd = null;
            preloadEndlessExitInterstitial();
            if (onComplete != null) onComplete.run();
        });
    }

    public void onEndlessModeQuit(@Nullable Activity activity, @Nullable Runnable onComplete) {
        showEndlessExitInterstitial(activity, onComplete);
    }

    public void showInterstitial(@NonNull Activity activity, @Nullable Runnable onDismiss) {
        showInterstitial(activity, onDismiss, "boss_level_completion");
    }

    public void showInterstitial(@NonNull Activity activity, @Nullable Runnable onDismiss, @NonNull String placement) {
        showCustomInterstitial(activity, interstitialBossAd, placement, () -> {
            interstitialBossAd = null;
            preloadInterstitial();
            if (onDismiss != null) onDismiss.run();
        });
    }

    private void showCustomInterstitial(@Nullable Activity activity, @Nullable InterstitialAd ad, @NonNull String placement, @NonNull Runnable onDismissOrFailed) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            onDismissOrFailed.run();
            return;
        }

        if (ad == null) {
            onDismissOrFailed.run();
            return;
        }

        isShowingFullscreenAd = true;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                isShowingFullscreenAd = true;
                AnalyticsManager.getInstance(activity).logAdImpression("interstitial", placement);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                isShowingFullscreenAd = false;
                onDismissOrFailed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                isShowingFullscreenAd = false;
                AnalyticsManager.getInstance(activity).logAdFailedToShow("interstitial", placement, adError.getMessage());
                onDismissOrFailed.run();
            }
        });

        ad.show(activity);
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
        isShowingFullscreenAd = true;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                isShowingFullscreenAd = true;
                AnalyticsManager.getInstance(activity).logAdImpression("rewarded", "reward_placement");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                isShowingFullscreenAd = false;
                rewardedAd = null;
                preloadRewarded();
                callback.onAdClosed(didEarnReward[0]);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                isShowingFullscreenAd = false;
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
        if (activity.isFinishing() || activity.isDestroyed()) return;

        try {
            AdView adView = new AdView(activity);
            adView.setAdUnitId(getBannerAdUnitId());

            AdSize adSize = getAdaptiveBannerSize(activity, container);
            adView.setAdSize(adSize);

            container.removeAllViews();
            container.addView(adView);

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    container.setVisibility(View.VISIBLE);
                    AnalyticsManager.getInstance(activity).logAdImpression("banner", activity.getClass().getSimpleName());
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    Log.w(TAG, "Banner ad failed to load: " + error.getMessage());
                }
            });

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error loading banner ad", e);
        }
    }

    private AdSize getAdaptiveBannerSize(@NonNull Activity activity, @NonNull FrameLayout container) {
        try {
            DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
            float density = displayMetrics.density;
            float adWidthPixels = container.getWidth();
            if (adWidthPixels <= 0) {
                adWidthPixels = displayMetrics.widthPixels;
            }
            int adWidth = (int) (adWidthPixels / density);
            if (adWidth <= 0) adWidth = 320;
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
        } catch (Exception e) {
            return AdSize.BANNER;
        }
    }
}
