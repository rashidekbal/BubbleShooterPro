package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogNoticeBinding;

/**
 * In-game notification dialog inspired by the LowShotsWarningDialog layout.
 * Features a 3D curved header plaque, gold inset card, smooth bouncy entrance,
 * tap-to-dismiss, and auto-dismiss after a brief delay.
 */
public class NoticeDialog extends Dialog {

    public enum Type {
        WARNING,
        INFO,
        REWARD
    }

    private static final long AUTO_HIDE_DELAY_MS = 2200L;

    private final Type type;
    private final String plaqueTitle;
    private final String tagText;
    private final String calloutText;
    private final String subtitleText;

    private final Handler autoDismissHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoDismissRunnable = this::dismissWithAnimation;
    private DialogNoticeBinding binding;
    private boolean isDismissing = false;

    public NoticeDialog(@NonNull Context context,
                        Type type,
                        String plaqueTitle,
                        String tagText,
                        String calloutText,
                        String subtitleText) {
        super(context);
        this.type = type;
        this.plaqueTitle = plaqueTitle;
        this.tagText = tagText;
        this.calloutText = calloutText;
        this.subtitleText = subtitleText;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogNoticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setCancelable(true);
        setCanceledOnTouchOutside(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        binding.tvNoticePlaqueTitle.setText(plaqueTitle);
        binding.tvNoticeTag.setText(tagText);
        binding.tvNoticeCallout.setText(calloutText);
        binding.tvNoticeSubtitle.setText(subtitleText);

        SoundManager soundManager = SoundManager.getInstance(getContext());
        switch (type) {
            case WARNING:
                binding.layoutNoticePlaque.setBackgroundResource(R.drawable.bg_dialog_header_warning_plaque);
                binding.tvNoticeTag.setTextColor(Color.parseColor("#DC2626"));
                binding.tvNoticeCallout.setTextColor(Color.parseColor("#D97706"));
                soundManager.playBounce();
                break;
            case REWARD:
                binding.layoutNoticePlaque.setBackgroundResource(R.drawable.bg_dialog_header_plaque);
                binding.tvNoticeTag.setTextColor(Color.parseColor("#059669"));
                binding.tvNoticeCallout.setTextColor(Color.parseColor("#D97706"));
                soundManager.playWin();
                break;
            case INFO:
            default:
                binding.layoutNoticePlaque.setBackgroundResource(R.drawable.bg_dialog_header_plaque);
                binding.tvNoticeTag.setTextColor(Color.parseColor("#2563EB"));
                binding.tvNoticeCallout.setTextColor(Color.parseColor("#1E3A8A"));
                soundManager.playBounce();
                break;
        }

        // Tap anywhere on dialog to dismiss immediately
        binding.getRoot().setOnClickListener(v -> dismissWithAnimation());

        // Smooth bouncy entrance animation
        binding.getRoot().setScaleX(0.72f);
        binding.getRoot().setScaleY(0.72f);
        binding.getRoot().setAlpha(0.0f);
        binding.getRoot().animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(1.25f))
                .start();

        autoDismissHandler.postDelayed(autoDismissRunnable, AUTO_HIDE_DELAY_MS);
    }

    public void dismissWithAnimation() {
        if (isDismissing) return;
        isDismissing = true;
        autoDismissHandler.removeCallbacks(autoDismissRunnable);

        if (binding != null && binding.getRoot() != null) {
            binding.getRoot().animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .alpha(0.0f)
                    .setDuration(160)
                    .withEndAction(NoticeDialog.super::dismiss)
                    .start();
        } else {
            super.dismiss();
        }
    }

    public static void showWarning(Context context, String plaqueTitle, String tag, String callout, String subtitle) {
        showInternal(context, Type.WARNING, plaqueTitle, tag, callout, subtitle);
    }

    public static void showInfo(Context context, String plaqueTitle, String tag, String callout, String subtitle) {
        showInternal(context, Type.INFO, plaqueTitle, tag, callout, subtitle);
    }

    public static void showReward(Context context, String plaqueTitle, String tag, String callout, String subtitle) {
        showInternal(context, Type.REWARD, plaqueTitle, tag, callout, subtitle);
    }

    private static void showInternal(Context context, Type type, String plaqueTitle, String tag, String callout, String subtitle) {
        if (context instanceof Activity) {
            Activity act = (Activity) context;
            if (act.isFinishing() || act.isDestroyed()) return;
        }
        new NoticeDialog(context, type, plaqueTitle, tag, callout, subtitle).show();
    }
}
