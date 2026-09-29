package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.NonNull;

import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogConfirmExitBinding;

public class ExitConfirmDialog extends Dialog {

    private final Runnable onExitConfirmed;
    private final SoundManager soundManager;
    private DialogConfirmExitBinding binding;

    public ExitConfirmDialog(@NonNull Context context, Runnable onExitConfirmed) {
        super(context);
        this.onExitConfirmed = onExitConfirmed;
        this.soundManager = SoundManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogConfirmExitBinding.inflate(getLayoutInflater());
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

        // Bouncy entrance animation
        binding.rootExitConfirmDialog.setScaleX(0.85f);
        binding.rootExitConfirmDialog.setScaleY(0.85f);
        binding.rootExitConfirmDialog.setAlpha(0f);
        binding.rootExitConfirmDialog.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();

        attachButtonTouchFeedback(binding.btnStay);
        attachButtonTouchFeedback(binding.btnExit);
        attachButtonTouchFeedback(binding.btnCloseExit);

        // Stay / Cancel button
        binding.btnStay.setOnClickListener(v -> {
            soundManager.playClick();
            dismissWithAnimation();
        });

        // Close 'X' button
        binding.btnCloseExit.setOnClickListener(v -> {
            soundManager.playClick();
            dismissWithAnimation();
        });

        // Exit / Quit button
        binding.btnExit.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
            if (onExitConfirmed != null) {
                onExitConfirmed.run();
            }
        });
    }

    private void dismissWithAnimation() {
        if (binding == null) {
            dismiss();
            return;
        }
        binding.rootExitConfirmDialog.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .alpha(0f)
                .setDuration(160)
                .withEndAction(super::dismiss)
                .start();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void attachButtonTouchFeedback(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140)
                            .setInterpolator(new OvershootInterpolator(2.0f)).start();
                    break;
            }
            return false;
        });
    }
}
