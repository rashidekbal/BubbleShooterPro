package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogStoreBinding;
import com.redcodersgroup.bubbleshooter.store.IapBillingManager;
import com.redcodersgroup.bubbleshooter.store.StoreManager;

public class StoreDialog extends Dialog {

    public interface StoreDialogListener {
        void onStoreClosed();
    }

    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final StoreDialogListener listener;
    private DialogStoreBinding binding;

    private final IapBillingManager.BillingListener billingListener = new IapBillingManager.BillingListener() {
        @Override
        public void onProductDetailsUpdated() {
            if (binding != null && isShowing()) {
                binding.getRoot().post(() -> updateDiamondPricesUI());
            }
        }

        @Override
        public void onPurchaseSuccess(String productId, int diamondsAdded) {
            if (binding != null && isShowing()) {
                binding.getRoot().post(() -> {
                    soundManager.playPurchase();
                    updateStoreUI();
                    NoticeDialog.showReward(getContext(), "VAULT", "PURCHASE SUCCESS", "+" + diamondsAdded + " DIAMONDS", "Diamonds successfully added to your vault!");
                });
            }
        }

        @Override
        public void onPurchaseFailed(String productId, String errorMessage) {
            if (errorMessage != null && !errorMessage.isEmpty() && isShowing()) {
                NoticeDialog.showWarning(getContext(), "PURCHASE", "TRANSACTION INCOMPLETE", "PAYMENT NOT COMPLETED", errorMessage);
            }
        }
    };

    public StoreDialog(@NonNull Context context, @Nullable StoreDialogListener listener) {
        super(context);
        this.prefs = new PreferencesManager(context);
        this.soundManager = SoundManager.getInstance(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogStoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.94),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        IapBillingManager.getInstance(getContext()).addListener(billingListener);
        IapBillingManager.getInstance(getContext()).startConnection();

        setOnDismissListener(dialog -> {
            IapBillingManager.getInstance(getContext()).removeListener(billingListener);
            if (listener != null) {
                listener.onStoreClosed();
            }
        });

        initClickListeners();
        updateStoreUI();
        updateDiamondPricesUI();
    }

