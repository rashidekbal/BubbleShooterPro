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

import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogClaimGiftBinding;

public class ClaimGiftDialog extends Dialog {

    public interface OnGiftClaimedListener {
        void onGiftClaimed(int worldNumber, int giftIndex, int diamondsEarned);
    }

    private final int worldNumber;
    private final int giftIndex;
    private final int rewardDiamonds;
    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final OnGiftClaimedListener listener;
    private DialogClaimGiftBinding binding;
    private ObjectAnimator claimPulseAnimator;

    private boolean hasClaimedInThisDialog = false;

    public ClaimGiftDialog(@NonNull Context context, int worldNumber, int giftIndex, int rewardDiamonds, @Nullable OnGiftClaimedListener listener) {
        super(context);
        this.worldNumber = worldNumber;
        this.giftIndex = giftIndex;
        this.rewardDiamonds = Math.max(1, rewardDiamonds);
        this.prefs = new PreferencesManager(context);
        this.soundManager = SoundManager.getInstance(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogClaimGiftBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        binding.tvGiftTag.setText("WORLD " + worldNumber + " PATH REWARD");
        binding.tvGiftRewardAmount.setText("+" + rewardDiamonds + " DIAMONDS");

        boolean alreadyClaimed = prefs.hasClaimedWorldGift(worldNumber, giftIndex);
        if (alreadyClaimed) {
            binding.ivGiftChestIcon.setImageResource(R.drawable.ic_star_chest_open);
            binding.btnClaimGift.setText("CLAIMED");
            binding.btnClaimGift.setEnabled(false);
            binding.btnClaimGift.setAlpha(0.6f);
            binding.tvGiftDescription.setText("You have already collected this chest reward!");
        } else {
            binding.ivGiftChestIcon.setImageResource(R.drawable.ic_star_chest_closed);
            startPulseAnimation();
        }

        binding.btnCloseClaimGift.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });

        binding.btnClaimGift.setOnClickListener(v -> {
            if (alreadyClaimed || hasClaimedInThisDialog || prefs.hasClaimedWorldGift(worldNumber, giftIndex)) {
                dismiss();
                return;
            }
            hasClaimedInThisDialog = true;
            soundManager.playPurchase();
            binding.ivGiftChestIcon.setImageResource(R.drawable.ic_star_chest_open);
            prefs.setClaimedWorldGift(worldNumber, giftIndex, true);
            prefs.addDiamonds(rewardDiamonds);
            AnalyticsManager.getInstance(getContext()).logGiftChestClaimed(worldNumber, giftIndex, rewardDiamonds);

            if (listener != null) {
                listener.onGiftClaimed(worldNumber, giftIndex, rewardDiamonds);
            }
            dismiss();
        });
    }

    private void startPulseAnimation() {
        if (binding == null || binding.btnClaimGift == null) return;
        stopPulseAnimation();

        View btn = binding.btnClaimGift;
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.06f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.06f);
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
        if (binding != null && binding.btnClaimGift != null) {
            binding.btnClaimGift.setScaleX(1.0f);
            binding.btnClaimGift.setScaleY(1.0f);
        }
    }

    @Override
    public void dismiss() {
        stopPulseAnimation();
        super.dismiss();
    }
}
