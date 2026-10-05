package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogStarChestBinding;

public class StarChestDialog extends Dialog {

    public interface OnStarChestClaimedListener {
        void onStarChestClaimed(int diamondsEarned);
    }

    private final int currentStars;
    private final int targetStars;
    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final OnStarChestClaimedListener listener;
    private DialogStarChestBinding binding;
    private ObjectAnimator claimPulseAnimator;
    private boolean hasClaimedInThisDialog = false;

    public static final int REWARD_DIAMONDS = 2;

    public StarChestDialog(@NonNull Context context, int currentStars, int targetStars) {
        this(context, currentStars, targetStars, null);
    }

    public StarChestDialog(@NonNull Context context, int currentStars, int targetStars, @Nullable OnStarChestClaimedListener listener) {
        super(context);
        this.currentStars = currentStars;
        this.targetStars = targetStars;
        this.prefs = new PreferencesManager(context);
        this.soundManager = SoundManager.getInstance(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogStarChestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        boolean isReadyToClaim = currentStars >= targetStars;
        int clampedProgress = Math.min(currentStars, targetStars);
        binding.tvChestProgress.setText(clampedProgress + " / " + targetStars);
        if (binding.progressBarStarChest != null) {
            binding.progressBarStarChest.setMax(targetStars);
            binding.progressBarStarChest.setProgress(clampedProgress);
        }

        if (isReadyToClaim) {
            binding.layoutStarChestReward.setVisibility(View.VISIBLE);
            binding.tvStarChestRewardAmount.setText("+" + REWARD_DIAMONDS + " DIAMONDS");
            binding.tvChestSubtitle.setText("🎉 Target Reached!\nClaim your star reward chest!");
            binding.btnChestOk.setText("🎁 CLAIM REWARD");
            startPulseAnimation();
        } else {
            binding.layoutStarChestReward.setVisibility(View.GONE);
            int needed = targetStars - currentStars;
            binding.tvChestSubtitle.setText("Collect " + needed + " more stars from levels\nto open!");
            binding.btnChestOk.setText("OK");
        }

        binding.btnChestOk.setOnClickListener(v -> {
            if (isReadyToClaim && !hasClaimedInThisDialog) {
                hasClaimedInThisDialog = true;
                soundManager.playPurchase();
                prefs.incrementClaimedStarChestsCount();
                prefs.addDiamonds(REWARD_DIAMONDS);
                AnalyticsManager.getInstance(getContext()).logEvent("star_chest_claimed", null);

                if (listener != null) {
                    listener.onStarChestClaimed(REWARD_DIAMONDS);
                }
            } else {
                soundManager.playClick();
            }
            dismiss();
        });

        binding.btnCloseChest.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });
    }

    private void startPulseAnimation() {
        if (binding == null || binding.btnChestOk == null) return;
        stopPulseAnimation();

        View btn = binding.btnChestOk;
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.06f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.06f);
        claimPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btn, pvhX, pvhY);
        claimPulseAnimator.setDuration(650);
        claimPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        claimPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        claimPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        claimPulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (claimPulseAnimator != null) {
            claimPulseAnimator.cancel();
            claimPulseAnimator = null;
        }
        if (binding != null && binding.btnChestOk != null) {
            binding.btnChestOk.setScaleX(1.0f);
            binding.btnChestOk.setScaleY(1.0f);
        }
    }

    @Override
    public void dismiss() {
        stopPulseAnimation();
        super.dismiss();
    }
}
