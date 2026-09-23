package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogStoreBinding;
import com.redcodersgroup.bubbleshooter.store.StoreManager;

public class StoreDialog extends Dialog {

    public interface StoreDialogListener {
        void onStoreClosed();
    }

    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final StoreDialogListener listener;
    private DialogStoreBinding binding;

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

        setOnDismissListener(dialog -> {
            if (listener != null) {
                listener.onStoreClosed();
            }
        });

        initClickListeners();
        updateStoreUI();
    }

    private void initClickListeners() {
        binding.btnCloseStore.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });

        // 1. Daily Free Diamonds
        binding.cardBuyDailyFree.setOnClickListener(v -> handleDailyFreeClaim());
        binding.btnBuyDailyFree.setOnClickListener(v -> handleDailyFreeClaim());

        // 2. Pouch: 150 Diamonds ($0.99)
        binding.cardBuyPouch.setOnClickListener(v -> handleDiamondPackPurchase(150, "Handful"));
        binding.btnBuyPouch.setOnClickListener(v -> handleDiamondPackPurchase(150, "Handful"));

        // 3. Sack: 500 Diamonds ($2.99)
        binding.cardBuySack.setOnClickListener(v -> handleDiamondPackPurchase(500, "Sack of Gems"));
        binding.btnBuySack.setOnClickListener(v -> handleDiamondPackPurchase(500, "Sack of Gems"));

        // 4. Chest: 1,500 Diamonds ($6.99)
        binding.cardBuyChest.setOnClickListener(v -> handleDiamondPackPurchase(1500, "Royal Chest"));
        binding.btnBuyChest.setOnClickListener(v -> handleDiamondPackPurchase(1500, "Royal Chest"));

        // 5. Mega Bundle (120 Diamonds)
        binding.cardBuyMegaBundle.setOnClickListener(v -> handleMegaBundlePurchase());
        binding.btnBuyMegaBundle.setOnClickListener(v -> handleMegaBundlePurchase());

        // 6. Bomb Booster (40 Diamonds)
        binding.cardBuyBomb.setOnClickListener(v -> handleBoosterPurchase("BOMB", 40));
        binding.btnBuyBomb.setOnClickListener(v -> handleBoosterPurchase("BOMB", 40));

        // 7. Fireball Booster (40 Diamonds)
        binding.cardBuyFireball.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", 40));
        binding.btnBuyFireball.setOnClickListener(v -> handleBoosterPurchase("FIREBALL", 40));

        // 8. Lightning Booster (40 Diamonds)
        binding.cardBuyLightning.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", 40));
        binding.btnBuyLightning.setOnClickListener(v -> handleBoosterPurchase("LIGHTNING", 40));

        // 9. Rainbow Booster (50 Diamonds)
        binding.cardBuyRainbow.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", 50));
        binding.btnBuyRainbow.setOnClickListener(v -> handleBoosterPurchase("RAINBOW", 50));

        // 10. Refill Lives (25 Diamonds)
        binding.cardBuyLives.setOnClickListener(v -> handleLivesRefill());
        binding.btnBuyLives.setOnClickListener(v -> handleLivesRefill());
    }

    private void handleDailyFreeClaim() {
        if (prefs.canClaimDailyFreeDiamonds()) {
            soundManager.playWin();
            prefs.addDiamonds(30);
            prefs.markDailyFreeDiamondsClaimed();
            Toast.makeText(getContext(), "🎉 +30 Free Daily Diamonds Claimed!", Toast.LENGTH_SHORT).show();
            updateStoreUI();
        } else {
            soundManager.playClick();
            Toast.makeText(getContext(), "⏰ Daily Gift already claimed! Come back tomorrow.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDiamondPackPurchase(int diamonds, String packName) {
        soundManager.playWin();
        prefs.addDiamonds(diamonds);
        Bundle bundle = new Bundle();
        bundle.putString("pack_name", packName);
        bundle.putInt("diamonds", diamonds);
        AnalyticsManager.getInstance(getContext()).logEvent("diamond_pack_purchased", bundle);
        Toast.makeText(getContext(), "💎 +" + String.format(java.util.Locale.getDefault(), "%,d", diamonds) + " Diamonds added (" + packName + ")!", Toast.LENGTH_SHORT).show();
        updateStoreUI();
    }

    private void handleBoosterPurchase(String type, int cost) {
        if (prefs.spendDiamonds(cost)) {
            soundManager.playWin();
            String name = "";
            switch (type) {
                case "BOMB":
                    prefs.addBombBoosters(3);
                    name = "💣 3x Bomb Boosters";
                    break;
                case "FIREBALL":
                    prefs.addFireballBoosters(3);
                    name = "🔥 3x Fireball Boosters";
                    break;
                case "LIGHTNING":
                    prefs.addLightningBoosters(3);
                    name = "⚡ 3x Lightning Boosters";
                    break;
                case "RAINBOW":
                    prefs.addRainbowBoosters(3);
                    name = "🌈 3x Rainbow Boosters";
                    break;
            }
            Toast.makeText(getContext(), "✓ Purchased: " + name + "!", Toast.LENGTH_SHORT).show();
            updateStoreUI();
        } else {
            soundManager.playClick();
            Toast.makeText(getContext(), "❌ Not enough diamonds! Choose a diamond pack above.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMegaBundlePurchase() {
        int cost = 120;
        if (prefs.spendDiamonds(cost)) {
            soundManager.playWin();
            prefs.addBombBoosters(2);
            prefs.addFireballBoosters(2);
            prefs.addLightningBoosters(2);
            prefs.addRainbowBoosters(2);
            prefs.refillLives();
            Toast.makeText(getContext(), "⭐ Ultimate Power Pack Unlocked! 2x Each Booster + Full Lives!", Toast.LENGTH_LONG).show();
            updateStoreUI();
        } else {
            soundManager.playClick();
            Toast.makeText(getContext(), "❌ Not enough diamonds! Choose a diamond pack above.", Toast.LENGTH_SHORT).show();
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

    private void updateStoreUI() {
        if (binding == null) return;
        binding.tvStoreDialogDiamonds.setText(String.format(java.util.Locale.getDefault(), "%,d", prefs.getDiamonds()));

        boolean canClaim = prefs.canClaimDailyFreeDiamonds();
        binding.btnBuyDailyFree.setText(canClaim ? "FREE" : "CLAIMED");
        binding.btnBuyDailyFree.setAlpha(canClaim ? 1.0f : 0.6f);

        int lives = prefs.getLives();
        binding.tvStoreLivesStatus.setText(lives >= 5 ? "Lives are FULL (5/5)" : "Current: " + lives + "/5 Hearts");
    }
}
