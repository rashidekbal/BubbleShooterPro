package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogHeartStoreBinding;

public class HeartStoreDialog extends Dialog {

    public interface HeartStoreDialogListener {
        void onHeartStoreClosed();
    }

    public static final int COST_ONE_HEART = 5;
    public static final int COST_TRIPLE_HEARTS = 12;
    public static final int COST_FULL_REFILL = 20;

    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final HeartStoreDialogListener listener;
    private DialogHeartStoreBinding binding;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isShowing()) {
                updateLivesUI();
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    public HeartStoreDialog(@NonNull Context context, @Nullable HeartStoreDialogListener listener) {
        super(context);
        this.prefs = new PreferencesManager(context);
        this.soundManager = SoundManager.getInstance(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogHeartStoreBinding.inflate(getLayoutInflater());
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
            timerHandler.removeCallbacks(timerRunnable);
            if (listener != null) {
                listener.onHeartStoreClosed();
            }
        });

        initClickListeners();
        updateLivesUI();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void initClickListeners() {
        binding.btnCloseHeartStore.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });

        // 1. Watch Ad for +1 Life (REWARDED AD)
        binding.cardWatchAdForLife.setOnClickListener(v -> handleWatchAdForLife());
        binding.btnWatchAdForLife.setOnClickListener(v -> handleWatchAdForLife());

        // 2. Buy Single Heart (5 Diamonds)
        binding.cardBuyOneHeart.setOnClickListener(v -> handleBuyOneHeart());
        binding.btnBuyOneHeart.setOnClickListener(v -> handleBuyOneHeart());

        // 4. Buy Triple Hearts (12 Diamonds)
        binding.cardBuyTripleHearts.setOnClickListener(v -> handleBuyTripleHearts());
        binding.btnBuyTripleHearts.setOnClickListener(v -> handleBuyTripleHearts());

        // 5. Buy Full Refill (20 Diamonds)
        binding.cardBuyFullRefill.setOnClickListener(v -> handleBuyFullRefill());
        binding.btnBuyFullRefill.setOnClickListener(v -> handleBuyFullRefill());
    }

    private void handleWatchAdForLife() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(
                    getContext(),
                    "NOTICE",
                    "HEARTS FULL",
                    "MAXIMUM CAPACITY",
                    "Your life energy is already fully restored (5/5)."
            );
            return;
        }

        soundManager.playWin();
        prefs.addLives(1);
        AnalyticsManager.getInstance(getContext()).logHeartRefilled("ad", 1);
        updateLivesUI();
        NoticeDialog.showReward(
                getContext(),
                "REWARD",
                "LIFE RESTORED",
                "+1 HEART ADDED",
                "Ad reward granted. One heart added to your pool."
        );
    }

    private void handleBuyOneHeart() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(
                    getContext(),
                    "NOTICE",
                    "HEARTS FULL",
                    "MAXIMUM CAPACITY",
                    "Your life energy is already fully restored (5/5)."
            );
            return;
        }

        if (prefs.spendDiamonds(COST_ONE_HEART)) {
            soundManager.playWin();
            prefs.addLives(1);
            AnalyticsManager.getInstance(getContext()).logHeartRefilled("diamond", 1);
            updateLivesUI();
            NoticeDialog.showReward(
                    getContext(),
                    "REFILL",
                    "PURCHASE COMPLETE",
                    "+1 HEART ADDED",
                    "One heart has been added to your pool."
            );
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(
                    getContext(),
                    "WARNING",
                    "INSUFFICIENT DIAMONDS",
                    "NEED MORE DIAMONDS",
                    "You do not have enough diamonds to purchase hearts."
            );
        }
    }

    private void handleBuyFullRefill() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(
                    getContext(),
                    "NOTICE",
                    "HEARTS FULL",
                    "MAXIMUM CAPACITY",
                    "Your life energy is already fully restored (5/5)."
            );
            return;
        }

        if (prefs.spendDiamonds(COST_FULL_REFILL)) {
            soundManager.playWin();
            prefs.refillLives();
            AnalyticsManager.getInstance(getContext()).logHeartRefilled("diamond", 5);
            updateLivesUI();
            NoticeDialog.showReward(
                    getContext(),
                    "REFILL",
                    "FULL RESTORATION",
                    "5/5 HEARTS RESTORED",
                    "Your life pool has been completely replenished."
            );
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(
                    getContext(),
                    "WARNING",
                    "INSUFFICIENT DIAMONDS",
                    "NEED MORE DIAMONDS",
                    "You do not have enough diamonds to purchase hearts."
            );
        }
    }

    private void handleBuyTripleHearts() {
        if (prefs.getLives() >= 5) {
            soundManager.playClick();
            NoticeDialog.showInfo(
                    getContext(),
                    "NOTICE",
                    "HEARTS FULL",
                    "MAXIMUM CAPACITY",
                    "Your life energy is already fully restored (5/5)."
            );
            return;
        }

        if (prefs.spendDiamonds(COST_TRIPLE_HEARTS)) {
            soundManager.playWin();
            prefs.addLives(3);
            AnalyticsManager.getInstance(getContext()).logHeartRefilled("diamond", 3);
            updateLivesUI();
            NoticeDialog.showReward(
                    getContext(),
                    "REFILL",
                    "PURCHASE COMPLETE",
                    "+3 HEARTS ADDED",
                    "Three hearts have been added to your pool."
            );
        } else {
            soundManager.playClick();
            NoticeDialog.showWarning(
                    getContext(),
                    "WARNING",
                    "INSUFFICIENT DIAMONDS",
                    "NEED MORE DIAMONDS",
                    "You do not have enough diamonds to purchase hearts."
            );
        }
    }

    private void updateLivesUI() {
        if (binding == null) return;

        binding.tvHeartStoreDiamonds.setText(String.format(java.util.Locale.getDefault(), "%,d", prefs.getDiamonds()));

        int lives = prefs.getLives();

        ImageView[] slots = new ImageView[]{
                binding.ivHeartSlot1,
                binding.ivHeartSlot2,
                binding.ivHeartSlot3,
                binding.ivHeartSlot4,
                binding.ivHeartSlot5
        };

        for (int i = 0; i < slots.length; i++) {
            slots[i].setImageResource(i < lives ? R.drawable.ic_heart_slot_full : R.drawable.ic_heart_slot_empty);
        }

        if (lives >= 5) {
            binding.tvHeartStatusText.setText("Lives: FULL (5/5 Hearts)");
            binding.tvHeartStatusText.setTextColor(Color.parseColor("#15803D"));
        } else {
            long secToNext = prefs.getSecondsUntilNextLife();
            long mins = secToNext / 60;
            long secs = secToNext % 60;
            binding.tvHeartStatusText.setText(String.format(java.util.Locale.getDefault(), "Lives: %d/5  •  Next in %02d:%02d", lives, mins, secs));
            binding.tvHeartStatusText.setTextColor(Color.parseColor("#BE123C"));
        }
    }
}
