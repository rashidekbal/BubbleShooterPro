package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redcodersgroup.bubbleshooter.ads.AdManager;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogOutOfHeartsBinding;
import com.redcodersgroup.bubbleshooter.ui.ShopActivity;

public class OutOfHeartsDialog extends Dialog {

    public interface OutOfHeartsDialogListener {
        void onPlayLevelWithAdReward();
        void onOpenShop();
        void onGoHome();
    }

    private final Activity hostActivity;
    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final OutOfHeartsDialogListener listener;
    private DialogOutOfHeartsBinding binding;
    private ObjectAnimator watchAdPulseAnimator;

    public OutOfHeartsDialog(@NonNull Context context, @Nullable OutOfHeartsDialogListener listener) {
        super(context);
        this.hostActivity = getActivityFromContext(context);
        this.prefs = new PreferencesManager(context);
        this.soundManager = SoundManager.getInstance(context);
        this.listener = listener;
    }

    @Nullable
    private static Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogOutOfHeartsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        initClickListeners();
        startPulseAnimation();
    }

    private void initClickListeners() {
        binding.btnCloseOutOfHearts.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });

        // 1. Watch Rewarded Ad to get +1 heart and immediately play the level
        binding.btnWatchAdToPlay.setOnClickListener(v -> handleWatchAdToPlay());

        // 2. Go to Shop (Hearts Refill tab)
        binding.btnGoToShop.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
            if (listener != null) {
                listener.onOpenShop();
            } else if (hostActivity != null) {
                hostActivity.startActivity(ShopActivity.createIntent(hostActivity, ShopActivity.TAB_HEARTS));
                hostActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        // 3. Go to Home Screen
        binding.btnGoToHome.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
            if (listener != null) {
                listener.onGoHome();
            }
        });
    }

    private void handleWatchAdToPlay() {
        soundManager.playClick();
        if (hostActivity == null || hostActivity.isFinishing() || hostActivity.isDestroyed()) {
            return;
        }

        AdManager.getInstance().showRewardedVideo(hostActivity, new AdManager.RewardCallback() {
            @Override
            public void onRewardEarned(int amount, String type) {
                soundManager.playPurchase();
                prefs.addLives(1);
                AnalyticsManager.getInstance(getContext()).logHeartRefilled("rewarded_ad_out_of_hearts", 1);

                if (hostActivity != null && !hostActivity.isFinishing() && !hostActivity.isDestroyed()) {
                    hostActivity.runOnUiThread(() -> {
                        dismiss();
                        if (listener != null) {
                            listener.onPlayLevelWithAdReward();
                        }
                    });
                }
            }

            @Override
            public void onAdClosed(boolean rewarded) {
                if (!rewarded) {
                    AdManager.getInstance().preloadRewarded();
                    if (hostActivity != null && !hostActivity.isFinishing() && !hostActivity.isDestroyed()) {
                        hostActivity.runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Watch the full video to get a free life and play!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void startPulseAnimation() {
        if (binding == null || binding.btnWatchAdToPlay == null) return;
        stopPulseAnimation();

        View btn = binding.btnWatchAdToPlay;
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.05f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.05f);
        watchAdPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btn, pvhX, pvhY);
        watchAdPulseAnimator.setDuration(700);
        watchAdPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        watchAdPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        watchAdPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        watchAdPulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (watchAdPulseAnimator != null) {
            watchAdPulseAnimator.cancel();
            watchAdPulseAnimator = null;
        }
        if (binding != null && binding.btnWatchAdToPlay != null) {
            binding.btnWatchAdToPlay.setScaleX(1.0f);
            binding.btnWatchAdToPlay.setScaleY(1.0f);
        }
    }

    @Override
    public void dismiss() {
        stopPulseAnimation();
        super.dismiss();
    }
}
