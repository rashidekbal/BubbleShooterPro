package com.redcodersgroup.bubbleshooter.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.ads.AdManager;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.ActivityShopBinding;
import com.redcodersgroup.bubbleshooter.store.IapBillingManager;
import com.redcodersgroup.bubbleshooter.store.StoreManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.NoticeDialog;

import java.util.Locale;

public class ShopActivity extends BaseActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";
    public static final String TAB_HEARTS = "hearts";
    public static final String TAB_DIAMONDS = "diamonds";
    public static final String TAB_BOOSTERS = "boosters";

    private ActivityShopBinding binding;
    private PreferencesManager prefs;
    private SoundManager soundManager;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                updateLivesUI();
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    public static Intent createIntent(Context context, @Nullable String initialTab) {
        Intent intent = new Intent(context, ShopActivity.class);
        if (initialTab != null) {
            intent.putExtra(EXTRA_INITIAL_TAB, initialTab);
        }
        return intent;
    }

    private final IapBillingManager.BillingListener billingListener = new IapBillingManager.BillingListener() {
        @Override
        public void onProductDetailsUpdated() {
            runOnUiThread(ShopActivity.this::updateDiamondPricesUI);
        }

        @Override
        public void onPurchaseSuccess(String productId, int diamondsAdded) {
            soundManager.playPurchase();
            updateAllUI();
            NoticeDialog.showReward(ShopActivity.this, "VAULT", "PURCHASE SUCCESS", "+" + diamondsAdded + " DIAMONDS", "Diamonds successfully added to your vault!");
        }

        @Override
        public void onPurchaseFailed(String productId, String errorMessage) {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                NoticeDialog.showWarning(ShopActivity.this, "PURCHASE", "TRANSACTION INCOMPLETE", "PAYMENT NOT COMPLETED", errorMessage);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);

        initViews();
        updateAllUI();
        updateDiamondPricesUI();

        IapBillingManager.getInstance(this).addListener(billingListener);
        IapBillingManager.getInstance(this).startConnection();

        String initialTab = getIntent().getStringExtra(EXTRA_INITIAL_TAB);
        selectTab(initialTab != null ? initialTab : TAB_HEARTS, false);

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IapBillingManager.getInstance(this).removeListener(billingListener);
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateDiamondPricesUI() {
        if (binding == null) return;
        IapBillingManager billing = IapBillingManager.getInstance(this);
        binding.btnShopBuyPouch.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_150));
        binding.btnShopBuySack.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_500));
        binding.btnShopBuyChest.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_1500));
    }

    private void initViews() {
        binding.btnShopBack.setOnClickListener(v -> {
            soundManager.playClick();
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Tabs
        binding.tabHearts.setOnClickListener(v -> selectTab(TAB_HEARTS, true));
        binding.tabDiamonds.setOnClickListener(v -> selectTab(TAB_DIAMONDS, true));
        binding.tabBoosters.setOnClickListener(v -> selectTab(TAB_BOOSTERS, true));

        // 1. Watch Ad for +1 Life
        binding.cardShopWatchAd.setOnClickListener(v -> handleWatchAdForLife());
        binding.btnShopWatchAd.setOnClickListener(v -> handleWatchAdForLife());

        // 2. Buy One Heart (5 Diamonds)
        binding.cardShopBuyOneHeart.setOnClickListener(v -> handleBuyOneHeart());
        binding.btnShopBuyOneHeart.setOnClickListener(v -> handleBuyOneHeart());

        // 3. Buy Triple Hearts (12 Diamonds)
        binding.cardShopBuyTripleHearts.setOnClickListener(v -> handleBuyTripleHearts());
        binding.btnShopBuyTripleHearts.setOnClickListener(v -> handleBuyTripleHearts());

        // 4. Buy Full Refill (20 Diamonds)
        binding.cardShopBuyFullRefill.setOnClickListener(v -> handleBuyFullRefill());
        binding.btnShopBuyFullRefill.setOnClickListener(v -> handleBuyFullRefill());

        // 5. Daily Free Diamonds
        binding.cardShopDailyFree.setOnClickListener(v -> handleDailyFreeClaim());
        binding.btnShopDailyFree.setOnClickListener(v -> handleDailyFreeClaim());

        // 5b. Watch Ad for +5 Diamonds (REWARDED)
        binding.cardShopWatchAdDiamonds.setOnClickListener(v -> handleWatchAdForDiamonds());
        binding.btnShopWatchAdDiamonds.setOnClickListener(v -> handleWatchAdForDiamonds());

        // 6. Buy Pouch (150 Diamonds - ₹75 / $0.99)
        binding.cardShopBuyPouch.setOnClickListener(v -> IapBillingManager.getInstance(this).launchPurchaseFlow(this, StoreManager.SKU_DIAMONDS_150));
        binding.btnShopBuyPouch.setOnClickListener(v -> IapBillingManager.getInstance(this).launchPurchaseFlow(this, StoreManager.SKU_DIAMONDS_150));

        // 7. Buy Sack (500 Diamonds - ₹250 / $2.99)
        binding.cardShopBuySack.setOnClickListener(v -> IapBillingManager.getInstance(this).launchPurchaseFlow(this, StoreManager.SKU_DIAMONDS_500));
        binding.btnShopBuySack.setOnClickListener(v -> IapBillingManager.getInstance(this).launchPurchaseFlow(this, StoreManager.SKU_DIAMONDS_500));

        // 8. Buy Chest (1500 Diamonds - ₹750 / $6.99)
        binding.cardShopBuyChest.setOnClickListener(v -> IapBillingManager.getInstance(this).launchPurchaseFlow(this, StoreManager.SKU_DIAMONDS_1500));
        binding.btnShopBuyChest.setOnClickListener(v -> IapBillingManager.getInstance(this).launchPurchaseFlow(this, StoreManager.SKU_DIAMONDS_1500));

        updateDiamondPricesUI();

        // 9. Boosters (Individual +1 Single & +3 Pack Cards)
        binding.cardShopBuyBomb1.setOnClickListener(v -> handleBoosterPurchase("BOMB", 1, 15));
        binding.btnShopBuyBomb1.setOnClickListener(v -> handleBoosterPurchase("BOMB", 1, 15));

        binding.cardShopBuyBomb3.setOnClickListener(v -> handleBoosterPurchase("BOMB", 3, 40));
        binding.btnShopBuyBomb3.setOnClickListener(v -> handleBoosterPurchase("BOMB", 3, 40));

        binding.cardShopBuyFireball1.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", 1, 15));
        binding.btnShopBuyFireball1.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", 1, 15));

        binding.cardShopBuyFireball3.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", 3, 40));
        binding.btnShopBuyFireball3.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", 3, 40));

        binding.cardShopBuyRainbow1.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", 1, 20));
        binding.btnShopBuyRainbow1.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", 1, 20));

        binding.cardShopBuyRainbow3.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", 3, 50));
        binding.btnShopBuyRainbow3.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", 3, 50));

        binding.cardShopBuyLightning1.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", 1, 20));
        binding.btnShopBuyLightning1.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", 1, 20));

        binding.cardShopBuyLightning3.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", 3, 50));
        binding.btnShopBuyLightning3.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", 3, 50));
    }

    private void selectTab(String tab, boolean playSound) {
        if (playSound) {
            soundManager.playClick();
        }
        boolean isDiamonds = TAB_DIAMONDS.equalsIgnoreCase(tab);
        boolean isBoosters = TAB_BOOSTERS.equalsIgnoreCase(tab);
        boolean isHearts = !isDiamonds && !isBoosters; // defaults to hearts

        binding.tabHearts.setBackgroundResource(isHearts ? R.drawable.bg_shop_tab_active : R.drawable.bg_shop_tab_inactive);
        binding.tabDiamonds.setBackgroundResource(isDiamonds ? R.drawable.bg_shop_tab_active : R.drawable.bg_shop_tab_inactive);
        binding.tabBoosters.setBackgroundResource(isBoosters ? R.drawable.bg_shop_tab_active : R.drawable.bg_shop_tab_inactive);

        binding.sectionHearts.setVisibility(isHearts ? View.VISIBLE : View.GONE);
        binding.sectionDiamonds.setVisibility(isDiamonds ? View.VISIBLE : View.GONE);
        binding.sectionBoosters.setVisibility(isBoosters ? View.VISIBLE : View.GONE);

        binding.scrollShopInventory.post(() -> binding.scrollShopInventory.scrollTo(0, 0));
    }

    private void updateAllUI() {
        updateDiamondsUI();
        updateLivesUI();
        updateBoostersUI();
        updateDailyFreeUI();
    }

    private void updateDiamondsUI() {
        binding.tvShopDiamonds.setText(String.format(Locale.getDefault(), "%,d", prefs.getDiamonds()));
    }

    private void updateBoostersUI() {
        int bomb = prefs.getBombBoosters();
        int fireball = prefs.getFireballBoosters();
        int rainbow = prefs.getRainbowBoosters();
        int lightning = prefs.getLightningBoosters();

        binding.tvShopInventoryBomb.setText(
                android.text.Html.fromHtml("Explodes radius • In bag: <b>" + bomb + "</b>", android.text.Html.FROM_HTML_MODE_LEGACY)
        );
        binding.tvShopInventoryFireball.setText(
                android.text.Html.fromHtml("Pierces column • In bag: <b>" + fireball + "</b>", android.text.Html.FROM_HTML_MODE_LEGACY)
        );
        binding.tvShopInventoryRainbow.setText(
                android.text.Html.fromHtml("Matches any color • In bag: <b>" + rainbow + "</b>", android.text.Html.FROM_HTML_MODE_LEGACY)
        );
        binding.tvShopInventoryLightning.setText(
                android.text.Html.fromHtml("Clears full row • In bag: <b>" + lightning + "</b>", android.text.Html.FROM_HTML_MODE_LEGACY)
        );
    }

    private void updateDailyFreeUI() {
        if (prefs.canClaimDailyFreeDiamonds()) {
            binding.btnShopDailyFree.setEnabled(true);
            binding.btnShopDailyFree.setText("CLAIM");
            binding.btnShopDailyFree.setBackgroundResource(R.drawable.btn_pill_blank_green);
            binding.btnShopDailyFree.setTextColor(Color.WHITE);
            binding.btnShopDailyFree.setAlpha(1.0f);
            binding.tvShopDailyFreeSubtitle.setText("Free gift is ready to collect!");
            binding.tvShopDailyFreeSubtitle.setTextColor(Color.parseColor("#059669"));
        } else {
            binding.btnShopDailyFree.setEnabled(false);
            binding.btnShopDailyFree.setText("CLAIMED");
            binding.btnShopDailyFree.setBackgroundResource(R.drawable.btn_pill_blank_disabled);
            binding.btnShopDailyFree.setTextColor(Color.parseColor("#E2E8F0"));
            binding.btnShopDailyFree.setAlpha(0.75f);
            binding.tvShopDailyFreeSubtitle.setText("Collected today. Returns in 24h.");
            binding.tvShopDailyFreeSubtitle.setTextColor(Color.parseColor("#64748B"));
        }
    }

    private void updateLivesUI() {
        int lives = prefs.getLives();
        binding.tvShopHearts.setText(lives + "/5");

        ImageView[] slots = {
                binding.ivShopHeartSlot1,
                binding.ivShopHeartSlot2,
                binding.ivShopHeartSlot3,
                binding.ivShopHeartSlot4,
                binding.ivShopHeartSlot5
        };

        for (int i = 0; i < slots.length; i++) {
            slots[i].setImageResource(R.drawable.ic_heart_slot_full);
            if (i < lives) {
                slots[i].setAlpha(1.0f);
            } else {
                slots[i].setAlpha(0.25f);
            }
        }

        if (lives >= 5) {
            binding.tvShopTimer.setText("FULL ENERGY (5/5)");
            binding.tvShopTimer.setTextColor(Color.parseColor("#059669"));
        } else {
            long remainingSec = prefs.getSecondsUntilNextLife();
            long mins = remainingSec / 60;
            long secs = remainingSec % 60;
            binding.tvShopTimer.setText(String.format(Locale.getDefault(), "Next free heart in %02d:%02d", mins, secs));
            binding.tvShopTimer.setTextColor(Color.parseColor("#E11D48"));
        }
    }

    // -------------------------------------------------------------
    // ACTIONS & PURCHASES
    // -------------------------------------------------------------

    private void handleWatchAdForLife() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(this, "NOTICE", "HEARTS FULL", "MAXIMUM CAPACITY", "Your energy is already at maximum capacity (5/5).");
            return;
        }

        soundManager.playClick();
        AdManager.getInstance().showRewardedVideo(this, new AdManager.RewardCallback() {
            @Override
            public void onRewardEarned(int amount, String type) {
                soundManager.playBounce();
                prefs.addLives(1);
                AnalyticsManager.getInstance(ShopActivity.this).logHeartRefilled("ad", 1);
                updateLivesUI();
                NoticeDialog.showReward(ShopActivity.this, "REWARD", "LIFE RESTORED", "+1 HEART ADDED", "Ad reward granted! One heart has been added to your pool.");
            }

            @Override
            public void onAdClosed(boolean rewarded) {
                if (!rewarded) {
                    AdManager.getInstance().preloadRewarded();
                }
            }
        });
    }

    private void handleWatchAdForDiamonds() {
        soundManager.playClick();
        AdManager.getInstance().showRewardedVideo(this, new AdManager.RewardCallback() {
            @Override
            public void onRewardEarned(int amount, String type) {
                soundManager.playBounce();
                prefs.addDiamonds(5);
                Bundle bundle = new Bundle();
                bundle.putString("reward_type", "diamonds");
                bundle.putInt("amount", 5);
                AnalyticsManager.getInstance(ShopActivity.this).logEvent("rewarded_ad_diamonds", bundle);
                updateAllUI();
                NoticeDialog.showReward(ShopActivity.this, "REWARD", "DIAMONDS EARNED", "+5 DIAMONDS ADDED", "Ad reward granted! 5 diamonds have been added to your vault.");
            }

            @Override
            public void onAdClosed(boolean rewarded) {
                if (!rewarded) {
                    AdManager.getInstance().preloadRewarded();
                }
            }
        });
    }

    private void handleBuyOneHeart() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(this, "NOTICE", "HEARTS FULL", "MAXIMUM CAPACITY", "Your energy is already at maximum capacity (5/5).");
            return;
        }

        if (prefs.spendDiamonds(5)) {
            soundManager.playPurchase();
            prefs.addLives(1);
            AnalyticsManager.getInstance(this).logHeartRefilled("diamond", 1);
            updateAllUI();
            NoticeDialog.showReward(this, "PURCHASE", "LIFE RESTORED", "+1 HEART ADDED", "One heart added to your pool.");
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(this, "WARNING", "INSUFFICIENT DIAMONDS", "NEED MORE DIAMONDS", "You don't have enough diamonds for this refill.");
        }
    }

    private void handleBuyTripleHearts() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(this, "NOTICE", "HEARTS FULL", "MAXIMUM CAPACITY", "Your energy is already at maximum capacity (5/5).");
            return;
        }

        if (prefs.spendDiamonds(12)) {
            soundManager.playPurchase();
            prefs.addLives(3);
            AnalyticsManager.getInstance(this).logHeartRefilled("diamond", 3);
            updateAllUI();
            NoticeDialog.showReward(this, "PURCHASE", "TRIPLE HEARTS", "+3 HEARTS ADDED", "Three hearts added to your pool.");
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(this, "WARNING", "INSUFFICIENT DIAMONDS", "NEED MORE DIAMONDS", "You don't have enough diamonds for this refill.");
        }
    }

    private void handleBuyFullRefill() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(this, "NOTICE", "HEARTS FULL", "MAXIMUM CAPACITY", "Your energy is already at maximum capacity (5/5).");
            return;
        }

        if (prefs.spendDiamonds(20)) {
            soundManager.playPurchase();
            prefs.addLives(5);
            AnalyticsManager.getInstance(this).logHeartRefilled("diamond", 5);
            updateAllUI();
            NoticeDialog.showReward(this, "PURCHASE", "ENERGY FULL", "MAX 5 HEARTS RESTORED", "Energy pool fully restored.");
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(this, "WARNING", "INSUFFICIENT DIAMONDS", "NEED MORE DIAMONDS", "You don't have enough diamonds for this refill.");
        }
    }

    private void handleDailyFreeClaim() {
        if (prefs.canClaimDailyFreeDiamonds()) {
            soundManager.playPurchase();
            prefs.addDiamonds(StoreManager.DIAMONDS_DAILY_FREE);
            prefs.markDailyFreeDiamondsClaimed();
            com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().saveToCloud(this);
            updateAllUI();
            NoticeDialog.showReward(this, "REWARD", "DAILY GIFT", "+" + StoreManager.DIAMONDS_DAILY_FREE + " FREE DIAMONDS", "Free diamonds added to your vault. Return tomorrow for more!");
        } else {
            soundManager.playClick();
            NoticeDialog.showInfo(this, "NOTICE", "DAILY GIFT", "ALREADY CLAIMED", "You have already collected today's free gift. Check back tomorrow!");
        }
    }

    private void handleDiamondPackPurchase(int diamonds, String packName) {
        soundManager.playPurchase();
        prefs.addDiamonds(diamonds);
        com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().saveToCloud(this);
        updateAllUI();
        NoticeDialog.showReward(this, "VAULT", "PURCHASE SUCCESS", "+" + diamonds + " DIAMONDS", "Diamonds successfully added to your vault!");
    }

    private void handleBoosterPurchase(String type, int count, int cost) {
        if (prefs.spendDiamonds(cost)) {
            soundManager.playPurchase();
            String name = "";
            switch (type) {
                case "BOMB":
                    prefs.addBombBoosters(count);
                    name = count + "x BOMB BOOSTER" + (count > 1 ? "S" : "");
                    break;
                case "FIREBALL":
                    prefs.addFireballBoosters(count);
                    name = count + "x FIREBALL BOOSTER" + (count > 1 ? "S" : "");
                    break;
                case "RAINBOW":
                    prefs.addRainbowBoosters(count);
                    name = count + "x RAINBOW BOOSTER" + (count > 1 ? "S" : "");
                    break;
                case "LIGHTNING":
                    prefs.addLightningBoosters(count);
                    name = count + "x LIGHTNING BOOSTER" + (count > 1 ? "S" : "");
                    break;
            }
            com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().saveToCloud(this);
            updateAllUI();
            NoticeDialog.showReward(this, "STORE", "PURCHASE SUCCESS", "+" + count + " " + type + " BOOSTER" + (count > 1 ? "S" : ""), count + " booster" + (count > 1 ? "s" : "") + " added to your battle arsenal!");
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(this, "WARNING", "INSUFFICIENT DIAMONDS", "NEED MORE DIAMONDS", "You do not have enough diamonds to purchase this booster.");
        }
    }
}
