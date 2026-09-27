package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import com.redcodersgroup.bubbleshooter.databinding.DialogBoosterIntroBinding;

public class BoosterIntroDialog extends Dialog {

    private final BubbleType boosterType;
    private final String worldName;
    @Nullable
    private final Runnable onDismissCallback;
    private DialogBoosterIntroBinding binding;
    private boolean isDismissing = false;

    public BoosterIntroDialog(@NonNull Context context,
                              BubbleType boosterType,
                              String worldName,
                              @Nullable Runnable onDismissCallback) {
        super(context);
        this.boosterType = boosterType;
        this.worldName = worldName;
        this.onDismissCallback = onDismissCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogBoosterIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        configureBoosterDetails();

        SoundManager soundManager = SoundManager.getInstance(getContext());
        soundManager.playBounce();

        binding.btnLetsPlay.setOnClickListener(v -> {
            soundManager.playClick();
            dismissWithAnimation();
        });

        startEntranceAnimation();
    }

    private void configureBoosterDetails() {
        if (worldName != null && !worldName.isEmpty()) {
            binding.tvWorldTag.setText(worldName.toUpperCase(java.util.Locale.ROOT));
        }

        switch (boosterType) {
            case BOMB:
                binding.tvBoosterPlaqueTitle.setText("NEW BOOSTER!");
                binding.ivBoosterIcon.setImageResource(R.drawable.bomb_bubble);
                binding.tvBoosterName.setText("BOMB BUBBLE");
                binding.tvBoosterDescription.setText(
                        "Tap the bomb to equip it! On impact, it obliterates all surrounding bubbles in a massive 3x3 blast radius."
                );
                binding.tvGiftText.setText("🎁 GIFT: +5 Free Bombs Added!");
                break;

            case RAINBOW:
                binding.tvBoosterPlaqueTitle.setText("NEW BOOSTER!");
                binding.ivBoosterIcon.setImageResource(R.drawable.rainbow_bubble);
                binding.tvBoosterName.setText("RAINBOW BUBBLE");
                binding.tvBoosterDescription.setText(
                        "The Rainbow Bubble is a magical wildcard! It matches and clears ANY color cluster it touches."
                );
                binding.tvGiftText.setText("🎁 GIFT: +5 Free Rainbows Added!");
                break;

            case FIREBALL:
                binding.tvBoosterPlaqueTitle.setText("NEW BOOSTER!");
                binding.ivBoosterIcon.setImageResource(R.drawable.fireball_bubble);
                binding.tvBoosterName.setText("FIREBALL BUBBLE");
                binding.tvBoosterDescription.setText(
                        "Blazes straight through all bubbles in its trajectory without stopping, clearing full paths!"
                );
                binding.tvGiftText.setText("🎁 GIFT: +5 Free Fireballs Added!");
                break;

            case LIGHTNING:
                binding.tvBoosterPlaqueTitle.setText("NEW BOOSTER!");
                binding.ivBoosterIcon.setImageResource(R.drawable.lightning_bubble);
                binding.tvBoosterName.setText("LIGHTNING BUBBLE");
                binding.tvBoosterDescription.setText(
                        "Strikes horizontally with lightning, zapping away the entire row of bubbles instantly!"
                );
                binding.tvGiftText.setText("🎁 GIFT: +5 Free Lightning Added!");
                break;

            default:
                break;
        }
    }

    private void startEntranceAnimation() {
        binding.rootBoosterIntroDialog.setAlpha(0f);
        binding.rootBoosterIntroDialog.setScaleX(0.70f);
        binding.rootBoosterIntroDialog.setScaleY(0.70f);

        binding.rootBoosterIntroDialog.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(320)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .withEndAction(this::startPulseAnimation)
                .start();
    }

    private void startPulseAnimation() {
        if (isDismissing || binding == null) return;
        binding.ivBoosterIcon.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(700)
                .withEndAction(() -> {
                    if (isDismissing || binding == null) return;
                    binding.ivBoosterIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(700)
                            .withEndAction(this::startPulseAnimation)
                            .start();
                })
                .start();
    }

    public void dismissWithAnimation() {
        if (isDismissing) return;
        isDismissing = true;

        if (binding != null) {
            binding.rootBoosterIntroDialog.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        dismiss();
                        if (onDismissCallback != null) {
                            onDismissCallback.run();
                        }
                    })
                    .start();
        } else {
            dismiss();
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        }
    }

    public static void show(Activity activity, BubbleType boosterType, String worldName, @Nullable Runnable onDismiss) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        BoosterIntroDialog dialog = new BoosterIntroDialog(activity, boosterType, worldName, onDismiss);
        dialog.show();
    }
}
