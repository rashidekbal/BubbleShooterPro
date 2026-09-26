package com.redcodersgroup.bubbleshooter;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.redcodersgroup.bubbleshooter.ui.dialogs.NoticeDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.data.ProgressRepository;
import com.redcodersgroup.bubbleshooter.data.WorldConfigManager;
import com.redcodersgroup.bubbleshooter.data.WorldConfigManager.WorldModel;
import com.redcodersgroup.bubbleshooter.databinding.ActivityMainBinding;
import com.redcodersgroup.bubbleshooter.databinding.DialogLevelPreviewBinding;
import com.redcodersgroup.bubbleshooter.level.Level;
import com.redcodersgroup.bubbleshooter.level.LevelManager;
import com.redcodersgroup.bubbleshooter.ui.GameActivity;
import com.redcodersgroup.bubbleshooter.ui.adapter.WorldMapPagerAdapter;
import com.redcodersgroup.bubbleshooter.profile.AvatarManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.ProfileDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.SettingsDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.StarChestDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.StoreDialog;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.HeartStoreDialog;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProgressRepository repository;
    private PreferencesManager prefs;
    private LevelManager levelManager;
    private SoundManager soundManager;
    private WorldConfigManager worldConfigManager;
    private WorldMapPagerAdapter worldMapAdapter;

    private SettingsDialog settingsDialog;
    private ProfileDialog profileDialog;
    private StarChestDialog starChestDialog;
    private StoreDialog storeDialog;
    private HeartStoreDialog heartStoreDialog;
    private Dialog activePreviewDialog;

    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> googleSignInLauncher;
    private com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback activeAuthCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = ProgressRepository.getInstance(this);
        prefs = repository.getPreferences();
        levelManager = LevelManager.getInstance(this);
        soundManager = SoundManager.getInstance(this);
        worldConfigManager = WorldConfigManager.getInstance(this);

        setupAuthLauncher();
        initViews();
        checkPlayGamesSignIn();
    }

    private void setupAuthLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    android.content.Intent data = result.getData();
                    com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance().handleSignInResult(data,
                            new com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback() {
                                @Override
                                public void onSuccess(@androidx.annotation.NonNull com.google.android.gms.games.Player player) {
                                    String currentName = prefs.getPlayerName();
                                    if (player.getDisplayName() != null && !player.getDisplayName().isEmpty()) {
                                        if (currentName == null || currentName.isEmpty() || currentName.equals(com.redcodersgroup.bubbleshooter.profile.AvatarManager.DEFAULT_PLAYER_NAME)) {
                                            prefs.setPlayerName(player.getDisplayName());
                                            updateProfileUI();
                                        }
                                    }
                                    if (settingsDialog != null && settingsDialog.isShowing()) {
                                        settingsDialog.onAuthSuccess();
                                    }
                                    if (profileDialog != null && profileDialog.isShowing()) {
                                        profileDialog.onAuthSuccess(player.getDisplayName());
                                    }
                                    if (activeAuthCallback != null) {
                                        activeAuthCallback.onSuccess(player);
                                        activeAuthCallback = null;
                                    }
                                }

                                @Override
                                public void onFailure(Exception exception) {
                                    if (settingsDialog != null && settingsDialog.isShowing()) {
                                        settingsDialog.onAuthFailure(exception);
                                    }
                                    if (profileDialog != null && profileDialog.isShowing()) {
                                        profileDialog.onAuthFailure(exception);
                                    }
                                    if (activeAuthCallback != null) {
                                        activeAuthCallback.onFailure(exception);
                                        activeAuthCallback = null;
                                    }
                                }
                            });
                }
        );
    }

    private void initViews() {
        // 1. Settings Dialog
        binding.btnMainSettings.setOnClickListener(v -> {
            soundManager.playClick();
            if (settingsDialog != null && settingsDialog.isShowing()) {
                settingsDialog.dismiss();
            }
            settingsDialog = new SettingsDialog(this);
            settingsDialog.show();
        });

        // 2. Profile Avatar Dialog
        binding.layoutPlayerProfile.setOnClickListener(v -> {
            soundManager.playClick();
            if (profileDialog != null && profileDialog.isShowing()) {
                profileDialog.dismiss();
            }
            profileDialog = new ProfileDialog(this, (name, avatarId) -> {
                updateProfileUI();
                if (worldMapAdapter != null) {
                    worldMapAdapter.notifyDataSetChanged();
                }
            });
            profileDialog.show();
        });

        // 3. Star Chest Dialog
        binding.layoutStarChestBadge.setOnClickListener(v -> {
            soundManager.playClick();
            if (starChestDialog != null && starChestDialog.isShowing()) {
                starChestDialog.dismiss();
            }
            int totalStars = repository.getTotalStarsEarned(levelManager.getTotalLevels());
            int chestTarget = 20;
            starChestDialog = new StarChestDialog(this, totalStars % chestTarget, chestTarget);
            starChestDialog.show();
        });

        // 4. Endless Mode Floating Button
        binding.btnHomeEndlessMode.setOnClickListener(v -> {
            soundManager.playClick();
            startActivity(GameActivity.createEndlessIntent(this));
        });

        // 5. Floating Play Button on Bottom Right
        binding.btnFloatingPlay.setOnClickListener(v -> {
            soundManager.playClick();
            int currentLevel = prefs.getHighestUnlockedLevel();
            showLevelPreviewDialog(currentLevel);
        });

        // 6. Diamonds & Lives Clickables
        binding.layoutDiamondsCounter.setOnClickListener(v -> {
            soundManager.playClick();
            showStoreDialog();
        });

        binding.layoutLivesCounter.setOnClickListener(v -> {
            soundManager.playClick();
            showHeartStoreDialog();
        });

        // 7. World Map ViewPager2 setup
        worldMapAdapter = new WorldMapPagerAdapter(this, new WorldMapPagerAdapter.OnWorldInteractionListener() {
            @Override
            public void onLevelSelected(int levelNumber) {
                showLevelPreviewDialog(levelNumber);
            }

            @Override
            public void onWorldGiftClaimed(int worldNumber, int giftIndex, int bonusDiamonds) {
                soundManager.playWin();
                prefs.addDiamonds(bonusDiamonds);
                AnalyticsManager.getInstance(MainActivity.this).logGiftChestClaimed(worldNumber, giftIndex, bonusDiamonds);
                updateDiamondsUI();
                NoticeDialog.showReward(
                        MainActivity.this,
                        "REWARD",
                        "MYSTERY GIFT UNLOCKED",
                        "+" + bonusDiamonds + " DIAMONDS",
                        "Bonus diamonds added to your vault."
                );
            }
        });
        binding.viewPagerWorldMaps.setAdapter(worldMapAdapter);
        // Start at World 1 (which is the LAST page in reversed order)
        int initialPagerPos = worldMapAdapter.toPagerPosition(0);
        binding.viewPagerWorldMaps.setCurrentItem(initialPagerPos, false);

        binding.viewPagerWorldMaps.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Convert reversed pager position back to world index for UI
                int worldIndex = worldMapAdapter.toWorldIndex(position);
                updateWorldSwitcherUI(worldIndex);
            }
        });

        // Initial world title update
        updateWorldSwitcherUI(0);
        updateProfileUI();
        updateDiamondsUI();
        updateLivesUI();
    }

    private void showStoreDialog() {
        if (isFinishing() || isDestroyed()) return;
        AnalyticsManager.getInstance(this).logStoreOpened("diamond");
        startActivity(com.redcodersgroup.bubbleshooter.ui.ShopActivity.createIntent(this, com.redcodersgroup.bubbleshooter.ui.ShopActivity.TAB_DIAMONDS));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showHeartStoreDialog() {
        if (isFinishing() || isDestroyed()) return;
        AnalyticsManager.getInstance(this).logStoreOpened("heart");
        startActivity(com.redcodersgroup.bubbleshooter.ui.ShopActivity.createIntent(this, com.redcodersgroup.bubbleshooter.ui.ShopActivity.TAB_HEARTS));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void updateDiamondsUI() {
        if (binding != null && binding.tvHomeDiamonds != null) {
            binding.tvHomeDiamonds.setText(String.format(java.util.Locale.getDefault(), "%,d", prefs.getDiamonds()));
        }
    }

    private void updateLivesUI() {
        if (binding != null && binding.tvHomeLives != null) {
            int lives = prefs.getLives();
            binding.tvHomeLives.setText(lives >= 5 ? "5 FULL" : lives + "/5");
        }
    }

    private void updateProfileUI() {
        if (binding != null && binding.ivHomeAvatar != null) {
            String avatarId = prefs.getPlayerAvatar();
            binding.ivHomeAvatar.setImageResource(AvatarManager.getAvatarDrawable(avatarId));
        }
    }

    private void updateWorldSwitcherUI(int worldIndex) {
        WorldModel world = worldConfigManager.getWorldByIndex(worldIndex);
        if (world != null && binding.tvCurrentWorldTitle != null) {
            binding.tvCurrentWorldTitle.setText(world.subtitle);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileUI();
        updateDiamondsUI();
        updateLivesUI();
        int currentLevel = prefs.getHighestUnlockedLevel();
        int totalStars = repository.getTotalStarsEarned(levelManager.getTotalLevels());

        binding.tvFloatingLevelNumber.setText(String.valueOf(currentLevel));
        binding.tvStarChestProgress.setText((totalStars % 20) + "/20");

        int targetWorldIndex = worldConfigManager.getWorldIndexForLevel(currentLevel);
        int pagerPos = worldMapAdapter.toPagerPosition(targetWorldIndex);
        android.util.Log.d("MainActivity", "onResume: highestUnlockedLevel=" + currentLevel + " -> worldIndex=" + targetWorldIndex + " -> pagerPos=" + pagerPos);
        binding.viewPagerWorldMaps.setCurrentItem(pagerPos, false);
        updateWorldSwitcherUI(targetWorldIndex);

        if (worldMapAdapter != null) {
            worldMapAdapter.notifyDataSetChanged();
        }

        int endlessHigh = prefs.getEndlessHighScore();
        if (endlessHigh > 0) {
            binding.tvEndlessBestTag.setText("🏆 Best: " + String.format(java.util.Locale.getDefault(), "%,d", endlessHigh));
        } else {
            binding.tvEndlessBestTag.setText("🏆 Best: 0");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (settingsDialog != null && settingsDialog.isShowing()) settingsDialog.dismiss();
        if (profileDialog != null && profileDialog.isShowing()) profileDialog.dismiss();
        if (starChestDialog != null && starChestDialog.isShowing()) starChestDialog.dismiss();
        if (storeDialog != null && storeDialog.isShowing()) storeDialog.dismiss();
        if (heartStoreDialog != null && heartStoreDialog.isShowing()) heartStoreDialog.dismiss();
        if (activePreviewDialog != null && activePreviewDialog.isShowing()) activePreviewDialog.dismiss();
    }

    private void showLevelPreviewDialog(int level) {
        if (isFinishing() || isDestroyed()) return;
        if (activePreviewDialog != null && activePreviewDialog.isShowing()) {
            activePreviewDialog.dismiss();
        }

        soundManager.playClick();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DialogLevelPreviewBinding previewBinding = DialogLevelPreviewBinding.inflate(getLayoutInflater());
        dialog.setContentView(previewBinding.getRoot());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        WorldModel world = worldConfigManager.getWorldForLevel(level);
        previewBinding.tvPreviewWorld.setText(world.subtitle);
        previewBinding.tvPreviewLevel.setText("LEVEL " + level);

        int stars = prefs.getStarsForLevel(level);
        previewBinding.ivPreviewStar1.setImageResource(stars >= 1 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        previewBinding.ivPreviewStar2.setImageResource(stars >= 2 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        previewBinding.ivPreviewStar3.setImageResource(stars >= 3 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);



        previewBinding.btnToggleBoosterRainbow.setOnClickListener(v -> soundManager.playClick());
        previewBinding.btnToggleBoosterFireball.setOnClickListener(v -> soundManager.playClick());
        previewBinding.btnToggleBoosterLightning.setOnClickListener(v -> soundManager.playClick());
        previewBinding.btnToggleBoosterBomb.setOnClickListener(v -> soundManager.playClick());

        previewBinding.btnStartLevel.setOnClickListener(v -> {
            soundManager.playClick();
            if (prefs.getLives() <= 0) {
                showHeartStoreDialog();
                return;
            }
            dialog.dismiss();
            activePreviewDialog = null;
            startActivity(GameActivity.createIntent(MainActivity.this, level));
        });

        previewBinding.btnClosePreview.setOnClickListener(v -> {
            soundManager.playClick();
            dialog.dismiss();
            activePreviewDialog = null;
        });

        activePreviewDialog = dialog;
        dialog.show();
    }

    public void startGoogleSignIn(com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback callback) {
        this.activeAuthCallback = callback;
        // 1. Try modern Google Play Games v2 sign-in first
        com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance().signInWithPlayGames(this,
                new com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull com.google.android.gms.games.Player player) {
                        String currentName = prefs.getPlayerName();
                        if (player.getDisplayName() != null && !player.getDisplayName().isEmpty()) {
                            if (currentName == null || currentName.isEmpty() || currentName.equals(com.redcodersgroup.bubbleshooter.profile.AvatarManager.DEFAULT_PLAYER_NAME)) {
                                prefs.setPlayerName(player.getDisplayName());
                                updateProfileUI();
                            }
                        }
                        if (settingsDialog != null && settingsDialog.isShowing()) {
                            settingsDialog.onAuthSuccess();
                        }
                        if (profileDialog != null && profileDialog.isShowing()) {
                            profileDialog.onAuthSuccess(player.getDisplayName());
                        }
                        if (callback != null) {
                            callback.onSuccess(player);
                        }
                        activeAuthCallback = null;
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // 2. If Play Games is unlinked or returns an error, launch the modern Google Sign-In account chooser
                        try {
                            android.content.Intent signInIntent =
                                    com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance().getGoogleSignInIntent(MainActivity.this);
                            if (googleSignInLauncher != null) {
                                googleSignInLauncher.launch(signInIntent);
                            }
                        } catch (Exception e) {
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                            activeAuthCallback = null;
                        }
                    }
                });
    }

    private void checkPlayGamesSignIn() {
        com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.getInstance().checkSilentSignIn(this,
                new com.redcodersgroup.bubbleshooter.auth.PlayGamesAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull com.google.android.gms.games.Player player) {
                        String currentName = prefs.getPlayerName();
                        if (currentName == null || currentName.isEmpty() || currentName.equals(com.redcodersgroup.bubbleshooter.profile.AvatarManager.DEFAULT_PLAYER_NAME)) {
                            if (player.getDisplayName() != null && !player.getDisplayName().isEmpty()) {
                                prefs.setPlayerName(player.getDisplayName());
                                updateProfileUI();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // Silent sign-in not available or cancelled
                    }
                });
    }
}