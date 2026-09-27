package com.redcodersgroup.bubbleshooter.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.redcodersgroup.bubbleshooter.MainActivity;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.ads.AdManager;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.data.ProgressRepository;
import com.redcodersgroup.bubbleshooter.databinding.ActivityGameBinding;
import com.redcodersgroup.bubbleshooter.game.GameEngine;
import com.redcodersgroup.bubbleshooter.level.Level;
import com.redcodersgroup.bubbleshooter.level.LevelManager;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.BoosterIntroDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.BuyBoosterDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.GameOverDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.LowShotsWarningDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.NoticeDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.PauseDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.VictoryDialog;

public class GameActivity extends BaseActivity implements GameEngine.GameEventListener {

    public static final String EXTRA_LEVEL_NUMBER = "extra_level_number";
    public static final String EXTRA_IS_ENDLESS = "extra_is_endless";

    private ActivityGameBinding binding;
    private GameEngine gameEngine;
    private ProgressRepository repository;
    private PreferencesManager prefs;
    private LevelManager levelManager;
    private SoundManager soundManager;

    private int currentLevelNumber = 1;
    private boolean isEndlessMode = false;
    private int currentStarsCount = 0;
    private int[] currentStarThresholds = new int[]{1000, 2000, 3000};
    private android.animation.ValueAnimator progressAnimator;
    private ObjectAnimator doubleBonusPulseAnimator;
    private PauseDialog activePauseDialog;
    private VictoryDialog activeVictoryDialog;
    private GameOverDialog activeGameOverDialog;
    private LowShotsWarningDialog activeLowShotsWarningDialog;
    private BuyBoosterDialog activeBuyBoosterDialog;
    private boolean hasShownLowShotsWarning = false;
    private boolean isGameOverOrWon = false;
    private boolean wasBackgrounded = false;
    private int continueBubblePurchasesCount = 0;

