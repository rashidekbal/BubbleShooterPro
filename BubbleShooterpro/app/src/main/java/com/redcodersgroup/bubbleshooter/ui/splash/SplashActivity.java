package com.redcodersgroup.bubbleshooter.ui.splash;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.NonNull;
import com.google.android.gms.games.Player;
import com.redcodersgroup.bubbleshooter.MainActivity;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.auth.CloudSaveManager;
import com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.ActivitySplashBinding;
import com.redcodersgroup.bubbleshooter.profile.AvatarManager;
import com.redcodersgroup.bubbleshooter.ui.BaseActivity;

@android.annotation.SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private boolean isNavigated = false;
    private boolean isAnimFinished = false;
    private boolean isSyncFinished = false;
    private SoundManager soundManager;
    private PreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        soundManager = SoundManager.getInstance(this);
        prefs = new PreferencesManager(this);

        startEntranceAnimations();
        startLoadingSimulation();
        startCloudRestore();

        // Safety fallback: ensure splash never hangs longer than 4.5 seconds
        binding.getRoot().postDelayed(() -> {
            if (!isNavigated) {
                isSyncFinished = true;
                isAnimFinished = true;
                checkProceedToHome();
            }
        }, 4500);
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

    private void startCloudRestore() {
        PlayGamesAuthManager.getInstance().checkSilentSignIn(this, new PlayGamesAuthManager.AuthCallback() {
            @Override
            public void onSuccess(@NonNull Player player) {
                if (player.getDisplayName() != null && !player.getDisplayName().isEmpty()) {
                    String currentName = prefs.getPlayerName();
                    if (currentName == null || currentName.isEmpty() || currentName.equals(AvatarManager.DEFAULT_PLAYER_NAME)) {
                        prefs.setPlayerName(player.getDisplayName());
                    }
                }
                runOnUiThread(() -> {
                    if (binding != null && !isNavigated) {
                        binding.tvLoadingHint.setText("Restoring cloud progress...");
                    }
                });

                CloudSaveManager.getInstance().loadAndSyncFromCloud(SplashActivity.this, (success, message) -> {
                    runOnUiThread(() -> onRestoreCompleted());
                });
            }

            @Override
            public void onFailure(Exception exception) {
                runOnUiThread(() -> onRestoreCompleted());
            }
        });
    }

    private void onRestoreCompleted() {
        isSyncFinished = true;
        checkProceedToHome();
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

                // Dynamic contextual loading hints (if not currently syncing cloud)
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
                    isAnimFinished = true;
                    checkProceedToHome();
                }
            });

            progressAnim.start();
        });
    }

    private void checkProceedToHome() {
        if (isAnimFinished && isSyncFinished && !isNavigated) {
            isNavigated = true;
            binding.getRoot().postDelayed(this::navigateToHome, 200);
        }
    }

    private void navigateToHome() {
        MainActivity.resetLaunchPromptState();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_AUTO_OPEN_LEVEL_PREVIEW, true);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
