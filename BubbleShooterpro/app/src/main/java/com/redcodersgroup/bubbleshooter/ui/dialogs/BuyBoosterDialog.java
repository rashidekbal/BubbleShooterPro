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
import com.redcodersgroup.bubbleshooter.databinding.DialogBuyBoosterBinding;

public class BuyBoosterDialog extends Dialog {

    public interface BuyBoosterListener {
        void onGoToShop(BubbleType type);
    }

    private final BubbleType boosterType;
    @Nullable
    private final BuyBoosterListener listener;
    private DialogBuyBoosterBinding binding;
    private boolean isDismissing = false;

    public BuyBoosterDialog(@NonNull Context context,
                            BubbleType boosterType,
                            @Nullable BuyBoosterListener listener) {
        super(context);
        this.boosterType = boosterType;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogBuyBoosterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setCancelable(true);
        setCanceledOnTouchOutside(true);

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

        binding.btnCloseDialog.setOnClickListener(v -> {
            soundManager.playClick();
            dismissWithAnimation(null);
        });

        binding.btnCancel.setOnClickListener(v -> {
            soundManager.playClick();
            dismissWithAnimation(null);
        });

        binding.btnBuyInShop.setOnClickListener(v -> {
            soundManager.playClick();
            dismissWithAnimation(() -> {
                if (listener != null) {
                    listener.onGoToShop(boosterType);
                }
            });
        });

        startEntranceAnimation();
    }

    private void configureBoosterDetails() {
        if (boosterType == null) return;

        switch (boosterType) {
            case BOMB:
                binding.ivBoosterIcon.setImageResource(R.drawable.bomb_bubble);
                binding.tvBoosterName.setText("OUT OF BOMBS");
                binding.tvBoosterDescription.setText(
                        "You've used all your Bomb power-ups! Blast 3x3 bubble clusters away by grabbing more in the Shop."
                );
                break;

            case RAINBOW:
                binding.ivBoosterIcon.setImageResource(R.drawable.rainbow_bubble);
                binding.tvBoosterName.setText("OUT OF RAINBOWS");
                binding.tvBoosterDescription.setText(
                        "You've used all your Rainbow power-ups! Match and clear any color cluster by getting more in the Shop."
                );
                break;

            case FIREBALL:
                binding.ivBoosterIcon.setImageResource(R.drawable.fireball_bubble);
                binding.tvBoosterName.setText("OUT OF FIREBALLS");
                binding.tvBoosterDescription.setText(
                        "You've used all your Fireball power-ups! Incinerate whole vertical columns by restocking in the Shop."
                );
                break;

            case LIGHTNING:
                binding.ivBoosterIcon.setImageResource(R.drawable.lightning_bubble);
                binding.tvBoosterName.setText("OUT OF LIGHTNING");
                binding.tvBoosterDescription.setText(
                        "You've used all your Lightning power-ups! Zap entire horizontal rows away with a quick refill from the Shop."
                );
                break;

            default:
                binding.tvBoosterName.setText("OUT OF POWER-UPS");
                binding.tvBoosterDescription.setText(
                        "You've used all of this power-up! Visit the Shop to restock and keep your winning streak going."
                );
                break;
        }
    }

    private void startEntranceAnimation() {
        binding.rootBuyBoosterDialog.setAlpha(0f);
        binding.rootBuyBoosterDialog.setScaleX(0.70f);
        binding.rootBuyBoosterDialog.setScaleY(0.70f);

        binding.rootBuyBoosterDialog.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
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

    public void dismissWithAnimation(@Nullable Runnable onComplete) {
        if (isDismissing) return;
        isDismissing = true;

        if (binding != null) {
            binding.rootBuyBoosterDialog.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        dismiss();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .start();
        } else {
            dismiss();
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public static BuyBoosterDialog show(Activity activity, BubbleType boosterType, @Nullable BuyBoosterListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return null;
        BuyBoosterDialog dialog = new BuyBoosterDialog(activity, boosterType, listener);
        dialog.show();
        return dialog;
    }
}