    public static Intent createIntent(Context context, int levelNumber) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(EXTRA_LEVEL_NUMBER, levelNumber);
        return intent;
    }

    public static Intent createEndlessIntent(Context context) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(EXTRA_IS_ENDLESS, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        wasBackgrounded = false;
        isEndlessMode = getIntent().getBooleanExtra(EXTRA_IS_ENDLESS, false);
        currentLevelNumber = getIntent().getIntExtra(EXTRA_LEVEL_NUMBER, 1);
        repository = ProgressRepository.getInstance(this);
        prefs = repository.getPreferences();
        levelManager = LevelManager.getInstance(this);
        soundManager = SoundManager.getInstance(this);

        initViews();
        setupGame();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private void initViews() {
        binding.btnPause.setOnClickListener(v -> showPauseDialog());

        ViewCompat.setOnApplyWindowInsetsListener(binding.topHud, (v, insets) -> {
            Insets cutoutOrStatusInsets = insets.getInsets(
                    WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.statusBars()
            );
            float density = getResources().getDisplayMetrics().density;
            int defaultPadTop = (int) (32 * density);
            int safeTop = Math.max(defaultPadTop, cutoutOrStatusInsets.top + (int) (6 * density));
            int padBottom = (int) (8 * density);
            int padStart = (int) (12 * density) + cutoutOrStatusInsets.left;
            int padEnd = (int) (12 * density) + cutoutOrStatusInsets.right;

            binding.topHud.setPadding(padStart, safeTop, padEnd, padBottom);
            return insets;
        });

        binding.topHud.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int hudHeight = bottom - top;
            if (hudHeight > 0 && gameEngine != null) {
                gameEngine.setTopMargin(hudHeight + (4 * getResources().getDisplayMetrics().density));
            }
        });

        binding.layoutStarProgressTrack.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft)) {
                positionStarNodes();
            }
        });

        binding.btnBoosterRainbow.setOnClickListener(v -> handleBoosterClick(BubbleType.RAINBOW));
        binding.btnBoosterFireball.setOnClickListener(v -> handleBoosterClick(BubbleType.FIREBALL));
        binding.btnBoosterLightning.setOnClickListener(v -> handleBoosterClick(BubbleType.LIGHTNING));
        binding.btnBoosterBomb.setOnClickListener(v -> handleBoosterClick(BubbleType.BOMB));

        updateBoosterCounts();
    }

    private boolean isBoosterUnlockedForGame(BubbleType type) {
        if (isEndlessMode) {
            return prefs.isBoosterUnlockedGlobally(type);
        } else {
            return prefs.isBoosterUnlocked(type, currentLevelNumber);
        }
    }

    private void handleBoosterClick(BubbleType type) {
        if (!isBoosterUnlockedForGame(type)) {
            soundManager.playClick();
            String worldName;
            int reqLevel;
            switch (type) {
                case BOMB:
                    worldName = "World 2 (Levels 21-40)";
                    reqLevel = PreferencesManager.UNLOCK_LEVEL_BOMB;
                    break;
                case RAINBOW:
                    worldName = "World 3 (Levels 41-60)";
                    reqLevel = PreferencesManager.UNLOCK_LEVEL_RAINBOW;
                    break;
                case FIREBALL:
                    worldName = "World 4 (Levels 61-80)";
                    reqLevel = PreferencesManager.UNLOCK_LEVEL_FIREBALL;
                    break;
                case LIGHTNING:
                    worldName = "World 5 (Levels 81-100)";
                    reqLevel = PreferencesManager.UNLOCK_LEVEL_LIGHTNING;
                    break;
                default:
                    worldName = "Later Worlds";
                    reqLevel = 1;
                    break;
            }
            NoticeDialog.showWarning(
                    this,
                    "BOOSTER LOCKED",
                    "UNLOCKS AT LEVEL " + reqLevel,
                    "LOCKED POWER-UP",
                    "Advance to " + worldName + " to unlock and use this booster!"
            );
            return;
        }

        boolean consumed = false;
        View targetLayout = null;
        switch (type) {
            case BOMB:
                consumed = prefs.consumeBombBooster();
                targetLayout = binding.layoutBoosterBomb;
                break;
            case RAINBOW:
                consumed = prefs.consumeRainbowBooster();
                targetLayout = binding.layoutBoosterRainbow;
                break;
            case FIREBALL:
                consumed = prefs.consumeFireballBooster();
                targetLayout = binding.layoutBoosterFireball;
                break;
            case LIGHTNING:
                consumed = prefs.consumeLightningBooster();
                targetLayout = binding.layoutBoosterLightning;
                break;
            default:
                break;
        }

        if (consumed) {
            playBoosterTapFeedback(targetLayout);
            gameEngine.equipBooster(type);
            updateBoosterCounts();
        } else {
            soundManager.playClick();
            if (activeBuyBoosterDialog != null && activeBuyBoosterDialog.isShowing()) {
                activeBuyBoosterDialog.dismiss();
                activeBuyBoosterDialog = null;
            }
            activeBuyBoosterDialog = BuyBoosterDialog.show(this, type, boosterType -> {
                startActivity(ShopActivity.createIntent(GameActivity.this, ShopActivity.TAB_BOOSTERS));
            });
        }
    }

    private void playBoosterTapFeedback(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(0.80f);
        view.setScaleY(0.80f);
        view.animate()
                .scaleX(1.20f)
                .scaleY(1.20f)
                .setDuration(110)
                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start())
                .start();
    }

    private void updateBoosterCounts() {
        boolean bombUnlocked = isBoosterUnlockedForGame(BubbleType.BOMB);
        boolean rainbowUnlocked = isBoosterUnlockedForGame(BubbleType.RAINBOW);
        boolean fireballUnlocked = isBoosterUnlockedForGame(BubbleType.FIREBALL);
        boolean lightningUnlocked = isBoosterUnlockedForGame(BubbleType.LIGHTNING);

        int bomb = prefs.getBombBoosters();
        int rainbow = prefs.getRainbowBoosters();
        int fireball = prefs.getFireballBoosters();
        int lightning = prefs.getLightningBoosters();

        updateSingleBoosterUI(binding.layoutBoosterBomb, binding.tvCountBomb, bombUnlocked, bomb);
        updateSingleBoosterUI(binding.layoutBoosterRainbow, binding.tvCountRainbow, rainbowUnlocked, rainbow);
        updateSingleBoosterUI(binding.layoutBoosterFireball, binding.tvCountFireball, fireballUnlocked, fireball);
        updateSingleBoosterUI(binding.layoutBoosterLightning, binding.tvCountLightning, lightningUnlocked, lightning);
    }

    private void updateSingleBoosterUI(View boosterLayout, TextView badgeView, boolean isUnlocked, int count) {
        if (boosterLayout == null || badgeView == null) return;
        if (!isUnlocked) {
            // Locked booster: clearly dimmed out with lock badge
            boosterLayout.setAlpha(0.28f);
            badgeView.setText("🔒");
            badgeView.setBackgroundResource(R.drawable.bg_booster_badge_locked);
            badgeView.setTextColor(Color.parseColor("#CBD5E1"));
        } else if (count > 0) {
            // Available booster: fully vibrant (1.0f) with emerald green count badge
            boosterLayout.setAlpha(1.0f);
            badgeView.setText(String.valueOf(count));
            badgeView.setBackgroundResource(R.drawable.bg_booster_badge_available);
            badgeView.setTextColor(Color.WHITE);
        } else {
            // Unlocked but Exhausted (0 count): crisp & visible (0.92f) with gold '+' refill badge
            boosterLayout.setAlpha(0.92f);
            badgeView.setText("+");
            badgeView.setBackgroundResource(R.drawable.bg_booster_badge_buy);
            badgeView.setTextColor(Color.WHITE);
        }
    }

    private void setupGame() {
        gameEngine = new GameEngine(this);
        gameEngine.setEventListener(this);
        binding.bubbleGameView.setGameEngine(gameEngine);

        if (isEndlessMode) {
            loadEndlessMode();
        } else {
            loadCurrentLevel();
        }
    }

    private void loadCurrentLevel() {
        continueBubblePurchasesCount = 0;
        isGameOverOrWon = false;
        wasBackgrounded = false;
        hasShownLowShotsWarning = false;
        if (binding != null && binding.overlayVictory != null) {
            binding.overlayVictory.rootVictoryOverlay.setVisibility(View.GONE);
        }
        if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
            activeLowShotsWarningDialog.dismissWithAnimation();
            activeLowShotsWarningDialog = null;
        }
        binding.tvShotsLabel.setText("SHOTS");
        binding.bubbleGameView.setBiomeLevel(currentLevelNumber);
        BubbleGameView.BiomeTheme theme = binding.bubbleGameView.getCurrentBiome();
        binding.tvLevelTitle.setText("LVL " + currentLevelNumber + " • " + (theme != null ? theme.title : ""));
        Level level = levelManager.getLevel(currentLevelNumber);
        if (level != null && level.getObjective() != null) {
            level.getObjective().resetProgress();
            binding.ivObjectiveIcon.setImageResource(R.drawable.ic_target_bullseye);
            binding.tvObjectiveBadge.setText(level.getObjective().getBadgeText(0, level.getRows().size() * 8));
            binding.layoutObjectiveBadge.setBackgroundResource(R.drawable.bg_badge_objective);
        }
        currentStarsCount = 0;
        resetStarProgressNodes(level != null ? level.getStarThresholds() : new int[]{1000, 2000, 3000});
        gameEngine.loadLevel(level);
        AnalyticsManager.getInstance(this).logLevelStart(currentLevelNumber, theme != null ? theme.title : "World");
        updateBoosterCounts();
        checkBoosterIntroOnLevelStart();
    }

    private void checkBoosterIntroOnLevelStart() {
        if (isEndlessMode) return;

        BubbleType introType = null;
        String worldName = "";
        if (currentLevelNumber >= PreferencesManager.UNLOCK_LEVEL_LIGHTNING && !prefs.hasSeenBoosterIntro("LIGHTNING")) {
            introType = BubbleType.LIGHTNING;
            worldName = "World 5 • Neon Cyberland";
        } else if (currentLevelNumber >= PreferencesManager.UNLOCK_LEVEL_FIREBALL && !prefs.hasSeenBoosterIntro("FIREBALL")) {
            introType = BubbleType.FIREBALL;
            worldName = "World 4 • Volcanic Forge";
        } else if (currentLevelNumber >= PreferencesManager.UNLOCK_LEVEL_RAINBOW && !prefs.hasSeenBoosterIntro("RAINBOW")) {
            introType = BubbleType.RAINBOW;
            worldName = "World 3 • Celestial Cosmos";
        } else if (currentLevelNumber >= PreferencesManager.UNLOCK_LEVEL_BOMB && !prefs.hasSeenBoosterIntro("BOMB")) {
            introType = BubbleType.BOMB;
            worldName = "World 2 • Crystal Caverns";
        }

        if (introType != null) {
            final BubbleType typeToUnlock = introType;
            prefs.setSeenBoosterIntro(typeToUnlock.name(), true);
            prefs.grantBoosterUnlockReward(typeToUnlock);
            updateBoosterCounts();

            BoosterIntroDialog.show(this, typeToUnlock, worldName, this::updateBoosterCounts);
        }
    }

    private void loadEndlessMode() {
        continueBubblePurchasesCount = 0;
        AnalyticsManager.getInstance(this).logEndlessStart();
        isGameOverOrWon = false;
        wasBackgrounded = false;
        hasShownLowShotsWarning = false;
        if (binding != null && binding.overlayVictory != null) {
            binding.overlayVictory.rootVictoryOverlay.setVisibility(View.GONE);
        }
        if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
            activeLowShotsWarningDialog.dismissWithAnimation();
            activeLowShotsWarningDialog = null;
        }

        binding.bubbleGameView.setEndlessBiome(1);
        BubbleGameView.BiomeTheme theme = binding.bubbleGameView.getCurrentBiome();
        String themeTitle = (theme != null) ? theme.title : "Meadows";
        binding.tvLevelTitle.setText("WAVE 1 • " + themeTitle);

        binding.tvShotsLabel.setText("WAVE");
        binding.tvShotsCount.setText("1");

        binding.ivObjectiveIcon.setImageResource(R.drawable.ic_lightning);
        int personalBest = prefs.getEndlessHighScore();
        if (personalBest > 0) {
            binding.tvObjectiveBadge.setText("BEST: " + String.format(java.util.Locale.getDefault(), "%,d", personalBest) + " • WAVE 1");
        } else {
            binding.tvObjectiveBadge.setText("SURVIVE • WAVE 1");
        }
        binding.layoutObjectiveBadge.setBackgroundResource(R.drawable.bg_badge_objective);
        currentStarsCount = 0;

        int[] thresholds;
        if (personalBest > 0) {
            int t1 = Math.max(500, (int) (personalBest * 0.35f));
            int t2 = Math.max(1000, (int) (personalBest * 0.70f));
            int t3 = Math.max(1500, personalBest);
            thresholds = new int[]{t1, t2, t3};
        } else {
            thresholds = new int[]{1000, 2500, 5000};
        }

        resetStarProgressNodes(thresholds);
        updateBoosterCounts();
        gameEngine.loadEndlessMode(personalBest, thresholds, null);
    }

    private void resetStarProgressNodes(int[] thresholds) {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        binding.pbStarProgress.setProgress(0);
        binding.tvScore.setText("SCORE: 0");

        binding.hudStarNode1.setBackgroundResource(R.drawable.bg_hud_star_node);
        binding.hudStarNode2.setBackgroundResource(R.drawable.bg_hud_star_node);
        binding.hudStarNode3.setBackgroundResource(R.drawable.bg_hud_star_node);

        binding.hudStar1.setImageResource(R.drawable.ic_star_empty);
        binding.hudStar2.setImageResource(R.drawable.ic_star_empty);
        binding.hudStar3.setImageResource(R.drawable.ic_star_empty);

        binding.layoutShots.setBackgroundResource(R.drawable.bg_button_glossy_green);

        if (thresholds != null && thresholds.length >= 3) {
            currentStarThresholds = thresholds;
        }

        binding.layoutStarProgressTrack.post(this::positionStarNodes);
    }

    private void positionStarNodes() {
        int width = binding.layoutStarProgressTrack.getWidth();
        if (width <= 0) return;

        int maxThreshold = Math.max(1, currentStarThresholds[2]);
        float ratio1 = Math.max(0.1f, Math.min(0.85f, (float) currentStarThresholds[0] / maxThreshold));
        float ratio2 = Math.max(ratio1 + 0.1f, Math.min(0.92f, (float) currentStarThresholds[1] / maxThreshold));

        int node1W = binding.hudStarNode1.getWidth();
        int node2W = binding.hudStarNode2.getWidth();
        int node3W = binding.hudStarNode3.getWidth();

        float x1 = Math.max(0, ratio1 * width - node1W / 2f);
        float x2 = Math.max(0, ratio2 * width - node2W / 2f);
        float x3 = Math.max(0, width - node3W);

        binding.hudStarNode1.setTranslationX(x1);
        binding.hudStarNode2.setTranslationX(x2);
        binding.hudStarNode3.setTranslationX(x3);
    }

    private void showPauseDialog() {
        if (isFinishing() || isDestroyed() || isGameOverOrWon) return;
        if (activePauseDialog != null && activePauseDialog.isShowing()) return;

        if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
            activeLowShotsWarningDialog.dismiss();
            activeLowShotsWarningDialog = null;
        }

        if (gameEngine != null) {
            gameEngine.pause();
        }

        activePauseDialog = new PauseDialog(this, isEndlessMode, new PauseDialog.PauseDialogListener() {
            @Override
            public void onResumeClicked() {
                activePauseDialog = null;
                if (gameEngine != null) {
                    gameEngine.resume();
                }
                enableImmersiveStickyMode();
            }

            @Override
            public void onRestartClicked() {
                activePauseDialog = null;
                if (isEndlessMode) {
                    loadEndlessMode();
                } else {
                    loadCurrentLevel();
                }
                enableImmersiveStickyMode();
            }

            @Override
            public void onHomeClicked() {
                activePauseDialog = null;
                Intent intent = new Intent(GameActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
        activePauseDialog.show();
    }

    private void handleBackPress() {
        if (isFinishing() || isDestroyed()) return;

        // If full-screen victory overlay is active, back press finishes cleanly
        if (binding != null && binding.overlayVictory != null &&
                binding.overlayVictory.rootVictoryOverlay.getVisibility() == View.VISIBLE) {
            finish();
            return;
        }

        // If a terminal dialog (Victory or Game Over) is active, back finishes cleanly
        if (activeVictoryDialog != null && activeVictoryDialog.isShowing()) {
            activeVictoryDialog.dismiss();
            activeVictoryDialog = null;
            finish();
            return;
        }

        if (activeGameOverDialog != null && activeGameOverDialog.isShowing()) {
            activeGameOverDialog.dismiss();
            activeGameOverDialog = null;
            finish();
            return;
        }

        // If pause dialog is already visible, back press dismisses and resumes gameplay
        if (activePauseDialog != null && activePauseDialog.isShowing()) {
            activePauseDialog.dismiss();
            activePauseDialog = null;
            if (gameEngine != null) {
                gameEngine.resume();
            }
            enableImmersiveStickyMode();
            return;
        }

        // If in active gameplay (any mode: Level Mode or Endless Mode), show the pause dialog
        if (!isGameOverOrWon) {
            showPauseDialog();
        } else {
            finish();
        }
    }

    @Override
    public void onScoreUpdated(int score, int stars, float starProgress) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            binding.tvScore.setText("SCORE: " + String.format("%,d", score));

            int targetProgress = Math.min(1000, (int) (starProgress * 1000f));
            if (progressAnimator != null && progressAnimator.isRunning()) {
                progressAnimator.cancel();
            }
            progressAnimator = android.animation.ObjectAnimator.ofInt(binding.pbStarProgress, "progress", binding.pbStarProgress.getProgress(), targetProgress);
            progressAnimator.setDuration(280);
            progressAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            progressAnimator.start();

            if (stars > currentStarsCount) {
                for (int s = currentStarsCount + 1; s <= stars; s++) {
                    activateStarNode(s);
                }
                currentStarsCount = stars;
            }
        });
    }

    private void activateStarNode(int starIndex) {
        View nodeView;
        android.widget.ImageView starImg;
        if (starIndex == 1) {
            nodeView = binding.hudStarNode1;
            starImg = binding.hudStar1;
        } else if (starIndex == 2) {
            nodeView = binding.hudStarNode2;
            starImg = binding.hudStar2;
        } else if (starIndex == 3) {
            nodeView = binding.hudStarNode3;
            starImg = binding.hudStar3;
        } else {
            return;
        }

        nodeView.setBackgroundResource(R.drawable.bg_hud_star_node_active);
        starImg.setImageResource(R.drawable.ic_star_filled);

        nodeView.setScaleX(0.4f);
        nodeView.setScaleY(0.4f);
        nodeView.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .setDuration(220)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.5f))
                .withEndAction(() -> {
                    nodeView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .start();
                })
                .start();
    }

    @Override
    public void onObjectiveUpdated(String badgeText, boolean isCompleted, String summaryText) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            binding.tvObjectiveBadge.setText(badgeText);
            if (isCompleted) {
                binding.ivObjectiveIcon.setImageResource(R.drawable.ic_star_filled);
                binding.layoutObjectiveBadge.setBackgroundResource(R.drawable.bg_badge_objective_completed);
                binding.layoutObjectiveBadge.animate().cancel();
                binding.layoutObjectiveBadge.setScaleX(1.18f);
                binding.layoutObjectiveBadge.setScaleY(1.18f);
                binding.layoutObjectiveBadge.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(250)
                        .start();
            } else {
                binding.ivObjectiveIcon.setImageResource(isEndlessMode ? R.drawable.ic_lightning : R.drawable.ic_target_bullseye);
                binding.layoutObjectiveBadge.setBackgroundResource(R.drawable.bg_badge_objective);
            }
        });
    }

    @Override
    public void onShotsUpdated(int shotsRemainingOrWave) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            binding.tvShotsCount.setText(String.valueOf(shotsRemainingOrWave));
            if (isEndlessMode) {
                // Synchronize endless biome and background illustration as waves progress
                binding.bubbleGameView.setEndlessBiome(shotsRemainingOrWave);
                BubbleGameView.BiomeTheme theme = binding.bubbleGameView.getCurrentBiome();
                String themeTitle = (theme != null) ? theme.title : "Meadows";
                binding.tvLevelTitle.setText("WAVE " + shotsRemainingOrWave + " • " + themeTitle);
            } else {
                if (shotsRemainingOrWave <= 5) {
                    binding.layoutShots.setBackgroundResource(R.drawable.bg_button_glossy_red);
                    binding.layoutShots.animate().cancel();
                    binding.layoutShots.setScaleX(1.15f);
                    binding.layoutShots.setScaleY(1.15f);
                    binding.layoutShots.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start();
                } else {
                    binding.layoutShots.setBackgroundResource(R.drawable.bg_button_glossy_green);
                }

                // Show auto-hiding warning dialog when less than 5 shots remain
                if (shotsRemainingOrWave < 5 && shotsRemainingOrWave > 0 && !hasShownLowShotsWarning && !isGameOverOrWon) {
                    hasShownLowShotsWarning = true;
                    showLowShotsWarningDialog(shotsRemainingOrWave);
                }
            }
        });
    }

    private void showLowShotsWarningDialog(int shotsRemaining) {
        if (isFinishing() || isDestroyed() || isGameOverOrWon) return;
        if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
            activeLowShotsWarningDialog.dismiss();
            activeLowShotsWarningDialog = null;
        }
        activeLowShotsWarningDialog = new LowShotsWarningDialog(this, shotsRemaining);
        activeLowShotsWarningDialog.setOnDismissListener(dialog -> {
            activeLowShotsWarningDialog = null;
            enableImmersiveStickyMode();
        });
        activeLowShotsWarningDialog.show();
    }

    @Override
    public void onGameWon(int score, int stars) {
        onGameWon(score, stars, "✓ Level Completed!", 0, 0);
    }

    @Override
    public void onGameWon(int score, int stars, String objectiveSummary) {
        onGameWon(score, stars, objectiveSummary, 0, 0);
    }

    @Override
    public void onGameWon(int score, int stars, String objectiveSummary, int shotsRemaining, int shotBonus) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            isGameOverOrWon = true;
            wasBackgrounded = false;

            if (activePauseDialog != null && activePauseDialog.isShowing()) {
                activePauseDialog.dismiss();
                activePauseDialog = null;
            }
            if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
                activeLowShotsWarningDialog.dismiss();
                activeLowShotsWarningDialog = null;
            }

            int effectiveStars = Math.max(1, Math.min(3, stars));
            int previousHigh = prefs.getHighScoreForLevel(currentLevelNumber);
            int newHigh = Math.max(previousHigh, score);
            repository.completeLevel(currentLevelNumber, effectiveStars, score);

            BubbleGameView.BiomeTheme theme = binding.bubbleGameView.getCurrentBiome();
            AnalyticsManager.getInstance(this).logLevelComplete(
                    currentLevelNumber,
                    theme != null ? theme.title : "World",
                    score,
                    effectiveStars
            );

            showVictoryOverlay(score, newHigh, effectiveStars, objectiveSummary, shotsRemaining, shotBonus);
        });
    }

    private void showVictoryOverlay(int score, int newHigh, int stars, String objectiveSummary, int shotsRemaining, int shotBonus) {
        if (binding == null || binding.overlayVictory == null) return;

        // Automatically grant the standard +2 Diamonds level completion reward
        prefs.addDiamonds(2);
        android.os.Bundle bVictoryBase = new android.os.Bundle();
        bVictoryBase.putInt("amount", 2);
        AnalyticsManager.getInstance(this).logEvent("level_victory_base_diamonds", bVictoryBase);

        final boolean[] hasDoubledReward = {false};

        // UI Texts
        binding.overlayVictory.tvVictoryTitle.setText(isEndlessMode ? "STAGE CLEARED!" : "LEVEL " + currentLevelNumber + " COMPLETE!");
        binding.overlayVictory.tvVictoryScore.setText(String.format(Locale.getDefault(), "%,d", score));
        binding.overlayVictory.tvVictoryHighScore.setText("HIGH SCORE: " + String.format(Locale.getDefault(), "%,d", Math.max(score, newHigh)));

        // Diamond reward card initial state
        binding.overlayVictory.tvVictoryDiamondReward.setText("+2 DIAMONDS");
        binding.overlayVictory.tvVictoryDiamondStatus.setText("CLAIMED");
        binding.overlayVictory.tvVictoryDiamondStatus.setBackgroundResource(R.drawable.bg_booster_badge_available);

        // Double bonus button initial state
        binding.overlayVictory.btnVictoryDoubleBonus.setText("🎬 DOUBLE BONUS • 💎 +4");
        binding.overlayVictory.btnVictoryDoubleBonus.setEnabled(true);
        binding.overlayVictory.btnVictoryDoubleBonus.setAlpha(1.0f);

        // Reset stars
        binding.overlayVictory.ivVictoryStar1.setImageResource(R.drawable.ic_star_empty);
        binding.overlayVictory.ivVictoryStar2.setImageResource(R.drawable.ic_star_empty);
        binding.overlayVictory.ivVictoryStar3.setImageResource(R.drawable.ic_star_empty);

        binding.overlayVictory.ivVictoryStar1.setScaleX(0f);
        binding.overlayVictory.ivVictoryStar1.setScaleY(0f);
        binding.overlayVictory.ivVictoryStar2.setScaleX(0f);
        binding.overlayVictory.ivVictoryStar2.setScaleY(0f);
        binding.overlayVictory.ivVictoryStar3.setScaleX(0f);
        binding.overlayVictory.ivVictoryStar3.setScaleY(0f);

        // Show overlay with fade-in
        binding.overlayVictory.rootVictoryOverlay.setVisibility(View.VISIBLE);
        binding.overlayVictory.rootVictoryOverlay.setAlpha(0f);
        binding.overlayVictory.rootVictoryOverlay.animate().alpha(1f).setDuration(240).start();

        // Animate stars popping in with bounce
        animateOverlayStar(binding.overlayVictory.ivVictoryStar1, stars >= 1, 250);
        animateOverlayStar(binding.overlayVictory.ivVictoryStar2, stars >= 2, 500);
        animateOverlayStar(binding.overlayVictory.ivVictoryStar3, stars >= 3, 750);

        // Start pulsing animation on Double Bonus button
        startDoubleBonusPulseAnimation();

        // Button handlers: Double Bonus (Rewarded Ad)
        binding.overlayVictory.btnVictoryDoubleBonus.setOnClickListener(v -> {
            soundManager.playClick();
            if (hasDoubledReward[0]) return;

            AdManager.getInstance().showRewardedVideo(GameActivity.this, new AdManager.RewardCallback() {
                @Override
                public void onRewardEarned(int amount, String type) {
                    hasDoubledReward[0] = true;
                    prefs.addDiamonds(2); // +2 more diamonds (making it +4 total)
                    android.os.Bundle bDouble = new android.os.Bundle();
                    bDouble.putInt("amount", 2);
                    AnalyticsManager.getInstance(GameActivity.this).logEvent("rewarded_ad_level_double", bDouble);
                    soundManager.playWin();

                    runOnUiThread(() -> {
                        if (binding != null && binding.overlayVictory != null) {
                            stopDoubleBonusPulseAnimation();
                            binding.overlayVictory.tvVictoryDiamondReward.setText("+4 DIAMONDS (2X)");
                            binding.overlayVictory.tvVictoryDiamondStatus.setText("DOUBLED! 🎁");
                            binding.overlayVictory.btnVictoryDoubleBonus.setText("✓ 2X BONUS CLAIMED!");
                            binding.overlayVictory.btnVictoryDoubleBonus.setEnabled(false);
                            binding.overlayVictory.btnVictoryDoubleBonus.setAlpha(0.65f);

                            // Pop animation on diamond icon
                            binding.overlayVictory.ivDiamondRewardIcon.animate()
                                    .scaleX(1.35f).scaleY(1.35f).setDuration(180)
                                    .withEndAction(() -> binding.overlayVictory.ivDiamondRewardIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start())
                                    .start();
                        }
                    });
                }

                @Override
                public void onAdClosed(boolean rewarded) {
                    if (!rewarded && !hasDoubledReward[0]) {
                        runOnUiThread(() -> NoticeDialog.showWarning(
                                GameActivity.this,
                                "AD SKIPPED",
                                "NO BONUS",
                                "AD INCOMPLETE",
                                "Watch the full video to double your diamonds bonus!"
                        ));
                    }
                }
            });
        });

        // Next Level Button
        binding.overlayVictory.btnVictoryNextLevel.setOnClickListener(v -> {
            soundManager.playClick();
            stopDoubleBonusPulseAnimation();
            if (binding != null && binding.overlayVictory != null) {
                binding.overlayVictory.rootVictoryOverlay.setVisibility(View.GONE);
            }
            final int completedLvl = currentLevelNumber;
            if (currentLevelNumber < levelManager.getTotalLevels()) {
                currentLevelNumber++;
                AdManager.getInstance().onLevelCompleted(GameActivity.this, completedLvl, () -> {
                    loadCurrentLevel();
                    enableImmersiveStickyMode();
                });
            } else {
                finish();
            }
        });

        // Replay Button
        binding.overlayVictory.btnVictoryReplay.setOnClickListener(v -> {
            soundManager.playClick();
            stopDoubleBonusPulseAnimation();
            if (binding != null && binding.overlayVictory != null) {
                binding.overlayVictory.rootVictoryOverlay.setVisibility(View.GONE);
            }
            if (isEndlessMode) {
                loadEndlessMode();
            } else {
                loadCurrentLevel();
            }
            enableImmersiveStickyMode();
        });

        // Home Button
        binding.overlayVictory.btnVictoryHome.setOnClickListener(v -> {
            soundManager.playClick();
            stopDoubleBonusPulseAnimation();
            if (binding != null && binding.overlayVictory != null) {
                binding.overlayVictory.rootVictoryOverlay.setVisibility(View.GONE);
            }
            finish();
        });
    }

    private void startDoubleBonusPulseAnimation() {
        stopDoubleBonusPulseAnimation();
        if (binding == null || binding.overlayVictory == null) return;
        View btn = binding.overlayVictory.btnVictoryDoubleBonus;
        btn.setScaleX(1.0f);
        btn.setScaleY(1.0f);

        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.08f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.08f);
        doubleBonusPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btn, pvhX, pvhY);
        doubleBonusPulseAnimator.setDuration(700);
        doubleBonusPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        doubleBonusPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        doubleBonusPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        doubleBonusPulseAnimator.start();
    }

    private void stopDoubleBonusPulseAnimation() {
        if (doubleBonusPulseAnimator != null) {
            doubleBonusPulseAnimator.cancel();
            doubleBonusPulseAnimator = null;
        }
        if (binding != null && binding.overlayVictory != null) {
            binding.overlayVictory.btnVictoryDoubleBonus.setScaleX(1.0f);
            binding.overlayVictory.btnVictoryDoubleBonus.setScaleY(1.0f);
        }
    }

    private void animateOverlayStar(ImageView iv, boolean filled, long delay) {
        if (!filled) return;
        iv.postDelayed(() -> {
            iv.setImageResource(R.drawable.ic_star_filled);
            iv.setScaleX(0f);
            iv.setScaleY(0f);
            soundManager.playBounce();
            iv.animate()
                    .scaleX(1.18f)
                    .scaleY(1.18f)
                    .setDuration(320)
                    .setInterpolator(new OvershootInterpolator(2.2f))
                    .withEndAction(() -> iv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start())
                    .start();
        }, delay);
    }

    @Override
    public void onGameLost(int score) {
        onGameLost(score, "Out of shots! Don't give up!");
    }

    @Override
    public void onGameLost(int score, String reason) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            isGameOverOrWon = true;
            wasBackgrounded = false;

            if (activePauseDialog != null && activePauseDialog.isShowing()) {
                activePauseDialog.dismiss();
                activePauseDialog = null;
            }
            if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
                activeLowShotsWarningDialog.dismiss();
                activeLowShotsWarningDialog = null;
            }

            int personalBest = 0;
            if (isEndlessMode) {
                personalBest = prefs.getEndlessHighScore();
                if (score > personalBest) {
                    prefs.setEndlessHighScore(score);
                }
                AnalyticsManager.getInstance(this).logEndlessGameOver(score, personalBest);
            } else {
                // Deduct one heart for losing a level (not in endless mode)
                prefs.deductLife();
                BubbleGameView.BiomeTheme theme = binding.bubbleGameView.getCurrentBiome();
                AnalyticsManager.getInstance(this).logLevelFail(
                        currentLevelNumber,
                        theme != null ? theme.title : "World",
                        score,
                        reason
                );
            }

            final int livesAfterLoss = prefs.getLives();
            final boolean isOutOfShots = !isEndlessMode && ((gameEngine != null && gameEngine.getShotsRemaining() <= 0) || (reason != null && reason.toLowerCase().contains("out of shot")));
            final int continueCost = 5 + continueBubblePurchasesCount;

            activeGameOverDialog = new GameOverDialog(this, score, reason, personalBest, isEndlessMode, livesAfterLoss, isOutOfShots, continueCost, new GameOverDialog.GameOverDialogListener() {
                @Override
                public void onRetryClicked() {
                    activeGameOverDialog = null;
                    if (!isEndlessMode && livesAfterLoss <= 0) {
                        // No hearts left — open Heart Store instead
                        showNoHeartsDialog();
                        return;
                    }
                    if (isEndlessMode) {
                        loadEndlessMode();
                    } else {
                        loadCurrentLevel();
                    }
                    enableImmersiveStickyMode();
                }

                @Override
                public void onHomeClicked() {
                    activeGameOverDialog = null;
                    finish();
                }

                @Override
                public void onGetMoreBubblesClicked() {
                    if (prefs.spendDiamonds(continueCost)) {
                        continueBubblePurchasesCount++;
                        prefs.addLives(1); // Refund the heart deducted when game over occurred
                        isGameOverOrWon = false;
                        if (activeGameOverDialog != null) {
                            activeGameOverDialog.dismiss();
                            activeGameOverDialog = null;
                        }
                        if (gameEngine != null) {
                            gameEngine.addExtraShots(5);
                        }
                        enableImmersiveStickyMode();
                    } else {
                        startActivity(com.redcodersgroup.bubbleshooter.ui.ShopActivity.createIntent(GameActivity.this, com.redcodersgroup.bubbleshooter.ui.ShopActivity.TAB_DIAMONDS));
                    }
                }
            });
            activeGameOverDialog.show();
        });
    }

    private void showNoHeartsDialog() {
        if (isFinishing() || isDestroyed()) return;
        startActivity(com.redcodersgroup.bubbleshooter.ui.ShopActivity.createIntent(this, com.redcodersgroup.bubbleshooter.ui.ShopActivity.TAB_HEARTS));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding != null && binding.bubbleGameView != null) {
            binding.bubbleGameView.pause();
        }
        if (gameEngine != null) {
            gameEngine.pause();
            if (!isGameOverOrWon) {
                wasBackgrounded = true;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bubbleGameView != null) {
            binding.bubbleGameView.resume();
        }
        updateBoosterCounts();

        // Only auto-show pause dialog if the app was actively sent to background during gameplay
        if (wasBackgrounded) {
            wasBackgrounded = false;
            if (!isGameOverOrWon && gameEngine != null) {
                if (activePauseDialog == null || !activePauseDialog.isShowing()) {
                    if ((activeVictoryDialog == null || !activeVictoryDialog.isShowing()) &&
                        (activeGameOverDialog == null || !activeGameOverDialog.isShowing())) {
                        showPauseDialog();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDoubleBonusPulseAnimation();
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
        if (activePauseDialog != null && activePauseDialog.isShowing()) {
            activePauseDialog.dismiss();
            activePauseDialog = null;
        }
        if (activeVictoryDialog != null && activeVictoryDialog.isShowing()) {
            activeVictoryDialog.dismiss();
            activeVictoryDialog = null;
        }
        if (activeGameOverDialog != null && activeGameOverDialog.isShowing()) {
            activeGameOverDialog.dismiss();
            activeGameOverDialog = null;
        }
        if (activeLowShotsWarningDialog != null && activeLowShotsWarningDialog.isShowing()) {
            activeLowShotsWarningDialog.dismiss();
            activeLowShotsWarningDialog = null;
        }
        if (activeBuyBoosterDialog != null && activeBuyBoosterDialog.isShowing()) {
            activeBuyBoosterDialog.dismiss();
            activeBuyBoosterDialog = null;
        }
    }
}
