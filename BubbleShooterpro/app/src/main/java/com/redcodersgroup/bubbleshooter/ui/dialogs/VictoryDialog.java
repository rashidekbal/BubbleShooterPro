package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogVictoryBinding;

public class VictoryDialog extends Dialog {

    public interface VictoryDialogListener {
        void onNextLevelClicked();
        void onReplayClicked();
        void onHomeClicked();
    }

    private final VictoryDialogListener listener;
    private final int score;
    private final int highScore;
    private final int stars;
    private final String objectiveSummary;
    private final int shotsRemaining;
    private final int shotBonus;
    private DialogVictoryBinding binding;

    public VictoryDialog(@NonNull Context context, int score, int highScore, int stars, VictoryDialogListener listener) {
        this(context, score, highScore, stars, "✓ Level Completed!", 0, 0, listener);
    }

    public VictoryDialog(@NonNull Context context, int score, int highScore, int stars, String objectiveSummary, VictoryDialogListener listener) {
        this(context, score, highScore, stars, objectiveSummary, 0, 0, listener);
    }

    public VictoryDialog(@NonNull Context context, int score, int highScore, int stars, String objectiveSummary, int shotsRemaining, int shotBonus, VictoryDialogListener listener) {
        super(context);
        this.score = score;
        this.highScore = highScore;
        this.stars = stars;
        this.objectiveSummary = (objectiveSummary != null && !objectiveSummary.isEmpty()) ? objectiveSummary : "✓ Level Completed!";
        this.shotsRemaining = shotsRemaining;
        this.shotBonus = shotBonus;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogVictoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        binding.tvWinScore.setText(String.format(java.util.Locale.getDefault(), "%,d", score));
        binding.tvWinHighScore.setText("HIGH SCORE: " + String.format(java.util.Locale.getDefault(), "%,d", Math.max(score, highScore)));
        binding.tvWinObjectiveSummary.setText(objectiveSummary);

        if (shotsRemaining > 0 && shotBonus > 0) {
            binding.layoutWinShotsBonus.setVisibility(android.view.View.VISIBLE);
            binding.tvWinShotsBonusCalculation.setText(
                    String.format(java.util.Locale.getDefault(), "+%,d Bonus (%d Shots Left × 100 pts)", shotBonus, shotsRemaining)
            );
        } else {
            binding.layoutWinShotsBonus.setVisibility(android.view.View.GONE);
        }

        // Animate stars popping in with bounce
        animateStar(binding.ivWinStar1, stars >= 1, 200);
        animateStar(binding.ivWinStar2, stars >= 2, 450);
        animateStar(binding.ivWinStar3, stars >= 3, 700);

        attachButtonTouchFeedback(binding.btnWinNext);
        attachButtonTouchFeedback(binding.btnWinRestart);
        attachButtonTouchFeedback(binding.btnWinHome);

        binding.btnWinNext.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onNextLevelClicked();
        });

        binding.btnWinRestart.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onReplayClicked();
        });

        binding.btnWinHome.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onHomeClicked();
        });
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void attachButtonTouchFeedback(android.view.View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160)
                            .setInterpolator(new OvershootInterpolator(2.5f)).start();
                    break;
            }
            return false;
        });
    }

    private void animateStar(ImageView iv, boolean filled, long delay) {
        if (!filled) return;
        iv.postDelayed(() -> {
            iv.setImageResource(R.drawable.ic_star_filled);
            iv.setScaleX(0f);
            iv.setScaleY(0f);
            SoundManager.getInstance(getContext()).playBounce();
            iv.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(350)
                    .setInterpolator(new OvershootInterpolator(2.5f))
                    .withEndAction(() -> iv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start())
                    .start();
        }, delay);
    }
}
