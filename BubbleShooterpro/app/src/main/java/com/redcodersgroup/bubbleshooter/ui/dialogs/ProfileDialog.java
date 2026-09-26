package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.databinding.DialogEditProfileBinding;
import com.redcodersgroup.bubbleshooter.profile.AvatarManager;
import com.redcodersgroup.bubbleshooter.ui.adapter.AvatarAdapter;

public class ProfileDialog extends Dialog {

    public interface ProfileDialogListener {
        void onProfileUpdated(String playerName, String avatarId);
    }

    private final android.app.Activity hostActivity;
    private final PreferencesManager prefs;
    private final ProfileDialogListener listener;
    private final SoundManager soundManager;
    private String selectedAvatarId;
    private DialogEditProfileBinding binding;

    public ProfileDialog(@NonNull Context context, ProfileDialogListener listener) {
        super(context);
        this.hostActivity = getActivityFromContext(context);
        this.prefs = new PreferencesManager(context);
        this.listener = listener;
        this.soundManager = SoundManager.getInstance(context);
        this.selectedAvatarId = prefs.getPlayerAvatar();
    }

    public ProfileDialog(@NonNull Context context, String currentName, String currentAvatarId, ProfileDialogListener listener) {
        super(context);
        this.hostActivity = getActivityFromContext(context);
        this.prefs = new PreferencesManager(context);
        this.listener = listener;
        this.soundManager = SoundManager.getInstance(context);
        this.selectedAvatarId = (currentAvatarId != null && !currentAvatarId.isEmpty())
                ? currentAvatarId : prefs.getPlayerAvatar();
        if (currentName != null && !currentName.isEmpty()) {
            prefs.setPlayerName(currentName);
        }
    }

    public ProfileDialog(@NonNull android.app.Activity activity, ProfileDialogListener listener) {
        super(activity);
        this.hostActivity = activity;
        this.prefs = new PreferencesManager(activity);
        this.listener = listener;
        this.soundManager = SoundManager.getInstance(activity);
        this.selectedAvatarId = prefs.getPlayerAvatar();
    }

    public ProfileDialog(@NonNull android.app.Activity activity, String currentName, String currentAvatarId, ProfileDialogListener listener) {
        super(activity);
        this.hostActivity = activity;
        this.prefs = new PreferencesManager(activity);
        this.listener = listener;
        this.soundManager = SoundManager.getInstance(activity);
        this.selectedAvatarId = (currentAvatarId != null && !currentAvatarId.isEmpty())
                ? currentAvatarId : prefs.getPlayerAvatar();
        if (currentName != null && !currentName.isEmpty()) {
            prefs.setPlayerName(currentName);
        }
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
        binding = DialogEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Initialize Player Name
        String currentName = prefs.getPlayerName();
        if (currentName == null || currentName.trim().isEmpty()) {
            currentName = AvatarManager.DEFAULT_PLAYER_NAME;
        }
        binding.etPlayerName.setText(currentName);
        if (binding.etPlayerName.getText() != null) {
            binding.etPlayerName.setSelection(binding.etPlayerName.getText().length());
        }

        // Initialize Avatar Preview
        updateAvatarPreview(selectedAvatarId);

        // Setup 4-column Grid for 8 Predefined Avatars
        binding.rvAvatarChoices.setLayoutManager(new GridLayoutManager(getContext(), 4));
        AvatarAdapter adapter = new AvatarAdapter(
                AvatarManager.getPredefinedAvatars(),
                selectedAvatarId,
                avatar -> {
                    soundManager.playClick();
                    selectedAvatarId = avatar.id;
                    updateAvatarPreview(avatar.id);
                }
        );
        binding.rvAvatarChoices.setAdapter(adapter);

        // Confirm Button
        binding.btnProfileConfirm.setOnClickListener(v -> {
            soundManager.playClick();
            String name = binding.etPlayerName.getText().toString().trim();
            if (name.isEmpty()) {
                name = AvatarManager.DEFAULT_PLAYER_NAME;
            }

            prefs.setPlayerName(name);
            prefs.setPlayerAvatar(selectedAvatarId);

            if (listener != null) {
                listener.onProfileUpdated(name, selectedAvatarId);
            }
            dismiss();
        });

        // Sync Play Games Name
        binding.tvSyncPlayGames.setOnClickListener(v -> {
            soundManager.playClick();
            com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager auth =
                    com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance();

            if (auth.isAuthenticated() && auth.getDisplayName() != null) {
                binding.etPlayerName.setText(auth.getDisplayName());
                if (binding.etPlayerName.getText() != null) {
                    binding.etPlayerName.setSelection(binding.etPlayerName.getText().length());
                }
                binding.tvSyncPlayGames.setText("Synced with Google ✓");
                binding.tvSyncPlayGames.setTextColor(android.graphics.Color.parseColor("#059669"));
            } else if (hostActivity != null) {
                binding.tvSyncPlayGames.setText("Signing in...");
                auth.signIn(hostActivity, new com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull com.google.android.gms.games.Player player) {
                        if (binding != null && player.getDisplayName() != null) {
                            binding.etPlayerName.setText(player.getDisplayName());
                            if (binding.etPlayerName.getText() != null) {
                                binding.etPlayerName.setSelection(binding.etPlayerName.getText().length());
                            }
                            binding.tvSyncPlayGames.setText("Synced with Google ✓");
                            binding.tvSyncPlayGames.setTextColor(android.graphics.Color.parseColor("#059669"));
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        if (binding != null) {
                            binding.tvSyncPlayGames.setText("Sign in failed. Tap to retry");
                            binding.tvSyncPlayGames.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                        }
                    }
                });
            }
        });

        // Close Button
        binding.btnCloseProfile.setOnClickListener(v -> {
            soundManager.playClick();
            dismiss();
        });
    }

    public void onAuthSuccess(String displayName) {
        if (binding != null && displayName != null && !displayName.isEmpty()) {
            binding.etPlayerName.setText(displayName);
            if (binding.etPlayerName.getText() != null) {
                binding.etPlayerName.setSelection(binding.etPlayerName.getText().length());
            }
            binding.tvSyncPlayGames.setText("Synced with Google ✓");
            binding.tvSyncPlayGames.setTextColor(android.graphics.Color.parseColor("#059669"));
        }
    }

    public void onAuthFailure(Exception exception) {
        if (binding != null) {
            binding.tvSyncPlayGames.setText("Sign in failed. Tap to retry");
            binding.tvSyncPlayGames.setTextColor(android.graphics.Color.parseColor("#EF4444"));
        }
    }

    private void updateAvatarPreview(String avatarId) {
        if (binding != null && binding.ivSelectedAvatarPreview != null) {
            binding.ivSelectedAvatarPreview.setImageResource(AvatarManager.getAvatarDrawable(avatarId));
        }
    }
}
