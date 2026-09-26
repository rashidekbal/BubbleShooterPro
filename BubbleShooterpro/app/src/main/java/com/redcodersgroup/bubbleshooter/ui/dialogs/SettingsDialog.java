package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.audio.MusicManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogSettingsBinding;

public class SettingsDialog extends Dialog {

    private final android.app.Activity hostActivity;
    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private DialogSettingsBinding binding;

    public SettingsDialog(@NonNull Context context) {
        super(context);
        this.hostActivity = getActivityFromContext(context);
        this.prefs = new PreferencesManager(context);
        this.soundManager = SoundManager.getInstance(context);
    }

    public SettingsDialog(@NonNull android.app.Activity activity) {
        super(activity);
        this.hostActivity = activity;
        this.prefs = new PreferencesManager(activity);
        this.soundManager = SoundManager.getInstance(activity);
    }

    @androidx.annotation.Nullable
    private static android.app.Activity getActivityFromContext(Context context) {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof android.app.Activity) {
                return (android.app.Activity) context;
            }
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        updateSoundUI();
        updateMusicUI();
        updateHapticUI();
        updatePlayGamesUI();

        binding.layoutSoundToggle.setOnClickListener(v -> toggleSound());
        binding.btnSettingSound.setOnClickListener(v -> toggleSound());

        binding.layoutMusicToggle.setOnClickListener(v -> toggleMusic());
        binding.btnSettingMusic.setOnClickListener(v -> toggleMusic());

        binding.layoutHapticToggle.setOnClickListener(v -> toggleHaptics());
        binding.btnSettingHaptic.setOnClickListener(v -> toggleHaptics());

        binding.btnPlayGamesAuth.setOnClickListener(v -> handlePlayGamesClick());
        binding.tvPlayGamesAction.setOnClickListener(v -> handlePlayGamesClick());
        binding.tvPlayGamesStatus.setOnClickListener(v -> handlePlayGamesClick());
        binding.tvPlayGamesTitle.setOnClickListener(v -> handlePlayGamesClick());

        binding.btnCloseSettings.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });
    }

    private void handlePlayGamesClick() {
        soundManager.playClick();
        if (hostActivity != null) {
            com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager authManager =
                    com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance();

            binding.tvPlayGamesStatus.setText("Signing in...");
            authManager.signIn(hostActivity, new com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback() {
                @Override
                public void onSuccess(@NonNull com.google.android.gms.games.Player player) {
                    if (binding != null) {
                        updatePlayGamesUI();
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    if (binding != null) {
                        binding.tvPlayGamesStatus.setText("Sign in failed. Tap to retry");
                        binding.tvPlayGamesAction.setText("Sign In");
                        binding.tvPlayGamesAction.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                    }
                }
            });
        }
    }

    public void onAuthSuccess() {
        if (binding != null) {
            updatePlayGamesUI();
        }
    }

    public void onAuthFailure(Exception exception) {
        if (binding != null) {
            binding.tvPlayGamesStatus.setText("Sign in failed. Tap to retry");
            binding.tvPlayGamesAction.setText("Sign In");
            binding.tvPlayGamesAction.setTextColor(android.graphics.Color.parseColor("#EF4444"));
        }
    }

    private void updatePlayGamesUI() {
        if (binding == null) return;
        com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager authManager =
                com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance();

        if (authManager.isAuthenticated()) {
            String name = authManager.getDisplayName() != null ? authManager.getDisplayName() : "Connected";
            binding.tvPlayGamesTitle.setText("Google Account");
            binding.tvPlayGamesStatus.setText(name);
            binding.tvPlayGamesStatus.setTextColor(android.graphics.Color.parseColor("#059669"));
            binding.tvPlayGamesAction.setText("Connected");
            binding.tvPlayGamesAction.setTextColor(android.graphics.Color.parseColor("#059669"));
        } else {
            binding.tvPlayGamesTitle.setText("Google Account");
            binding.tvPlayGamesStatus.setText("Tap to connect account");
            binding.tvPlayGamesStatus.setTextColor(android.graphics.Color.parseColor("#64748B"));
            binding.tvPlayGamesAction.setText("Sign In");
            binding.tvPlayGamesAction.setTextColor(android.graphics.Color.parseColor("#2563EB"));
        }
    }

    private void toggleSound() {
        boolean current = prefs.isSoundEnabled();
        prefs.setSoundEnabled(!current);
        soundManager.playClick();
        updateSoundUI();
    }

    private void toggleMusic() {
        boolean current = prefs.isMusicEnabled();
        boolean newVal = !current;
        prefs.setMusicEnabled(newVal);
        MusicManager.getInstance(getContext()).setMusicEnabled(newVal);
        soundManager.playClick();
        updateMusicUI();
    }

    private void toggleHaptics() {
        boolean current = prefs.isHapticEnabled();
        prefs.setHapticEnabled(!current);
        soundManager.playClick();
        updateHapticUI();
    }

    private void updateSoundUI() {
        if (prefs.isSoundEnabled()) {
            binding.btnSettingSound.setImageResource(R.drawable.btn_sound_green);
            binding.btnSettingSound.setAlpha(1.0f);
        } else {
            binding.btnSettingSound.setImageResource(R.drawable.btn_sound_gray);
            binding.btnSettingSound.setAlpha(0.65f);
        }
    }

    private void updateMusicUI() {
        if (prefs.isMusicEnabled()) {
            binding.btnSettingMusic.setImageResource(R.drawable.btn_music_green);
            binding.btnSettingMusic.setAlpha(1.0f);
        } else {
            binding.btnSettingMusic.setImageResource(R.drawable.btn_music_gray);
            binding.btnSettingMusic.setAlpha(0.65f);
        }
    }

    private void updateHapticUI() {
        if (prefs.isHapticEnabled()) {
            binding.btnSettingHaptic.setImageResource(R.drawable.btn_vibration_yellow);
            binding.btnSettingHaptic.setAlpha(1.0f);
        } else {
            binding.btnSettingHaptic.setImageResource(R.drawable.btn_vibration_gray);
            binding.btnSettingHaptic.setAlpha(0.65f);
        }
    }
}
