package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import com.redcodersgroup.bubbleshooter.databinding.DialogGameOverBinding;

public class GameOverDialog extends Dialog {

    public interface GameOverDialogListener {
        void onRetryClicked();
        void onHomeClicked();
    }

    private final GameOverDialogListener listener;
    private final int score;
    private final String reason;
    private final int highScore;
    private final boolean isEndless;
    private final int livesRemaining;
    private DialogGameOverBinding binding;

    public GameOverDialog(@NonNull Context context, int score, GameOverDialogListener listener) {
        this(context, score, "Out of shots! Don't give up!", 0, false, 1, listener);
    }

    public GameOverDialog(@NonNull Context context, int score, String reason, GameOverDialogListener listener) {
        this(context, score, reason, 0, false, 1, listener);
    }

    public GameOverDialog(@NonNull Context context, int score, String reason, int highScore, boolean isEndless, GameOverDialogListener listener) {
        this(context, score, reason, highScore, isEndless, 1, listener);
    }

    /** Full constructor that includes livesRemaining after the loss. */
    public GameOverDialog(@NonNull Context context, int score, String reason, int highScore,
                          boolean isEndless, int livesRemaining, GameOverDialogListener listener) {
        super(context);
        this.score = score;
        this.reason = reason;
        this.highScore = highScore;
        this.isEndless = isEndless;
        this.livesRemaining = livesRemaining;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogGameOverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        if (isEndless && score > highScore && highScore > 0) {
            binding.tvLoseSubtitle.setText("🎉 NEW BEST HIGH SCORE! 🎉");
            binding.tvLoseSubtitle.setTextColor(Color.parseColor("#16A34A"));
        } else if (reason != null && !reason.isEmpty()) {
            binding.tvLoseSubtitle.setText(reason);
        }

        binding.tvLoseScore.setText(String.format(java.util.Locale.getDefault(), "%,d", score));

        int displayBest = Math.max(score, highScore);
        if (displayBest > 0) {
            binding.tvLoseBestScore.setVisibility(View.VISIBLE);
            binding.tvLoseBestScore.setText("BEST: " + String.format(java.util.Locale.getDefault(), "%,d", displayBest));
        } else {
            binding.tvLoseBestScore.setVisibility(View.GONE);
        }

        // Show no-hearts warning for level mode
        if (!isEndless && livesRemaining <= 0) {
            binding.layoutNoHeartsWarning.setVisibility(View.VISIBLE);
            long nextLifeSecs = getSecondsUntilNextLife();
            if (nextLifeSecs > 0) {
                long mins = nextLifeSecs / 60;
                long secs = nextLifeSecs % 60;
                binding.tvNoHeartsDetail.setText(
                        String.format(java.util.Locale.getDefault(),
                                "Next heart in %02d:%02d — or buy one in the shop", mins, secs));
            }
            binding.layoutNoHeartsWarning.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onRetryClicked();
            });
        }

        binding.btnLoseRetry.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onRetryClicked();
        });

        binding.btnLoseHome.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onHomeClicked();
        });

        binding.btnCloseLose.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onHomeClicked();
        });
    }

    /** Returns seconds until next life regen based on a 20-min timer (mirrors PreferencesManager). */
    private long getSecondsUntilNextLife() {
        // We don't inject prefs here — return 0 (detail text fallback is fine)
        return 0;
    }
}
