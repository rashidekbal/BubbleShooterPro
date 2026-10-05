package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogStarChestBinding;
import com.redcodersgroup.bubbleshooter.store.StoreManager;

import java.util.ArrayList;
import java.util.List;

public class StarChestDialog extends Dialog {

    public interface OnStarChestClaimedListener {
        void onStarChestClaimed(int diamondsEarned);
    }

    private final int totalStars;
    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final OnStarChestClaimedListener listener;
    private DialogStarChestBinding binding;
    private StarRewardCardAdapter adapter;
    private final List<StarRewardCardAdapter.MilestoneRewardItem> rewardItems = new ArrayList<>();

    public static final int REWARD_DIAMONDS_PER_CHEST = 2;

    public StarChestDialog(@NonNull Context context, int totalStars, int claimedCount) {
        this(context, totalStars, claimedCount, null);
    }

    public StarChestDialog(@NonNull Context context, int totalStars, int claimedCount, @Nullable OnStarChestClaimedListener listener) {
        super(context);
        this.totalStars = totalStars;
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
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.94);
            int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.88);
            getWindow().setLayout(width, height);
        }

        setupDailyFreeSection();
        setupProgressSection();
        setupRewardsList();
        setupClickListeners();
    }

    private void setupProgressSection() {
        int currentProgress = Math.max(0, totalStars % 20);
        int needed = 20 - currentProgress;

        binding.tvChestProgress.setText(currentProgress + " / 20");
        if (binding.progressBarStarChest != null) {
            binding.progressBarStarChest.setMax(20);
            binding.progressBarStarChest.setProgress(currentProgress);
        }
    }

    private void setupRewardsList() {
        int totalMilestonesEarned = totalStars / 20;
        int claimedCount = prefs.getClaimedStarChestsCount();
        int unclaimedCount = Math.max(0, totalMilestonesEarned - claimedCount);

        rewardItems.clear();
        for (int i = 1; i <= unclaimedCount; i++) {
            rewardItems.add(new StarRewardCardAdapter.MilestoneRewardItem(claimedCount + i, REWARD_DIAMONDS_PER_CHEST));
        }

        adapter = new StarRewardCardAdapter(rewardItems, (position, item) -> claimSingleCard(position, item));
        binding.rvStarMilestoneCards.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStarMilestoneCards.setAdapter(adapter);

        updateRewardsViewState();
    }

    private void setupDailyFreeSection() {
        if (binding == null) return;
        boolean canClaim = prefs.canClaimDailyFreeDiamonds();
        if (canClaim) {
            binding.btnClaimDailyFree.setEnabled(true);
            binding.btnClaimDailyFree.setText("CLAIM");
            binding.btnClaimDailyFree.setBackgroundResource(R.drawable.btn_pill_blank_green);
            binding.btnClaimDailyFree.setTextColor(Color.WHITE);
            binding.btnClaimDailyFree.setAlpha(1.0f);
            binding.tvDailyFreeStatus.setText("Daily gift ready to collect!");
            binding.tvDailyFreeStatus.setTextColor(Color.parseColor("#059669"));
        } else {
            binding.btnClaimDailyFree.setEnabled(false);
            binding.btnClaimDailyFree.setText("CLAIMED");
            binding.btnClaimDailyFree.setBackgroundResource(R.drawable.btn_pill_blank_disabled);
            binding.btnClaimDailyFree.setTextColor(Color.parseColor("#94A3B8"));
            binding.btnClaimDailyFree.setAlpha(0.8f);
            binding.tvDailyFreeStatus.setText("Collected today. Returns tomorrow.");
            binding.tvDailyFreeStatus.setTextColor(Color.parseColor("#64748B"));
        }
    }

    private void setupClickListeners() {
        binding.btnCloseChest.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });

        binding.btnClaimDailyFree.setOnClickListener(v -> claimDailyFreeDiamonds());
        binding.cardDailyFreeReward.setOnClickListener(v -> {
            if (prefs.canClaimDailyFreeDiamonds()) {
                claimDailyFreeDiamonds();
            }
        });

        binding.btnCollectAll.setOnClickListener(v -> claimAllCards());
    }

    private void claimDailyFreeDiamonds() {
        if (!prefs.canClaimDailyFreeDiamonds()) return;
        soundManager.playPurchase();
        prefs.addDiamonds(StoreManager.DIAMONDS_DAILY_FREE);
        prefs.markDailyFreeDiamondsClaimed();
        AnalyticsManager.getInstance(getContext()).logEvent("daily_free_diamonds_claimed", null);
        triggerCloudSave();

        setupDailyFreeSection();
        updateRewardsViewState();

        if (listener != null) {
            listener.onStarChestClaimed(StoreManager.DIAMONDS_DAILY_FREE);
        }
    }

    private void claimSingleCard(int position, StarRewardCardAdapter.MilestoneRewardItem item) {
        soundManager.playPurchase();
        prefs.incrementClaimedStarChestsCount();
        prefs.addDiamonds(item.diamondsReward);
        AnalyticsManager.getInstance(getContext()).logEvent("star_chest_claimed", null);
        triggerCloudSave();

        adapter.removeCardAt(position);
        updateRewardsViewState();

        if (listener != null) {
            listener.onStarChestClaimed(item.diamondsReward);
        }
    }

    private void claimAllCards() {
        int milestoneCount = rewardItems.size();
        boolean hasDaily = prefs.canClaimDailyFreeDiamonds();
        if (milestoneCount == 0 && !hasDaily) return;

        soundManager.playPurchase();
        int totalDiamonds = milestoneCount * REWARD_DIAMONDS_PER_CHEST;

        if (milestoneCount > 0) {
            prefs.addClaimedStarChestsCount(milestoneCount);
            AnalyticsManager.getInstance(getContext()).logEvent("star_chest_claimed_all", null);
            rewardItems.clear();
            adapter.notifyDataSetChanged();
        }

        if (hasDaily) {
            totalDiamonds += StoreManager.DIAMONDS_DAILY_FREE;
            prefs.markDailyFreeDiamondsClaimed();
            AnalyticsManager.getInstance(getContext()).logEvent("daily_free_diamonds_claimed", null);
            setupDailyFreeSection();
        }

        prefs.addDiamonds(totalDiamonds);
        triggerCloudSave();
        updateRewardsViewState();

        if (listener != null) {
            listener.onStarChestClaimed(totalDiamonds);
        }
    }

    private void triggerCloudSave() {
        android.app.Activity activity = null;
        Context ctx = getContext();
        while (ctx instanceof android.content.ContextWrapper) {
            if (ctx instanceof android.app.Activity) {
                activity = (android.app.Activity) ctx;
                break;
            }
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
        }
        if (activity != null) {
            com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().saveToCloud(activity);
        }
    }

    private void updateRewardsViewState() {
        int remainingMilestones = rewardItems.size();
        boolean dailyAvailable = prefs.canClaimDailyFreeDiamonds();
        int totalAvailable = remainingMilestones + (dailyAvailable ? 1 : 0);

        if (remainingMilestones > 0) {
            binding.rvStarMilestoneCards.setVisibility(View.VISIBLE);
            binding.layoutEmptyRewards.setVisibility(View.GONE);
            binding.tvUnclaimedCountTag.setText(remainingMilestones + " available");
        } else {
            binding.rvStarMilestoneCards.setVisibility(View.GONE);
            binding.layoutEmptyRewards.setVisibility(View.VISIBLE);
            binding.tvUnclaimedCountTag.setText("0 available");
        }

        if (totalAvailable > 0) {
            binding.btnCollectAll.setEnabled(true);
            binding.btnCollectAll.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.btn_pill_blank_green));
            binding.btnCollectAll.setText("🎁 COLLECT ALL (" + totalAvailable + ")");
        } else {
            binding.btnCollectAll.setEnabled(false);
            binding.btnCollectAll.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.btn_pill_blank_disabled));
            binding.btnCollectAll.setText("ALL COLLECTED");
        }
    }
}
