package com.redcodersgroup.bubbleshooter.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.redcodersgroup.bubbleshooter.MainActivity;
import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.data.ProgressRepository;
import com.redcodersgroup.bubbleshooter.databinding.ActivityGameBinding;
import com.redcodersgroup.bubbleshooter.game.GameEngine;
import com.redcodersgroup.bubbleshooter.level.Level;
import com.redcodersgroup.bubbleshooter.level.LevelManager;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.ui.dialogs.GameOverDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.LowShotsWarningDialog;
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

    private int currentLevelNumber = 1;
    private boolean isEndlessMode = false;
    private int currentStarsCount = 0;
    private int[] currentStarThresholds = new int[]{1000, 2000, 3000};
    private android.animation.ValueAnimator progressAnimator;
    private PauseDialog activePauseDialog;
    private VictoryDialog activeVictoryDialog;
    private GameOverDialog activeGameOverDialog;
    private LowShotsWarningDialog activeLowShotsWarningDialog;
    private boolean hasShownLowShotsWarning = false;
    private boolean isGameOverOrWon = false;
    private boolean wasBackgrounded = false;

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

        binding.btnBoosterRainbow.setOnClickListener(v -> {
            playBoosterTapFeedback(binding.layoutBoosterRainbow);
            gameEngine.equipBooster(BubbleType.RAINBOW);
            updateBoosterCounts();
        });

        binding.btnBoosterFireball.setOnClickListener(v -> {
            playBoosterTapFeedback(binding.layoutBoosterFireball);
            gameEngine.equipBooster(BubbleType.FIREBALL);
            updateBoosterCounts();
        });

        binding.btnBoosterLightning.setOnClickListener(v -> {
            playBoosterTapFeedback(binding.layoutBoosterLightning);
            gameEngine.equipBooster(BubbleType.LIGHTNING);
            updateBoosterCounts();
        });

        binding.btnBoosterBomb.setOnClickListener(v -> {
            playBoosterTapFeedback(binding.layoutBoosterBomb);
            gameEngine.equipBooster(BubbleType.BOMB);
            updateBoosterCounts();
        });

        updateBoosterCounts();
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
        binding.tvCountRainbow.setText("∞");
        binding.tvCountFireball.setText("∞");
        binding.tvCountLightning.setText("∞");
        binding.tvCountBomb.setText("∞");
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
        isGameOverOrWon = false;
        wasBackgrounded = false;
        hasShownLowShotsWarning = false;
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
    }

    private void loadEndlessMode() {
        AnalyticsManager.getInstance(this).logEndlessStart();
        isGameOverOrWon = false;
        wasBackgrounded = false;
        hasShownLowShotsWarning = false;
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

            activeVictoryDialog = new VictoryDialog(this, score, newHigh, effectiveStars, objectiveSummary, shotsRemaining, shotBonus, new VictoryDialog.VictoryDialogListener() {
                @Override
                public void onNextLevelClicked() {
                    activeVictoryDialog = null;
                    if (currentLevelNumber < levelManager.getTotalLevels()) {
                        currentLevelNumber++;
                        loadCurrentLevel();
                    } else {
                        finish();
                    }
                    enableImmersiveStickyMode();
                }

                @Override
                public void onReplayClicked() {
                    activeVictoryDialog = null;
                    loadCurrentLevel();
                    enableImmersiveStickyMode();
                }

                @Override
                public void onHomeClicked() {
                    activeVictoryDialog = null;
                    finish();
                }
            });
            activeVictoryDialog.show();
        });
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

            activeGameOverDialog = new GameOverDialog(this, score, reason, personalBest, isEndlessMode, livesAfterLoss, new GameOverDialog.GameOverDialogListener() {
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
            });
            activeGameOverDialog.show();
        });
    }

    private void showNoHeartsDialog() {
        if (isFinishing() || isDestroyed()) return;
        new com.redcodersgroup.bubbleshooter.ui.dialogs.HeartStoreDialog(this, () -> {
            // After store is dismissed, re-check lives
            if (prefs.getLives() > 0) {
                loadCurrentLevel();
                enableImmersiveStickyMode();
            } else {
                // Still no hearts — go home
                finish();
            }
        }).show();
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
    }
}
