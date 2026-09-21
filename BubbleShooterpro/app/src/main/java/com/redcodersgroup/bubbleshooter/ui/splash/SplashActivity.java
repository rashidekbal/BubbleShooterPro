package com.redcodersgroup.bubbleshooter.ui.splash;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import com.redcodersgroup.bubbleshooter.MainActivity;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.databinding.ActivitySplashBinding;
import com.redcodersgroup.bubbleshooter.ui.BaseActivity;

@android.annotation.SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private boolean isNavigated = false;
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        soundManager = SoundManager.getInstance(this);

        startEntranceAnimations();
        startLoadingSimulation();
    }

    private void startEntranceAnimations() {
        // Logo entrance: Pop & Overshoot
        binding.ivGameLogo.setScaleX(0.65f);
        binding.ivGameLogo.setScaleY(0.65f);
        binding.ivGameLogo.setAlpha(0.0f);

        binding.ivGameLogo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .withEndAction(this::startLogoFloatingBob)
                .start();
    }

    private void startLogoFloatingBob() {
        // Gentle up-and-down floating motion
        ObjectAnimator bobAnim = ObjectAnimator.ofFloat(binding.ivGameLogo, "translationY", 0f, -10f, 0f);
        bobAnim.setDuration(2200);
        bobAnim.setRepeatCount(ValueAnimator.INFINITE);
        bobAnim.setRepeatMode(ValueAnimator.RESTART);
        bobAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        bobAnim.start();
    }

    private void startLoadingSimulation() {
        binding.layoutProgressTrack.post(() -> {
            int trackWidth = binding.layoutProgressTrack.getWidth();
            // Account for padding (6dp total)
            final int maxFillWidth = trackWidth - Math.round(6 * getResources().getDisplayMetrics().density);

            ValueAnimator progressAnim = ValueAnimator.ofFloat(0f, 1f);
            progressAnim.setDuration(2500);
            progressAnim.setInterpolator(new DecelerateInterpolator(1.2f));

            progressAnim.addUpdateListener(animator -> {
                float fraction = (float) animator.getAnimatedValue();
                int currentWidth = Math.round(fraction * maxFillWidth);

                ViewGroup.LayoutParams lp = binding.viewProgressFill.getLayoutParams();
                if (lp != null) {
                    lp.width = currentWidth;
                    binding.viewProgressFill.setLayoutParams(lp);
                }

                int percent = Math.min(100, Math.round(fraction * 100));
                binding.tvProgressPercent.setText(percent + "%");

                // Dynamic contextual loading hints
                if (percent < 28) {
                    binding.tvLoadingHint.setText("Exploring Bubble Meadows...");
                } else if (percent < 55) {
                    binding.tvLoadingHint.setText("Brewing Rainbow Potions...");
                } else if (percent < 82) {
                    binding.tvLoadingHint.setText("Polishing Fairy Crystals...");
                } else if (percent < 100) {
                    binding.tvLoadingHint.setText("Entering Magical Realms...");
                } else {
                    binding.tvLoadingHint.setText("Ready to Pop!");
                }
            });

            progressAnim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    binding.getRoot().postDelayed(() -> {
                        if (!isNavigated) {
                            isNavigated = true;
                            navigateToHome();
                        }
                    }, 350);
                }
            });

            progressAnim.start();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