    private void initClickListeners() {
        binding.btnCloseStore.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });

        // 1. Daily Free Diamonds
        binding.cardBuyDailyFree.setOnClickListener(v -> handleDailyFreeClaim());
        binding.btnBuyDailyFree.setOnClickListener(v -> handleDailyFreeClaim());

        // 1b. Pocket: 50 Diamonds (₹29 / $0.49)
        binding.cardBuyPocket.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_50));
        binding.btnBuyPocket.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_50));

        // 2. Pouch: 100 Diamonds (₹75 / $0.99)
        binding.cardBuyPouch.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_150));
        binding.btnBuyPouch.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_150));

        // 3. Sack: 350 Diamonds (₹250 / $2.99)
        binding.cardBuySack.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_500));
        binding.btnBuySack.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_500));

        // 4. Chest: 1,100 Diamonds (₹750 / $6.99)
        binding.cardBuyChest.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_1500));
        binding.btnBuyChest.setOnClickListener(v -> launchIap(StoreManager.SKU_DIAMONDS_1500));

        updateDiamondPricesUI();

        // 5. Mega Bundle (120 Diamonds)
        binding.cardBuyMegaBundle.setOnClickListener(v -> handleMegaBundlePurchase());
        binding.btnBuyMegaBundle.setOnClickListener(v -> handleMegaBundlePurchase());

        // 6. Bomb Booster (40 Diamonds)
        binding.cardBuyBomb.setOnClickListener(v -> handleBoosterPurchase("BOMB", StoreManager.COST_BOMB_PACK));
        binding.btnBuyBomb.setOnClickListener(v -> handleBoosterPurchase("BOMB", StoreManager.COST_BOMB_PACK));

        // 7. Fireball Booster (40 Diamonds)
        binding.cardBuyFireball.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", StoreManager.COST_FIREBALL_PACK));
        binding.btnBuyFireball.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", StoreManager.COST_FIREBALL_PACK));

        // 8. Lightning Booster (45 Diamonds)
        binding.cardBuyLightning.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", StoreManager.COST_LIGHTNING_PACK));
        binding.btnBuyLightning.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", StoreManager.COST_LIGHTNING_PACK));

        // 9. Rainbow Booster (50 Diamonds)
        binding.cardBuyRainbow.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", StoreManager.COST_RAINBOW_PACK));
        binding.btnBuyRainbow.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", StoreManager.COST_RAINBOW_PACK));

        // 10. Refill Lives (25 Diamonds)
        binding.cardBuyLives.setOnClickListener(v -> handleLivesRefill());
        binding.btnBuyLives.setOnClickListener(v -> handleLivesRefill());
    }

    private void handleDailyFreeClaim() {
        if (prefs.canClaimDailyFreeDiamonds()) {
            soundManager.playPurchase();
            prefs.addDiamonds(StoreManager.DIAMONDS_DAILY_FREE);
            prefs.markDailyFreeDiamondsClaimed();
            updateStoreUI();
            NoticeDialog.showReward(
                    getContext(),
                    "REWARD",
                    "DAILY GIFT",
                    "+" + StoreManager.DIAMONDS_DAILY_FREE + " FREE DIAMONDS",
                    "Free diamonds added to your vault. Return tomorrow for more!"
            );
        } else {
            soundManager.playClick();
            NoticeDialog.showInfo(
                    getContext(),
                    "NOTICE",
                    "DAILY GIFT",
                    "ALREADY CLAIMED",
                    "You have already collected today's free gift. Check back tomorrow!"
            );
        }
    }

    private void handleDiamondPackPurchase(int diamonds, String packName) {
        soundManager.playPurchase();
        prefs.addDiamonds(diamonds);
        Bundle bundle = new Bundle();
        bundle.putString("pack_name", packName);
        bundle.putInt("diamonds", diamonds);
        AnalyticsManager.getInstance(getContext()).logEvent("diamond_pack_purchased", bundle);
        updateStoreUI();
        NoticeDialog.showReward(
                getContext(),
                "PURCHASE",
                "ORDER COMPLETE",
                "+" + String.format(java.util.Locale.getDefault(), "%,d", diamonds) + " DIAMONDS",
                packName + " has been added to your vault."
        );
    }

    private void handleBoosterPurchase(String type, int cost) {
        BubbleType boosterType;
        try {
            boosterType = BubbleType.valueOf(type);
        } catch (Exception e) {
            boosterType = null;
        }

        if (boosterType != null && !prefs.isBoosterUnlockedGlobally(boosterType)) {
            soundManager.playClick();
            int reqLevel = prefs.getBoosterUnlockLevel(boosterType);
            String worldName = getWorldName(boosterType);
            NoticeDialog.showWarning(
                    getContext(),
                    "BOOSTER LOCKED",
                    "UNLOCKS AT LEVEL " + reqLevel,
                    "LOCKED POWER-UP",
                    "Reach Level " + reqLevel + " (" + worldName + ") to unlock and purchase this booster!"
            );
            return;
        }

        if (prefs.spendDiamonds(cost)) {
            soundManager.playPurchase();
            String name = "";
            switch (type) {
                case "BOMB":
                    prefs.addBombBoosters(3);
                    name = "3x BOMB BOOSTERS";
                    break;
                case "FIREBALL":
                    prefs.addFireballBoosters(3);
                    name = "3x FIREBALL BOOSTERS";
                    break;
                case "LIGHTNING":
                    prefs.addLightningBoosters(3);
                    name = "3x LIGHTNING BOOSTERS";
                    break;
                case "RAINBOW":
                    prefs.addRainbowBoosters(3);
                    name = "3x RAINBOW BOOSTERS";
                    break;
            }
            updateStoreUI();
            NoticeDialog.showReward(
                    getContext(),
                    "PURCHASE",
                    "BOOSTER UNLOCKED",
                    name,
                    "3 boosters added to your gameplay inventory."
            );
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(
                    getContext(),
                    "WARNING",
                    "INSUFFICIENT DIAMONDS",
                    "NEED MORE DIAMONDS",
                    "You do not have enough diamonds. Choose a pack above to top up."
            );
        }
    }

    private void handleMegaBundlePurchase() {
        if (!prefs.isBoosterUnlockedGlobally(BubbleType.BOMB)) {
            soundManager.playClick();
            NoticeDialog.showWarning(
                    getContext(),
                    "BUNDLE LOCKED",
                    "UNLOCKS AT LEVEL " + PreferencesManager.UNLOCK_LEVEL_BOMB,
                    "LOCKED BUNDLE",
                    "Reach Level " + PreferencesManager.UNLOCK_LEVEL_BOMB + " (World 2) to unlock power-up bundles!"
            );
            return;
        }

        int cost = 120;
        if (prefs.spendDiamonds(cost)) {
            soundManager.playPurchase();
            prefs.addBombBoosters(2);
            prefs.addFireballBoosters(2);
            prefs.addLightningBoosters(2);
            prefs.addRainbowBoosters(2);
            prefs.refillLives();
            updateStoreUI();
            NoticeDialog.showReward(
                    getContext(),
                    "BUNDLE",
                    "PURCHASE COMPLETE",
                    "ULTIMATE POWER PACK",
                    "2x each booster and full lives have been unlocked!"
            );
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(
                    getContext(),
                    "WARNING",
                    "INSUFFICIENT DIAMONDS",
                    "NEED MORE DIAMONDS",
                    "You do not have enough diamonds. Choose a pack above to top up."
            );
        }
    }

    private void handleLivesRefill() {
        dismiss();
        HeartStoreDialog heartDialog = new HeartStoreDialog(getContext(), () -> {
            if (listener != null) {
                listener.onStoreClosed();
            }
        });
        heartDialog.show();
    }

    private void launchIap(String productId) {
        if (getContext() instanceof Activity) {
            IapBillingManager.getInstance(getContext()).launchPurchaseFlow((Activity) getContext(), productId);
        }
    }

    private void updateDiamondPricesUI() {
        if (binding == null) return;
        IapBillingManager billing = IapBillingManager.getInstance(getContext());
        binding.btnBuyPocket.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_50));
        binding.btnBuyPouch.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_150));
        binding.btnBuySack.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_500));
        binding.btnBuyChest.setText(billing.getFormattedPrice(StoreManager.SKU_DIAMONDS_1500));
    }

    private void updateStoreUI() {
        if (binding == null) return;
        binding.tvStoreDialogDiamonds.setText(String.format(java.util.Locale.getDefault(), "%,d", prefs.getDiamonds()));

        boolean canClaim = prefs.canClaimDailyFreeDiamonds();
        binding.btnBuyDailyFree.setText(canClaim ? "FREE" : "CLAIMED");
        binding.btnBuyDailyFree.setBackgroundResource(canClaim ? R.drawable.bg_store_btn_green : R.drawable.bg_store_btn_disabled);
        binding.btnBuyDailyFree.setAlpha(canClaim ? 1.0f : 0.65f);

        int lives = prefs.getLives();
        binding.tvStoreLivesStatus.setText(lives >= 5 ? "Lives are FULL (5/5)" : "Current: " + lives + "/5 Hearts");

        updateBoostersUI();
    }

    private void updateBoostersUI() {
        if (binding == null) return;

        // Bomb (Level 21)
        boolean bombUnlocked = prefs.isBoosterUnlockedGlobally(BubbleType.BOMB);
        updateSingleBoosterUI(
                binding.cardBuyBomb,
                binding.btnBuyBomb,
                bombUnlocked,
                StoreManager.COST_BOMB_PACK,
                PreferencesManager.UNLOCK_LEVEL_BOMB
        );

        // Fireball (Level 61)
        boolean fireballUnlocked = prefs.isBoosterUnlockedGlobally(BubbleType.FIREBALL);
        updateSingleBoosterUI(
                binding.cardBuyFireball,
                binding.btnBuyFireball,
                fireballUnlocked,
                StoreManager.COST_FIREBALL_PACK,
                PreferencesManager.UNLOCK_LEVEL_FIREBALL
        );

        // Lightning (Level 81)
        boolean lightningUnlocked = prefs.isBoosterUnlockedGlobally(BubbleType.LIGHTNING);
        updateSingleBoosterUI(
                binding.cardBuyLightning,
                binding.btnBuyLightning,
                lightningUnlocked,
                StoreManager.COST_LIGHTNING_PACK,
                PreferencesManager.UNLOCK_LEVEL_LIGHTNING
        );

        // Rainbow (Level 41)
        boolean rainbowUnlocked = prefs.isBoosterUnlockedGlobally(BubbleType.RAINBOW);
        updateSingleBoosterUI(
                binding.cardBuyRainbow,
                binding.btnBuyRainbow,
                rainbowUnlocked,
                StoreManager.COST_RAINBOW_PACK,
                PreferencesManager.UNLOCK_LEVEL_RAINBOW
        );

        // Mega Bundle (Requires at least World 2 / Bomb unlocked at Level 21)
        boolean megaBundleUnlocked = prefs.isBoosterUnlockedGlobally(BubbleType.BOMB);
        if (megaBundleUnlocked) {
            binding.cardBuyMegaBundle.setAlpha(1.0f);
            binding.btnBuyMegaBundle.setText("120 💎");
            binding.btnBuyMegaBundle.setBackgroundResource(R.drawable.bg_store_btn_amber);
        } else {
            binding.cardBuyMegaBundle.setAlpha(0.55f);
            binding.btnBuyMegaBundle.setText("🔒 LVL " + PreferencesManager.UNLOCK_LEVEL_BOMB);
            binding.btnBuyMegaBundle.setBackgroundResource(R.drawable.bg_store_btn_disabled);
        }
    }

    private void updateSingleBoosterUI(View card, TextView btn, boolean isUnlocked, int cost, int unlockLevel) {
        if (isUnlocked) {
            card.setAlpha(1.0f);
            btn.setText(cost + " 💎");
            btn.setBackgroundResource(R.drawable.bg_store_btn_green);
        } else {
            card.setAlpha(0.55f);
            btn.setText("🔒 LVL " + unlockLevel);
            btn.setBackgroundResource(R.drawable.bg_store_btn_disabled);
        }
    }

    private String getWorldName(BubbleType type) {
        if (type == null) return "Later Worlds";
        switch (type) {
            case BOMB:
                return "World 2";
            case RAINBOW:
                return "World 3";
            case FIREBALL:
                return "World 4";
            case LIGHTNING:
                return "World 5";
            default:
                return "Later Worlds";
        }
    }
}
