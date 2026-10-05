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
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import com.redcodersgroup.bubbleshooter.level.Level;
import com.redcodersgroup.bubbleshooter.level.LevelManager;
import com.redcodersgroup.bubbleshooter.ui.GameActivity;
import com.redcodersgroup.bubbleshooter.ui.adapter.WorldMapPagerAdapter;
import com.redcodersgroup.bubbleshooter.profile.AvatarManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.ProfileDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.SettingsDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.StarChestDialog;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.ClaimGiftDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.HeartStoreDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.OutOfHeartsDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.ExitConfirmDialog;
import androidx.activity.OnBackPressedCallback;

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
    private ClaimGiftDialog activeClaimGiftDialog;
    private ObjectAnimator starChestBadgePulseAnimator;
    private HeartStoreDialog heartStoreDialog;
    private Dialog activePreviewDialog;
    private ExitConfirmDialog activeExitDialog;

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });
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
                                    // Sync levels, diamonds, boosters from Google Play Games Cloud
                                    com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().loadAndSyncFromCloud(MainActivity.this, (success, msg) -> {
                                        runOnUiThread(() -> refreshAllUI());
                                    });
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
            int claimedCount = prefs.getClaimedStarChestsCount();
            starChestDialog = new StarChestDialog(this, totalStars, claimedCount, diamondsEarned -> {
                updateDiamondsUI();
                updateStarChestProgressUI();
            });
            starChestDialog.setOnDismissListener(dialog -> {
                updateDiamondsUI();
                updateStarChestProgressUI();
            });
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
            if (prefs.getLives() <= 0) {
                showOutOfHeartsDialog(() -> {
                    int highest = prefs.getHighestUnlockedLevel();
                    showLevelPreviewDialog(highest);
                });
            } else {
                showHeartStoreDialog();
            }
        });

        // 7. World Map ViewPager2 setup
        worldMapAdapter = new WorldMapPagerAdapter(this, new WorldMapPagerAdapter.OnWorldInteractionListener() {
            @Override
            public void onLevelSelected(int levelNumber) {
                showLevelPreviewDialog(levelNumber);
            }

            @Override
            public void onWorldGiftClaimed(int worldNumber, int giftIndex, int bonusDiamonds) {
                updateDiamondsUI();
                if (worldMapAdapter != null) {
                    worldMapAdapter.notifyDataSetChanged();
                }
            }
        });
        binding.viewPagerWorldMaps.setAdapter(worldMapAdapter);
        // Start at player's current world
        int startLevel = prefs.getHighestUnlockedLevel();
        int initialWorldIndex = worldConfigManager.getWorldIndexForLevel(startLevel);
        int initialPagerPos = worldMapAdapter.toPagerPosition(initialWorldIndex);
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
        updateWorldSwitcherUI(initialWorldIndex);
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

    private void showOutOfHeartsDialog(@androidx.annotation.Nullable Runnable onPlayAction) {
        if (isFinishing() || isDestroyed()) return;
        OutOfHeartsDialog outOfHeartsDialog = new OutOfHeartsDialog(this, new OutOfHeartsDialog.OutOfHeartsDialogListener() {
            @Override
            public void onPlayLevelWithAdReward() {
                updateLivesUI();
                if (onPlayAction != null) {
                    onPlayAction.run();
                }
            }

            @Override
            public void onOpenShop() {
                showHeartStoreDialog();
            }

            @Override
            public void onGoHome() {
                updateLivesUI();
            }
        });
        outOfHeartsDialog.show();
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
            int currentLevel = prefs.getHighestUnlockedLevel();
            int highestUnlockedWorldIndex = worldConfigManager.getWorldIndexForLevel(currentLevel);
            if (worldIndex > highestUnlockedWorldIndex) {
                binding.tvCurrentWorldTitle.setText("🔒 " + world.subtitle + " (Locked)");
            } else {
                binding.tvCurrentWorldTitle.setText(world.subtitle);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileUI();
        updateDiamondsUI();
        updateLivesUI();
        int currentLevel = prefs.getHighestUnlockedLevel();

        binding.tvFloatingLevelNumber.setText(String.valueOf(currentLevel));
        updateStarChestProgressUI();

        if (worldMapAdapter != null) {
            worldMapAdapter.notifyDataSetChanged();
        }

        int targetWorldIndex = worldConfigManager.getWorldIndexForLevel(currentLevel);
        int pagerPos = worldMapAdapter.toPagerPosition(targetWorldIndex);
        android.util.Log.d("MainActivity", "onResume: highestUnlockedLevel=" + currentLevel + " -> worldIndex=" + targetWorldIndex + " -> pagerPos=" + pagerPos);
        binding.viewPagerWorldMaps.setCurrentItem(pagerPos, false);
        updateWorldSwitcherUI(targetWorldIndex);

        int endlessHigh = prefs.getEndlessHighScore();
        if (endlessHigh > 0) {
            binding.tvEndlessBestTag.setText("🏆 Best: " + String.format(java.util.Locale.getDefault(), "%,d", endlessHigh));
        } else {
            binding.tvEndlessBestTag.setText("🏆 Best: 0");
        }

        checkAndPromptAvailableGifts();
    }

    private void updateStarChestProgressUI() {
        if (binding == null || binding.tvStarChestBadge == null) return;
        int totalStars = repository.getTotalStarsEarned(levelManager.getTotalLevels());
        int claimedCount = prefs.getClaimedStarChestsCount();
        int totalMilestonesEarned = totalStars / 20;
        int unclaimedMilestones = Math.max(0, totalMilestonesEarned - claimedCount);
        boolean dailyFreeAvailable = prefs.canClaimDailyFreeDiamonds();
        int unclaimedCount = unclaimedMilestones + (dailyFreeAvailable ? 1 : 0);

        if (unclaimedCount > 0) {
            binding.tvStarChestBadge.setVisibility(View.VISIBLE);
            binding.tvStarChestBadge.setText(String.valueOf(unclaimedCount));
            startStarChestBadgePulse();
        } else {
            binding.tvStarChestBadge.setVisibility(View.GONE);
            stopStarChestBadgePulse();
        }
    }

    private void startStarChestBadgePulse() {
        if (binding == null || binding.layoutStarChestBadge == null) return;
        if (starChestBadgePulseAnimator != null && starChestBadgePulseAnimator.isRunning()) return;

        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.08f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.08f);
        starChestBadgePulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.layoutStarChestBadge, pvhX, pvhY);
        starChestBadgePulseAnimator.setDuration(700);
        starChestBadgePulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        starChestBadgePulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        starChestBadgePulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        starChestBadgePulseAnimator.start();
    }

    private void stopStarChestBadgePulse() {
        if (starChestBadgePulseAnimator != null) {
            starChestBadgePulseAnimator.cancel();
            starChestBadgePulseAnimator = null;
        }
        if (binding != null && binding.layoutStarChestBadge != null) {
            binding.layoutStarChestBadge.setScaleX(1.0f);
            binding.layoutStarChestBadge.setScaleY(1.0f);
        }
    }

    private void checkAndPromptAvailableGifts() {
        if (isFinishing() || isDestroyed()) return;

        // Prompt World Level Path Gifts if ready (Star Chests are claimed via the Star button badge)
        checkAndPromptWorldGift();
    }

    private void checkAndPromptWorldGift() {
        if (isFinishing() || isDestroyed()) return;
        if (activeClaimGiftDialog != null && activeClaimGiftDialog.isShowing()) return;

        int currentLevel = prefs.getHighestUnlockedLevel();
        int currentWorldIndex = worldConfigManager.getWorldIndexForLevel(currentLevel);
        WorldModel world = worldConfigManager.getWorldByIndex(currentWorldIndex);
        if (world != null && world.gifts != null) {
            for (int i = 0; i < world.gifts.size(); i++) {
                WorldConfigManager.GiftConfig gift = world.gifts.get(i);
                final int giftIndex = gift.giftIndex;
                int requiredLevel = (world.startLevel - 1) + gift.requiredLevelOffset;
                boolean isUnlocked = currentLevel > requiredLevel;
                boolean isClaimed = prefs.hasClaimedWorldGift(world.worldNumber, giftIndex);

                if (isUnlocked && !isClaimed) {
                    final int worldNum = world.worldNumber;
                    final int bonusDiamonds = (gift.rewardDiamonds > 0) ? gift.rewardDiamonds : 2;
                    binding.getRoot().postDelayed(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (activeClaimGiftDialog != null && activeClaimGiftDialog.isShowing()) return;
                        activeClaimGiftDialog = new ClaimGiftDialog(MainActivity.this, worldNum, giftIndex, bonusDiamonds, (wNum, gIdx, earned) -> {
                            updateDiamondsUI();
                            if (worldMapAdapter != null) {
                                worldMapAdapter.notifyDataSetChanged();
                            }
                        });
                        activeClaimGiftDialog.show();
                    }, 350);
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStarChestBadgePulse();
        if (settingsDialog != null && settingsDialog.isShowing()) settingsDialog.dismiss();
        if (profileDialog != null && profileDialog.isShowing()) profileDialog.dismiss();
        if (starChestDialog != null && starChestDialog.isShowing()) starChestDialog.dismiss();
        if (activeClaimGiftDialog != null && activeClaimGiftDialog.isShowing()) activeClaimGiftDialog.dismiss();
        if (heartStoreDialog != null && heartStoreDialog.isShowing()) heartStoreDialog.dismiss();
        if (activePreviewDialog != null && activePreviewDialog.isShowing()) activePreviewDialog.dismiss();
        if (activeExitDialog != null && activeExitDialog.isShowing()) activeExitDialog.dismiss();
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
        boolean isBoss = (level % 5 == 0);
        previewBinding.tvPreviewLevel.setText(isBoss ? "LEVEL " + level + " • BOSS" : "LEVEL " + level);

        int stars = prefs.getStarsForLevel(level);
        previewBinding.ivPreviewStar1.setImageResource(stars >= 1 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        previewBinding.ivPreviewStar2.setImageResource(stars >= 2 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        previewBinding.ivPreviewStar3.setImageResource(stars >= 3 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);



        boolean rainbowUnlocked = prefs.isBoosterUnlocked(BubbleType.RAINBOW, level);
        int rainbowCount = prefs.getRainbowBoosters();
        boolean rainbowAvailable = rainbowUnlocked && rainbowCount > 0;

        boolean fireballUnlocked = prefs.isBoosterUnlocked(BubbleType.FIREBALL, level);
        int fireballCount = prefs.getFireballBoosters();
        boolean fireballAvailable = fireballUnlocked && fireballCount > 0;

        boolean lightningUnlocked = prefs.isBoosterUnlocked(BubbleType.LIGHTNING, level);
        int lightningCount = prefs.getLightningBoosters();
        boolean lightningAvailable = lightningUnlocked && lightningCount > 0;

        boolean bombUnlocked = prefs.isBoosterUnlocked(BubbleType.BOMB, level);
        int bombCount = prefs.getBombBoosters();
        boolean bombAvailable = bombUnlocked && bombCount > 0;

        previewBinding.layoutPreviewBoosterRainbow.setVisibility(rainbowAvailable ? View.VISIBLE : View.GONE);
        if (rainbowAvailable) {
            previewBinding.tvCountPreviewRainbow.setText(String.valueOf(rainbowCount));
        }

        previewBinding.layoutPreviewBoosterFireball.setVisibility(fireballAvailable ? View.VISIBLE : View.GONE);
        if (fireballAvailable) {
            previewBinding.tvCountPreviewFireball.setText(String.valueOf(fireballCount));
        }

        previewBinding.layoutPreviewBoosterLightning.setVisibility(lightningAvailable ? View.VISIBLE : View.GONE);
        if (lightningAvailable) {
            previewBinding.tvCountPreviewLightning.setText(String.valueOf(lightningCount));
        }

        previewBinding.layoutPreviewBoosterBomb.setVisibility(bombAvailable ? View.VISIBLE : View.GONE);
        if (bombAvailable) {
            previewBinding.tvCountPreviewBomb.setText(String.valueOf(bombCount));
        }

        boolean anyBoosterAvailable = rainbowAvailable || fireballAvailable || lightningAvailable || bombAvailable;
        previewBinding.tvSelectBoostersHeader.setVisibility(anyBoosterAvailable ? View.VISIBLE : View.GONE);
        previewBinding.layoutBoostersRow.setVisibility(anyBoosterAvailable ? View.VISIBLE : View.GONE);

        final BubbleType[] selectedBooster = new BubbleType[]{null};
        Runnable updateBoosterSelection = () -> {
            BubbleType sel = selectedBooster[0];

            boolean isRainbow = sel == BubbleType.RAINBOW;
            previewBinding.ringBoosterRainbow.setVisibility(isRainbow ? View.VISIBLE : View.GONE);
            previewBinding.ivCheckPreviewRainbow.setVisibility(isRainbow ? View.VISIBLE : View.GONE);

            boolean isFireball = sel == BubbleType.FIREBALL;
            previewBinding.ringBoosterFireball.setVisibility(isFireball ? View.VISIBLE : View.GONE);
            previewBinding.ivCheckPreviewFireball.setVisibility(isFireball ? View.VISIBLE : View.GONE);

            boolean isLightning = sel == BubbleType.LIGHTNING;
            previewBinding.ringBoosterLightning.setVisibility(isLightning ? View.VISIBLE : View.GONE);
            previewBinding.ivCheckPreviewLightning.setVisibility(isLightning ? View.VISIBLE : View.GONE);

            boolean isBomb = sel == BubbleType.BOMB;
            previewBinding.ringBoosterBomb.setVisibility(isBomb ? View.VISIBLE : View.GONE);
            previewBinding.ivCheckPreviewBomb.setVisibility(isBomb ? View.VISIBLE : View.GONE);
        };

        previewBinding.btnToggleBoosterRainbow.setOnClickListener(v -> {
            soundManager.playClick();
            if (selectedBooster[0] == BubbleType.RAINBOW) {
                selectedBooster[0] = null;
            } else {
                selectedBooster[0] = BubbleType.RAINBOW;
                v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                ).start();
            }
            updateBoosterSelection.run();
        });

        previewBinding.btnToggleBoosterFireball.setOnClickListener(v -> {
            soundManager.playClick();
            if (selectedBooster[0] == BubbleType.FIREBALL) {
                selectedBooster[0] = null;
            } else {
                selectedBooster[0] = BubbleType.FIREBALL;
                v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                ).start();
            }
            updateBoosterSelection.run();
        });

        previewBinding.btnToggleBoosterLightning.setOnClickListener(v -> {
            soundManager.playClick();
            if (selectedBooster[0] == BubbleType.LIGHTNING) {
                selectedBooster[0] = null;
            } else {
                selectedBooster[0] = BubbleType.LIGHTNING;
                v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                ).start();
            }
            updateBoosterSelection.run();
        });

        previewBinding.btnToggleBoosterBomb.setOnClickListener(v -> {
            soundManager.playClick();
            if (selectedBooster[0] == BubbleType.BOMB) {
                selectedBooster[0] = null;
            } else {
                selectedBooster[0] = BubbleType.BOMB;
                v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                ).start();
            }
            updateBoosterSelection.run();
        });

        previewBinding.btnStartLevel.setOnClickListener(v -> {
            soundManager.playClick();
            if (prefs.getLives() <= 0) {
                showOutOfHeartsDialog(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    activePreviewDialog = null;
                    String boosterExtra = selectedBooster[0] != null ? selectedBooster[0].name() : null;
                    startActivity(GameActivity.createIntent(MainActivity.this, level, boosterExtra));
                });
                return;
            }
            dialog.dismiss();
            activePreviewDialog = null;
            String boosterExtra = selectedBooster[0] != null ? selectedBooster[0].name() : null;
            startActivity(GameActivity.createIntent(MainActivity.this, level, boosterExtra));
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
                        // Sync levels, diamonds, boosters from Google Play Games Cloud
                        com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().loadAndSyncFromCloud(MainActivity.this, (success, msg) -> {
                            runOnUiThread(() -> refreshAllUI());
                        });
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
                        // Sync levels, diamonds, boosters silently on app launch
                        com.redcodersgroup.bubbleshooter.auth.CloudSaveManager.getInstance().loadAndSyncFromCloud(MainActivity.this, (success, msg) -> {
                            runOnUiThread(() -> refreshAllUI());
                        });
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // Silent sign-in not available or cancelled
                    }
                });
    }

    public void refreshAllUI() {
        updateProfileUI();
        updateDiamondsUI();
        updateLivesUI();
        int curLvl = prefs.getHighestUnlockedLevel();
        int worldIdx = worldConfigManager.getWorldIndexForLevel(curLvl);
        updateWorldSwitcherUI(worldIdx);
        if (worldMapAdapter != null) {
            worldMapAdapter.notifyDataSetChanged();
        }
    }

    private void showExitConfirmDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (activeExitDialog != null && activeExitDialog.isShowing()) return;

        activeExitDialog = new ExitConfirmDialog(this, () -> {
            finishAffinity();
        });
        activeExitDialog.show();
    }
}