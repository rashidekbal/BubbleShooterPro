package com.redcodersgroup.bubbleshooter.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.board.BubbleBoard;
import com.redcodersgroup.bubbleshooter.board.BubbleGrid;
import com.redcodersgroup.bubbleshooter.board.GridPosition;
import com.redcodersgroup.bubbleshooter.board.NeighborCalculator;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import com.redcodersgroup.bubbleshooter.bubble.BubbleProjectile;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import com.redcodersgroup.bubbleshooter.level.Level;
import com.redcodersgroup.bubbleshooter.level.LevelObjective;
import com.redcodersgroup.bubbleshooter.physics.CollisionDetector;
import com.redcodersgroup.bubbleshooter.physics.TrajectoryCalculator;
import com.redcodersgroup.bubbleshooter.physics.WallBounceCalculator;
import com.redcodersgroup.bubbleshooter.scoring.ComboManager;
import com.redcodersgroup.bubbleshooter.scoring.ScoreManager;
import com.redcodersgroup.bubbleshooter.visual.ConfettiSystem;
import com.redcodersgroup.bubbleshooter.visual.FloatingText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameEngine {

    public interface GameEventListener {
        void onScoreUpdated(int score, int stars, float starProgress);
        void onShotsUpdated(int shotsRemaining);
        void onGameWon(int score, int stars);
        default void onGameWon(int score, int stars, String objectiveSummary) {
            onGameWon(score, stars);
        }
        default void onGameWon(int score, int stars, String objectiveSummary, int shotsRemaining, int shotBonus) {
            onGameWon(score, stars, objectiveSummary);
        }
        void onGameLost(int score);
        default void onGameLost(int score, String reason) {
            onGameLost(score);
        }
        default void onObjectiveUpdated(String badgeText, boolean isCompleted, String summaryText) {}
    }

    private final Context context;
    private final SoundManager soundManager;
    private final BubbleGrid grid;
    private final BubbleBoard board;
    private final ScoreManager scoreManager;
    private final ComboManager comboManager;
    private final ConfettiSystem confettiSystem;
    private final Random random = new Random();

    private GameState state = GameState.READY;
    private Level currentLevel;
    private GameEventListener listener;

    private float boardLeft;
    private float boardRight;
    private float boardTop;
    private float boardBottom;
    private float bubbleRadius = 45f;
    private float launcherX;
    private float launcherY;
    private float previewX;
    private float previewY;
    private float deadlineY;
    private float dangerPulseTimer = 0f;
    private DashPathEffect normalDashEffect;
    private DashPathEffect dangerDashEffect;

    private float aimAngleRad = (float) (-Math.PI / 2.0); // straight up
    private List<PointF> trajectoryPoints = new ArrayList<>();
    private final Path laserPath = new Path();

    private Bubble currentBubble;
    private Bubble nextBubble;
    private BubbleProjectile activeProjectile;
    private int shotsRemaining = 25;
    private int initialShots = 25;
    private boolean isEndlessMode = false;
    private int endlessWaveCount = 1;
    private int endlessHighScore = 0;
    private boolean hasCelebratedNewBest = false;
    private List<BubbleColor> endlessColorsPool = new ArrayList<>();
    private final java.util.LinkedList<List<Bubble>> pregeneratedRowQueue = new java.util.LinkedList<>();

    private int getEffectiveStars() {
        return isEndlessMode ? scoreManager.getStarsEarned() : scoreManager.calculateLiveStars(shotsRemaining, initialShots);
    }

    public String getObjectiveBadgeText() {
        if (isEndlessMode) {
            int curScore = scoreManager.getScore();
            if (endlessHighScore > 0 && curScore > endlessHighScore) {
                return "🎉 NEW BEST! • " + String.format(java.util.Locale.getDefault(), "%,d", curScore);
            }
            if (endlessHighScore > 0) {
                return "⚡ BEST: " + String.format(java.util.Locale.getDefault(), "%,d", endlessHighScore) + " • WAVE " + endlessWaveCount;
            }
            return "⚡ SURVIVE • WAVE " + endlessWaveCount;
        }
        if (currentLevel != null && currentLevel.getObjective() != null) {
            LevelObjective obj = currentLevel.getObjective();
            return obj.getBadgeText(scoreManager.getScore(), grid.getBubbleCount());
        }
        return "🎯 CLEAR ALL: " + grid.getBubbleCount() + " LEFT";
    }

    public boolean isObjectiveCompleted() {
        if (isEndlessMode) {
            return endlessHighScore > 0 && scoreManager.getScore() > endlessHighScore;
        }
        if (currentLevel != null && currentLevel.getObjective() != null) {
            return currentLevel.getObjective().isMet(board, scoreManager);
        }
        return grid.getBubbleCount() == 0;
    }

    public String getObjectiveCompletedSummary() {
        if (isEndlessMode) {
            return "Survived " + endlessWaveCount + " waves!";
        }
        if (currentLevel != null && currentLevel.getObjective() != null) {
            return currentLevel.getObjective().getCompletedSummaryText();
        }
        return "All bubbles cleared!";
    }

    public void notifyObjectiveUpdated() {
        if (listener != null) {
            listener.onObjectiveUpdated(getObjectiveBadgeText(), isObjectiveCompleted(), getObjectiveCompletedSummary());
        }
    }

    private final List<Bubble> poppingBubbles = new ArrayList<>();
    private final List<Bubble> fallingBubbles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    private float resolveTimer = 0f;
    private boolean isSuperAimActive = false;
    private boolean isAimCancelled = false;
    private boolean isFireballBlocked = false;

    // Launcher reload jump & pop-in animation
    private boolean isLauncherReloading = false;
    private float reloadTimer = 0f;
    private static final float RELOAD_DURATION = 0.22f;

    // Launcher swap jump animation
    private boolean isSwapping = false;
    private float swapTimer = 0f;
    private static final float SWAP_DURATION = 0.20f;

    // Booster mod equip pop & jump animation
    private boolean isBoosterEquipping = false;
    private float boosterEquipTimer = 0f;
    private static final float BOOSTER_EQUIP_DURATION = 0.22f;

    public GameEngine(Context context) {
        this.context = context.getApplicationContext();
        this.soundManager = SoundManager.getInstance(context);
        this.grid = new BubbleGrid();
        this.board = new BubbleBoard(grid);
        this.scoreManager = new ScoreManager();
        this.comboManager = new ComboManager();
        this.confettiSystem = new ConfettiSystem();
        Bubble.initResources(this.context);
    }

    private float customTopMargin = -1f;
    private int lastViewWidth = 0;
    private int lastViewHeight = 0;

    public void setEventListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void setTopMargin(float topMarginPx) {
        this.customTopMargin = topMarginPx;
        if (lastViewWidth > 0 && lastViewHeight > 0) {
            setViewBounds(lastViewWidth, lastViewHeight);
        }
    }

    public void setViewBounds(int width, int height) {
        this.lastViewWidth = width;
        this.lastViewHeight = height;

        float density = context.getResources().getDisplayMetrics().density;
        float topMargin = (customTopMargin > 0) ? customTopMargin : (92f * density);

        // Tablet & wide screen optimization:
        // On phones (aspect ratio 9:16 to 9:21), board spans the full width.
        // On tablets / wide screens (4:3, 16:10 or landscape), constrain playfield board width
        // so bubbles maintain ideal crisp proportions and avoid taking over the entire screen.
        float maxBoardWidth = Math.min(width, Math.min(height * 0.62f, 560f * density));
        float boardWidth = Math.min(width, maxBoardWidth);

        this.boardLeft = (width - boardWidth) / 2.0f;
        this.boardRight = this.boardLeft + boardWidth;
        this.boardTop = topMargin;
        this.boardBottom = height;

        // 9 bubbles across on even row: boardWidth = 9 * 2 * radius = 18 * radius
        this.bubbleRadius = boardWidth / (BubbleGrid.COLS_EVEN * 2.0f);
        this.grid.setDimensions(boardLeft, boardTop, bubbleRadius);

        this.launcherX = boardLeft + (boardWidth * 0.5f);
        this.launcherY = height - (130f * density); // Elevated ~50-60dp above bottom booster bar
        this.previewX = launcherX - (bubbleRadius * 2.85f);
        this.previewY = launcherY + (bubbleRadius * 0.15f);
        this.deadlineY = launcherY - (bubbleRadius * 1.35f);
        this.normalDashEffect = new DashPathEffect(new float[]{16f, 12f}, 0);
        this.dangerDashEffect = new DashPathEffect(new float[]{18f, 8f}, 0);

        if (currentBubble != null) {
            currentBubble.setX(launcherX);
            currentBubble.setY(launcherY);
            currentBubble.setRadius(bubbleRadius);
            currentBubble.setScale(1.0f);
            currentBubble.setAlpha(1.0f);
        }
        if (nextBubble != null) {
            nextBubble.setX(previewX);
            nextBubble.setY(previewY);
            nextBubble.setRadius(bubbleRadius * 0.75f);
            nextBubble.setScale(1.0f);
            nextBubble.setAlpha(1.0f);
        }

        updateTrajectory();
    }

    public float getBoardTop() {
        return boardTop;
    }

    public float getBoardLeft() {
        return boardLeft;
    }

    public float getBoardRight() {
        return boardRight;
    }

    public float getBubbleRadius() {
        return bubbleRadius;
    }

    public void loadLevel(Level level) {
        this.isEndlessMode = false;
        this.currentLevel = level;
        this.initialShots = level.getMaxShots();
        this.shotsRemaining = level.getMaxShots();
        this.scoreManager.reset();
        this.scoreManager.setStarThresholds(level.getStarThresholds());
        this.comboManager.reset();
        this.poppingBubbles.clear();
        this.fallingBubbles.clear();
        this.floatingTexts.clear();
        this.confettiSystem.clear();
        this.grid.clear();
        this.state = GameState.READY;
        this.isLauncherReloading = false;
        if (level.getObjective() != null) {
            level.getObjective().resetProgress();
        }

        // Populate grid from level row strings
        List<String> rows = level.getRows();
        for (int r = 0; r < rows.size() && r < BubbleGrid.MAX_ROWS; r++) {
            String rowStr = rows.get(r);
            int maxCols = (r % 2 == 0) ? BubbleGrid.COLS_EVEN : BubbleGrid.COLS_ODD;
            for (int c = 0; c < rowStr.length() && c < maxCols; c++) {
                char ch = rowStr.charAt(c);
                if (ch == '.' || ch == ' ') continue;

                BubbleType bType = BubbleType.NORMAL;
                BubbleColor bColor;
                if (ch == 'X' || ch == 'x') {
                    bType = BubbleType.BOMB;
                    bColor = BubbleColor.BOMB;
                } else if (ch == '*') {
                    bType = BubbleType.RAINBOW;
                    bColor = BubbleColor.RAINBOW;
                } else if (ch == 'L' || ch == 'l') {
                    bType = BubbleType.LIGHTNING;
                    bColor = BubbleColor.LIGHTNING;
                } else if (ch == 'F' || ch == 'f') {
                    bType = BubbleType.FIREBALL;
                    bColor = BubbleColor.FIREBALL;
                } else if (ch == 'S' || ch == 's') {
                    bType = BubbleType.STONE;
                    bColor = BubbleColor.STONE;
                } else if (ch == 'T' || ch == 't') {
                    bType = BubbleType.TRANSPARENT;
                    bColor = BubbleColor.TRANSPARENT;
                } else {
                    bColor = BubbleColor.fromChar(ch);
                }

                if (bColor != BubbleColor.NONE) {
                    Bubble b = new Bubble(bColor, bType, new GridPosition(r, c));
                    grid.setBubble(r, c, b);
                }
            }
        }

        // Initialize launcher bubbles with smart frontier and danger-aware colors
        BubbleColor firstColor = pickSmartLauncherColor(null);
        this.currentBubble = new Bubble(firstColor, BubbleType.NORMAL, null);
        this.currentBubble.setX(launcherX);
        this.currentBubble.setY(launcherY);
        this.currentBubble.setRadius(bubbleRadius);
        this.currentBubble.setScale(1.0f);
        this.currentBubble.setAlpha(1.0f);

        if (isEndlessMode || shotsRemaining > 1) {
            BubbleColor secondColor = pickSmartLauncherColor(firstColor);
            this.nextBubble = new Bubble(secondColor, BubbleType.NORMAL, null);
            this.nextBubble.setX(previewX);
            this.nextBubble.setY(previewY);
            this.nextBubble.setRadius(bubbleRadius * 0.75f);
            this.nextBubble.setScale(1.0f);
            this.nextBubble.setAlpha(1.0f);
        } else {
            this.nextBubble = null;
        }

        if (listener != null) {
            listener.onScoreUpdated(scoreManager.getScore(), getEffectiveStars(), scoreManager.getStarProgress());
            listener.onShotsUpdated(shotsRemaining);
            notifyObjectiveUpdated();
        }


        updateTrajectory();
    }

    public void loadEndlessMode(int personalBestHighScore, List<BubbleColor> colorsPool) {
        loadEndlessMode(personalBestHighScore, null, colorsPool);
    }

    public void loadEndlessMode(int personalBestHighScore, int[] thresholds, List<BubbleColor> colorsPool) {
        this.isEndlessMode = true;
        this.hasCelebratedNewBest = false;
        this.currentLevel = null;
        this.initialShots = 0;
        this.endlessWaveCount = 1;
        this.endlessHighScore = personalBestHighScore;
        this.shotsRemaining = 999999;
        this.scoreManager.reset();
        if (thresholds != null && thresholds.length >= 3) {
            this.scoreManager.setStarThresholds(thresholds);
        } else if (personalBestHighScore > 0) {
            int t1 = Math.max(500, (int) (personalBestHighScore * 0.35f));
            int t2 = Math.max(1000, (int) (personalBestHighScore * 0.70f));
            int t3 = Math.max(1500, personalBestHighScore);
            this.scoreManager.setStarThresholds(new int[]{t1, t2, t3});
        } else {
            this.scoreManager.setStarThresholds(new int[]{1000, 2500, 5000});
        }
        this.comboManager.reset();
        this.poppingBubbles.clear();
        this.fallingBubbles.clear();
        this.floatingTexts.clear();
        this.confettiSystem.clear();
        this.pregeneratedRowQueue.clear();
        this.grid.clear();
        this.state = GameState.READY;
        this.isLauncherReloading = false;

        this.endlessColorsPool = (colorsPool != null && !colorsPool.isEmpty())
                ? new ArrayList<>(colorsPool)
                : EndlessPatternGenerator.getActiveColors(1, null);

        // Pre-fill buffer queue with 3 multi-row pattern chunks
        for (int i = 0; i < 3; i++) {
            int nextParity = (grid.getRowParity() ^ (i % 2)) & 1;
            List<BubbleColor> activeColors = EndlessPatternGenerator.getActiveColors(1, endlessColorsPool);
            pregeneratedRowQueue.add(EndlessPatternGenerator.generateRow(1, grid.getCols(nextParity), nextParity, activeColors, random));
        }

        // Populate initial board with 4 rows of patterned bubbles
        List<BubbleColor> initialActiveColors = EndlessPatternGenerator.getActiveColors(1, endlessColorsPool);
        EndlessPatternGenerator.populateInitialBoard(grid, 4, initialActiveColors, random);

        // Initialize launcher bubbles
        BubbleColor firstColor = pickSmartLauncherColor(null);
        this.currentBubble = new Bubble(firstColor, BubbleType.NORMAL, null);
        this.currentBubble.setX(launcherX);
        this.currentBubble.setY(launcherY);
        this.currentBubble.setRadius(bubbleRadius);
        this.currentBubble.setScale(1.0f);
        this.currentBubble.setAlpha(1.0f);

        BubbleColor secondColor = pickSmartLauncherColor(firstColor);
        this.nextBubble = new Bubble(secondColor, BubbleType.NORMAL, null);
        this.nextBubble.setX(previewX);
        this.nextBubble.setY(previewY);
        this.nextBubble.setRadius(bubbleRadius * 0.75f);
        this.nextBubble.setScale(1.0f);
        this.nextBubble.setAlpha(1.0f);

        // Introductory floating objective banner matching campaign mode aesthetics
        floatingTexts.add(new FloatingText("⚡ SURVIVE THE ENDLESS DESCENT", (boardLeft + boardRight) * 0.5f, boardTop + bubbleRadius * 3.5f, Color.parseColor("#FFF176"), 44f, 2.4f));

        if (listener != null) {
            listener.onScoreUpdated(scoreManager.getScore(), getEffectiveStars(), scoreManager.getStarProgress());
            listener.onShotsUpdated(endlessWaveCount);
            notifyObjectiveUpdated();
        }

        updateTrajectory();
    }

    private List<Bubble> generateEndlessRow() {
        if (pregeneratedRowQueue.size() < 2) {
            int nextParity = grid.getRowParity() ^ 1;
            if (!pregeneratedRowQueue.isEmpty()) {
                nextParity ^= (pregeneratedRowQueue.size() % 2);
            }
            List<BubbleColor> activeColors = EndlessPatternGenerator.getActiveColors(endlessWaveCount, endlessColorsPool);
            List<List<Bubble>> nextChunk = EndlessPatternGenerator.generateMultiRowChunk(
                    endlessWaveCount + pregeneratedRowQueue.size() + 1,
                    nextParity,
                    3,
                    activeColors,
                    random
            );
            pregeneratedRowQueue.addAll(nextChunk);
        }

        return pregeneratedRowQueue.pollFirst();
    }

    private BubbleColor pickRandomColor() {
        return pickSmartLauncherColor(currentBubble != null ? currentBubble.getColor() : null);
    }

    public Set<BubbleColor> getRequiredColors(BubbleColor launcherColor) {
        Set<BubbleColor> requiredColors = new LinkedHashSet<>();

        // 1. Collect all distinct normal bubble colors on the board grid
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null && !b.isPopping() && !b.isFalling()
                    && b.getType() == BubbleType.NORMAL
                    && b.getColor() != null && b.getColor() != BubbleColor.NONE) {
                requiredColors.add(b.getColor());
            }
        }

        // 2. If launcher has a color, include it as well (up to that color count)
        if (launcherColor != null && launcherColor != BubbleColor.NONE) {
            requiredColors.add(launcherColor);
        }

        return requiredColors;
    }

    private BubbleColor pickSmartLauncherColor(BubbleColor avoidColorIfPossible) {
        Set<BubbleColor> requiredSet = getRequiredColors(avoidColorIfPossible);

        // Fallback if no bubbles are present on the board or in launcher
        if (requiredSet.isEmpty()) {
            if (isEndlessMode) {
                List<BubbleColor> active = EndlessPatternGenerator.getActiveColors(endlessWaveCount, endlessColorsPool);
                return !active.isEmpty() ? active.get(random.nextInt(active.size())) : BubbleColor.RED;
            }
            if (currentLevel != null && !currentLevel.getAvailableColors().isEmpty()) {
                return currentLevel.getAvailableColors().get(random.nextInt(currentLevel.getAvailableColors().size()));
            }
            return BubbleColor.RED;
        }

        List<BubbleColor> requiredColors = new ArrayList<>(requiredSet);

        // 1. Identify all active normal bubbles on the board
        List<Bubble> allBubbles = new ArrayList<>();
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null && !b.isPopping() && !b.isFalling()
                    && b.getType() == BubbleType.NORMAL && b.getColor() != BubbleColor.NONE) {
                allBubbles.add(b);
            }
        }

        // 2. Identify exposed bottom frontier bubbles and lowest danger bubble
        List<Bubble> frontierBubbles = new ArrayList<>();
        Bubble lowestDangerBubble = null;
        float maxDangerY = -1f;

        for (Bubble b : allBubbles) {
            GridPosition pos = b.getGridPosition();
            if (pos == null) continue;

            boolean isBottomExposed = false;
            List<GridPosition> neighbors = NeighborCalculator.getNeighbors(pos, grid.getRowParity());
            for (GridPosition n : neighbors) {
                if (n.row > pos.row && grid.getBubble(n) == null) {
                    isBottomExposed = true;
                    break;
                }
            }
            if (pos.row == BubbleGrid.MAX_ROWS - 1 || isBottomExposed) {
                frontierBubbles.add(b);
            }

            if (deadlineY > 0 && (b.getY() + b.getRadius()) >= (deadlineY - bubbleRadius * 3.5f)) {
                if (b.getY() > maxDangerY) {
                    maxDangerY = b.getY();
                    lowestDangerBubble = b;
                }
            }
        }

        if (frontierBubbles.isEmpty()) {
            frontierBubbles = allBubbles;
        }

        // Priority 1: If in critical danger zone, 75% chance to give danger bubble's color (if in requiredColors)
        if (lowestDangerBubble != null && requiredColors.contains(lowestDangerBubble.getColor()) && random.nextInt(100) < 75) {
            return lowestDangerBubble.getColor();
        }

        // Priority 2: Look for match clusters on the exposed frontier (groups of 2+ connected same color)
        List<BubbleColor> matchableColors = new ArrayList<>();
        List<BubbleColor> frontierColors = new ArrayList<>();
        for (Bubble b : frontierBubbles) {
            BubbleColor c = b.getColor();
            if (requiredColors.contains(c) && !frontierColors.contains(c)) {
                frontierColors.add(c);
            }
            GridPosition pos = b.getGridPosition();
            if (pos != null) {
                for (GridPosition n : NeighborCalculator.getNeighbors(pos, grid.getRowParity())) {
                    Bubble nb = grid.getBubble(n);
                    if (nb != null && !nb.isPopping() && !nb.isFalling()
                            && nb.getColor() == c && requiredColors.contains(c) && !matchableColors.contains(c)) {
                        matchableColors.add(c);
                    }
                }
            }
        }

        // Select candidate pool strictly from required colors
        List<BubbleColor> candidatePool;
        if (!matchableColors.isEmpty() && random.nextInt(100) < 70) {
            candidatePool = new ArrayList<>(matchableColors);
        } else if (!frontierColors.isEmpty()) {
            candidatePool = new ArrayList<>(frontierColors);
        } else {
            candidatePool = new ArrayList<>(requiredColors);
        }

        // Enforce candidatePool strictly never contains any color outside requiredColors
        candidatePool.retainAll(requiredColors);
        if (candidatePool.isEmpty()) {
            candidatePool = new ArrayList<>(requiredColors);
        }

        // If possible, pick a color different from avoidColorIfPossible to provide shot versatility
        if (avoidColorIfPossible != null && candidatePool.size() > 1) {
            List<BubbleColor> diversePool = new ArrayList<>(candidatePool);
            diversePool.remove(avoidColorIfPossible);
            if (!diversePool.isEmpty() && random.nextInt(100) < 80) {
                return diversePool.get(random.nextInt(diversePool.size()));
            }
        }

        return candidatePool.get(random.nextInt(candidatePool.size()));
    }

    private void sanitizeNextBubble() {
        if (nextBubble == null || nextBubble.getType() != BubbleType.NORMAL) return;
        BubbleColor currColor = currentBubble != null ? currentBubble.getColor() : null;
        Set<BubbleColor> required = getRequiredColors(currColor);
        if (!required.isEmpty() && !required.contains(nextBubble.getColor())) {
            nextBubble.setColor(pickSmartLauncherColor(currColor));
        }
    }

    public void swapBubbles() {
        if (state != GameState.READY && state != GameState.AIMING) return;
        if (currentBubble == null || nextBubble == null) return;

        // Cancel other animations
        isLauncherReloading = false;
        isBoosterEquipping = false;

        // Swap logical colors and types
        BubbleColor tempColor = currentBubble.getColor();
        BubbleType tempType = currentBubble.getType();

        currentBubble.setColor(nextBubble.getColor());
        currentBubble.setType(nextBubble.getType());

        nextBubble.setColor(tempColor);
        nextBubble.setType(tempType);

        // Start swap jump animation:
        isSwapping = true;
        swapTimer = 0f;

        soundManager.playClick();
        updateTrajectory();
    }

    public void equipBooster(BubbleType type) {
        if (state != GameState.READY && state != GameState.AIMING) return;
        if (currentBubble == null) return;

        // Cancel other animations
        isLauncherReloading = false;
        isSwapping = false;

        currentBubble.setType(type);
        if (type == BubbleType.BOMB) {
            currentBubble.setColor(BubbleColor.BOMB);
        } else if (type == BubbleType.RAINBOW) {
            currentBubble.setColor(BubbleColor.RAINBOW);
        } else if (type == BubbleType.LIGHTNING) {
            currentBubble.setColor(BubbleColor.LIGHTNING);
        } else if (type == BubbleType.FIREBALL) {
            currentBubble.setColor(BubbleColor.FIREBALL);
        }

        // Start booster mod equip jump & pop animation
        isBoosterEquipping = true;
        boosterEquipTimer = 0f;

        // Sparkle burst particles around launcher base
        int particleColor = currentBubble.getColor().primaryColor;
        confettiSystem.spawnPopParticles(launcherX, launcherY, particleColor, 12);

        // Visual text feedback
        String boosterName = "BOOSTER!";
        int textColor = Color.parseColor("#FFD54F");
        if (type == BubbleType.BOMB) {
            boosterName = "BOMB!";
            textColor = Color.parseColor("#FF5722");
        } else if (type == BubbleType.RAINBOW) {
            boosterName = "RAINBOW!";
            textColor = Color.parseColor("#E040FB");
        } else if (type == BubbleType.LIGHTNING) {
            boosterName = "LIGHTNING!";
            textColor = Color.parseColor("#FFEB3B");
        } else if (type == BubbleType.FIREBALL) {
            boosterName = "FIREBALL!";
            textColor = Color.parseColor("#FF9800");
        }
        floatingTexts.add(new FloatingText(boosterName, launcherX, launcherY - bubbleRadius * 1.4f, textColor, 44f, 0.9f));

        soundManager.playClick();
        updateTrajectory();
    }

    public void onTouchDown(float touchX, float touchY) {
        if (state != GameState.READY && state != GameState.AIMING) return;

        // Check if user tapped preview bubble, launcher base, or the swap icon area to swap
        if (nextBubble != null) {
            float midX = (launcherX + previewX) / 2f;
            float midY = (launcherY + previewY) / 2f;
            float distToMid = (float) Math.hypot(touchX - midX, touchY - midY);
            float distToPreview = (float) Math.hypot(touchX - previewX, touchY - previewY);
            if (distToPreview <= bubbleRadius * 1.6f || distToMid <= bubbleRadius * 1.5f) {
                swapBubbles();
                return;
            }

            // Check if user tapped launcher
            float distToLauncher = (float) Math.hypot(touchX - launcherX, touchY - launcherY);
            if (distToLauncher <= bubbleRadius * 1.2f) {
                swapBubbles();
                return;
            }
        }

        if (currentBubble == null) return;

        // Imaginary cancel threshold line: slightly above the launcher
        float cancelThreshold = launcherY - (bubbleRadius * 0.4f);
        if (touchY < cancelThreshold) {
            state = GameState.AIMING;
            isAimCancelled = false;
            updateAimAngle(touchX, touchY);
        }
    }

    public void onTouchMove(float touchX, float touchY) {
        if (state == GameState.AIMING) {
            float cancelThreshold = launcherY - (bubbleRadius * 0.4f);
            if (touchY >= cancelThreshold) {
                // Aim dragged below imaginary line -> cancel shot and hide laser
                isAimCancelled = true;
                if (trajectoryPoints != null) {
                    trajectoryPoints.clear();
                }
            } else {
                isAimCancelled = false;
                updateAimAngle(touchX, touchY);
            }
        }
    }

    public void onTouchUp(float touchX, float touchY) {
        if (state == GameState.AIMING) {
            float cancelThreshold = launcherY - (bubbleRadius * 0.4f);
            if (isAimCancelled || touchY >= cancelThreshold || isFireballBlocked) {
                // Canceled shot: reset to READY, clear trajectory, do NOT launch projectile
                state = GameState.READY;
                isAimCancelled = false;
                if (isFireballBlocked) {
                    soundManager.playBounce();
                    isFireballBlocked = false;
                }
                if (trajectoryPoints != null) {
                    trajectoryPoints.clear();
                }
            } else {
                shoot();
            }
        }
    }

    private void updateAimAngle(float touchX, float touchY) {
        float dx = touchX - launcherX;
        float dy = touchY - launcherY;
        float angle = (float) Math.atan2(dy, dx);

        // Clamp aiming angle: must shoot upward
        // Range: -168 degrees (-2.93 rad) to -12 degrees (-0.21 rad)
        float minAngle = (float) (-Math.PI + 0.20);
        float maxAngle = -0.20f;

        if (angle < minAngle) angle = minAngle;
        if (angle > maxAngle && angle < 0) angle = maxAngle;
        if (angle >= 0) {
            angle = (dx < 0) ? minAngle : maxAngle;
        }

        this.aimAngleRad = angle;
        updateTrajectory();
    }

    // Fireball piercing trajectory tracking
    private final Set<GridPosition> fireballPoppedPositions = new HashSet<>();

    private void updateTrajectory() {
        if (currentBubble == null) {
            if (trajectoryPoints != null) {
                trajectoryPoints.clear();
            }
            isFireballBlocked = false;
            return;
        }

        boolean isFireball = (currentBubble.getType() == BubbleType.FIREBALL
                || currentBubble.getColor() == BubbleColor.FIREBALL);

        int maxBounces = isFireball ? 1 : TrajectoryCalculator.MAX_BOUNCES;

        TrajectoryCalculator.TrajectoryResult result = TrajectoryCalculator.calculateTrajectory(
                launcherX, launcherY, aimAngleRad,
                boardLeft, boardRight, boardTop,
                grid, bubbleRadius, false, maxBounces
        );

        this.trajectoryPoints = result.points;
        this.isFireballBlocked = isFireball && result.bounceLimitExceeded;
    }

    private void shoot() {
        if (currentBubble == null || shotsRemaining <= 0) return;

        // Finish any active swap or booster animation immediately
        if (isSwapping) {
            isSwapping = false;
            if (currentBubble != null) {
                currentBubble.setX(launcherX);
                currentBubble.setY(launcherY);
                currentBubble.setScale(1.0f);
            }
            if (nextBubble != null) {
                nextBubble.setX(previewX);
                nextBubble.setY(previewY);
                nextBubble.setScale(1.0f);
            }
        }
        if (isBoosterEquipping) {
            isBoosterEquipping = false;
            if (currentBubble != null) {
                currentBubble.setX(launcherX);
                currentBubble.setY(launcherY);
                currentBubble.setScale(1.0f);
            }
        }

        state = GameState.SHOOTING;
        fireballPoppedPositions.clear();
        activeProjectile = new BubbleProjectile(currentBubble.getColor(), currentBubble.getType(), bubbleRadius);
        float dirX = (float) Math.cos(aimAngleRad);
        float dirY = (float) Math.sin(aimAngleRad);
        activeProjectile.launch(launcherX, launcherY, dirX, dirY);

        soundManager.playShoot();

        // 1. Promote queued bubble into currentBubble, starting at preview position
        if (nextBubble != null) {
            currentBubble.setColor(nextBubble.getColor());
            currentBubble.setType(nextBubble.getType());
            currentBubble.setRadius(bubbleRadius);
            currentBubble.setX(previewX);
            currentBubble.setY(previewY);
            currentBubble.setScale(0.75f);
            currentBubble.setAlpha(1.0f);

            // Reserve check: (shotsRemaining - 2) > 0 because this shot (1) is in flight,
            // currentBubble holds 1, leaving (shotsRemaining - 2) in reserve for nextBubble refill
            if (isEndlessMode || (shotsRemaining - 2) > 0) {
                // 2. Pick a new smart nextBubble and prepare it to pop into the preview position
                nextBubble.setColor(pickSmartLauncherColor(currentBubble.getColor()));
                nextBubble.setType(BubbleType.NORMAL);
                nextBubble.setRadius(bubbleRadius * 0.75f);
                nextBubble.setX(previewX);
                nextBubble.setY(previewY);
                nextBubble.setScale(0.0f);
                nextBubble.setAlpha(0.0f);
            } else {
                // Refiller ball is now empty
                nextBubble = null;
            }

            // 3. Trigger jump & pop reload animation
            isLauncherReloading = true;
            reloadTimer = 0f;
        } else {
            // Final shot was fired — launcher is now empty
            currentBubble = null;
            isLauncherReloading = false;
        }
    }

    public void update(float dt) {
        if (state == GameState.PAUSED) {
            return;
        }

        dangerPulseTimer += dt;

        // 0. Update launcher reload jump & pop-in animation
        if (isLauncherReloading) {
            reloadTimer += dt;
            float t = Math.min(1.0f, reloadTimer / RELOAD_DURATION);

            // Parabolic hop arc from preview to launcher
            float hopHeight = bubbleRadius * 0.70f;
            float hop = (float) Math.sin(t * Math.PI) * hopHeight;

            float curX = previewX + (launcherX - previewX) * t;
            float curY = previewY + (launcherY - previewY) * t - hop;
            float curScale = 0.75f + 0.25f * t;

            if (currentBubble != null) {
                currentBubble.setX(curX);
                currentBubble.setY(curY);
                currentBubble.setScale(curScale);
            }

            // Pop-in with elastic bounce for new queued bubble
            float popT = Math.max(0f, (reloadTimer - 0.03f) / (RELOAD_DURATION * 0.85f));
            popT = Math.min(1.0f, popT);
            float p = popT - 1.0f;
            float popScale = (popT == 0f) ? 0f : (p * p * (2.2f * p + 1.2f) + 1.0f);

            if (nextBubble != null) {
                nextBubble.setScale(Math.max(0f, popScale));
                nextBubble.setAlpha(Math.min(1.0f, popT * 2.5f));
            }

            if (t >= 1.0f) {
                isLauncherReloading = false;
                if (currentBubble != null) {
                    currentBubble.setX(launcherX);
                    currentBubble.setY(launcherY);
                    currentBubble.setScale(1.0f);
                    currentBubble.setAlpha(1.0f);
                }
                if (nextBubble != null) {
                    nextBubble.setX(previewX);
                    nextBubble.setY(previewY);
                    nextBubble.setScale(1.0f);
                    nextBubble.setAlpha(1.0f);
                }
            }
        }

        // 0.1 Update launcher swap jump animation
        if (isSwapping) {
            swapTimer += dt;
            float t = Math.min(1.0f, swapTimer / SWAP_DURATION);

            // currentBubble hops OVER top from preview to launcher
            float hopUp = (float) Math.sin(t * Math.PI) * (bubbleRadius * 0.65f);
            float curX = previewX + (launcherX - previewX) * t;
            float curY = previewY + (launcherY - previewY) * t - hopUp;
            float curScale = 0.75f + 0.25f * t;

            if (currentBubble != null) {
                currentBubble.setX(curX);
                currentBubble.setY(curY);
                currentBubble.setScale(curScale);
                currentBubble.setAlpha(1.0f);
            }

            // nextBubble dips UNDER from launcher to preview
            float dipDown = (float) Math.sin(t * Math.PI) * (bubbleRadius * 0.45f);
            float nxtX = launcherX + (previewX - launcherX) * t;
            float nxtY = launcherY + (previewY - launcherY) * t + dipDown;
            float nxtScale = 1.333f - 0.333f * t;

            if (nextBubble != null) {
                nextBubble.setX(nxtX);
                nextBubble.setY(nxtY);
                nextBubble.setScale(nxtScale);
                nextBubble.setAlpha(1.0f);
            }

            if (t >= 1.0f) {
                isSwapping = false;
                if (currentBubble != null) {
                    currentBubble.setX(launcherX);
                    currentBubble.setY(launcherY);
                    currentBubble.setScale(1.0f);
                    currentBubble.setAlpha(1.0f);
                }
                if (nextBubble != null) {
                    nextBubble.setX(previewX);
                    nextBubble.setY(previewY);
                    nextBubble.setScale(1.0f);
                    nextBubble.setAlpha(1.0f);
                }
            }
        }

        // 0.2 Update booster mod equip pop & jump animation
        if (isBoosterEquipping) {
            boosterEquipTimer += dt;
            float t = Math.min(1.0f, boosterEquipTimer / BOOSTER_EQUIP_DURATION);

            // Elastic pop scale: 0.3 -> 1.25 -> 1.0
            float hop = (float) Math.sin(t * Math.PI) * (bubbleRadius * 0.45f);
            float curY = launcherY - hop;
            float p = t - 1.0f;
            float scale = (p * p * (2.4f * p + 1.4f) + 1.0f);
            scale = Math.max(0.3f, scale);

            if (currentBubble != null) {
                currentBubble.setX(launcherX);
                currentBubble.setY(curY);
                currentBubble.setScale(scale);
                currentBubble.setAlpha(Math.min(1.0f, t * 3.0f));
            }

            if (t >= 1.0f) {
                isBoosterEquipping = false;
                if (currentBubble != null) {
                    currentBubble.setX(launcherX);
                    currentBubble.setY(launcherY);
                    currentBubble.setScale(1.0f);
                    currentBubble.setAlpha(1.0f);
                }
            }
        }

        // 1. Update board grid (smooth 60 FPS unified descent animation) and bubbles
        grid.update(dt);
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null) {
                b.update(dt);
            }
        }

        // 1.1 Update visual particles and texts
        confettiSystem.update(dt);

        Iterator<FloatingText> textIt = floatingTexts.iterator();
        while (textIt.hasNext()) {
            FloatingText ft = textIt.next();
            ft.update(dt);
            if (!ft.isAlive()) textIt.remove();
        }

        Iterator<Bubble> popIt = poppingBubbles.iterator();
        while (popIt.hasNext()) {
            Bubble b = popIt.next();
            b.update(dt);
            if (b.getPopProgress() >= 1.0f) popIt.remove();
        }

        Iterator<Bubble> fallIt = fallingBubbles.iterator();
        while (fallIt.hasNext()) {
            Bubble b = fallIt.next();
            b.update(dt);
            if (b.getY() > boardBottom + bubbleRadius * 2) {
                fallIt.remove();
            }
        }

        // 2. Projectile Movement and Collision
        if (state == GameState.SHOOTING && activeProjectile != null) {
            float prevX = activeProjectile.getX();
            float prevY = activeProjectile.getY();

            activeProjectile.update(dt);

            float currX = activeProjectile.getX();
            float currY = activeProjectile.getY();

            boolean isFireball = (activeProjectile.getType() == BubbleType.FIREBALL
                    || activeProjectile.getColor() == BubbleColor.FIREBALL);

            int maxBounces = isFireball ? 1 : -1;
            boolean bounced = WallBounceCalculator.checkAndHandleWallBounce(activeProjectile, boardLeft, boardRight, maxBounces);
            if (bounced) {
                soundManager.playBounce();
            } else if (isFireball && activeProjectile.getBounceCount() >= 1) {
                float r = activeProjectile.getRadius();
                float minX = boardLeft + r;
                float maxX = boardRight - r;
                boolean hitLeft = (activeProjectile.getX() <= minX && activeProjectile.getVx() < 0);
                boolean hitRight = (activeProjectile.getX() >= maxX && activeProjectile.getVx() > 0);
                if (hitLeft || hitRight) {
                    finishFireballFlight();
                }
            }

            if (isFireball) {
                if (state != GameState.RESOLVING) {
                    // Fireball penetrates along its trajectory line, incinerating visible bubbles in its path
                    handleFireballPiercing(prevX, prevY, currX, currY);

                    // Check ceiling strike
                    if (currY - activeProjectile.getRadius() <= boardTop) {
                        finishFireballFlight();
                    }
                }
            } else {
                CollisionDetector.CollisionResult collision =
                        CollisionDetector.checkCollision(activeProjectile, board, boardTop);

                if (collision.collided) {
                    resolveCollision(collision.snapPosition);
                }
            }
        }

        // 3. Resolving Timer
        if (state == GameState.RESOLVING) {
            resolveTimer -= dt;
            if (resolveTimer <= 0f) {
                finishResolution();
            }
        }
    }

    private void resolveCollision(GridPosition snapPos) {
        state = GameState.RESOLVING;
        resolveTimer = 0.28f; // time for pop/fall animation to start

        if (snapPos == null) {
            // Fallback: nearest position in row 0
            snapPos = board.findNearestSnapPosition(activeProjectile.getX(), boardTop + bubbleRadius);
        }

        if (snapPos != null) {
            Bubble snapped = new Bubble(activeProjectile.getColor(), activeProjectile.getType(), snapPos);
            grid.setBubble(snapPos, snapped);

            // Evaluate matches
            List<GridPosition> matches = board.findMatches(snapPos);
            if (!matches.isEmpty()) {
                boolean hadBomb = (activeProjectile.getType() == BubbleType.BOMB);
                if (!hadBomb) {
                    for (GridPosition pos : matches) {
                        Bubble b = grid.getBubble(pos);
                        if (b != null && (b.getType() == BubbleType.BOMB || b.getColor() == BubbleColor.BOMB)) {
                            hadBomb = true;
                            break;
                        }
                    }
                }

                if (hadBomb) {
                    soundManager.playBomb();
                } else {
                    soundManager.playPop(comboManager.getStreak());
                }

                // Remove and animate matched bubbles
                for (GridPosition pos : matches) {
                    Bubble popped = grid.removeBubble(pos);
                    if (popped != null) {
                        popped.startPop();
                        poppingBubbles.add(popped);
                        int particleColor;
                        if (popped.getColor() == BubbleColor.BOMB || popped.getType() == BubbleType.BOMB) {
                            particleColor = Color.parseColor("#FF6D00");
                        } else if (popped.getColor() == BubbleColor.TRANSPARENT || popped.getType() == BubbleType.TRANSPARENT) {
                            particleColor = Color.parseColor("#E0F7FA");
                        } else {
                            particleColor = popped.getColor().primaryColor;
                        }
                        confettiSystem.spawnPopParticles(popped.getX(), popped.getY(), particleColor, hadBomb ? 20 : 12);
                    }
                }

                // Identify floating bubbles
                List<Bubble> floating = board.findFloatingBubbles();
                for (Bubble fb : floating) {
                    float vx = (float) ((Math.random() - 0.5) * 450.0);
                    float vy = (float) (-150 - Math.random() * 200.0);
                    fb.startFalling(vx, vy);
                    fallingBubbles.add(fb);
                }

                // Update combo and scoring
                comboManager.registerSuccess();
                int multiplier = comboManager.getMultiplier();
                int popScore = scoreManager.addPoppedBubbles(matches.size(), multiplier);
                int dropScore = scoreManager.addDroppedBubbles(floating.size(), multiplier);
                int totalTurnScore = popScore + dropScore;

                // Floating text feedback
                float textX = grid.getCenterX(snapPos.row, snapPos.col);
                float textY = grid.getCenterY(snapPos.row);
                if (hadBomb) {
                    floatingTexts.add(new FloatingText("BOOM! +" + totalTurnScore, textX, textY, Color.parseColor("#FF5722"), 48f, 1.3f));
                } else {
                    floatingTexts.add(new FloatingText("+" + totalTurnScore, textX, textY, Color.parseColor("#FFF176"), 42f, 1.1f));
                }

                String praise = comboManager.getPraiseText();
                if (!praise.isEmpty()) {
                    floatingTexts.add(new FloatingText(praise, textX, textY - 60f, Color.parseColor("#FF4081"), 48f, 1.4f));
                }

                // Update objective progress
                if (currentLevel != null) {
                    LevelObjective obj = currentLevel.getObjective();
                    if (obj.getType() == LevelObjective.Type.DROP_COUNT) {
                        obj.addProgress(floating.size());
                    } else if (obj.getType() == LevelObjective.Type.POP_COLOR) {
                        int matchedTargetColor = 0;
                        for (GridPosition pos : matches) {
                            if (activeProjectile.getColor() == obj.getTargetColor()) matchedTargetColor++;
                        }
                        obj.addProgress(matchedTargetColor);
                    }
                }

            } else {
                // Miss
                comboManager.registerMiss();
                soundManager.playBounce();
            }
        }

        checkEndlessAesthetics();

        if (isEndlessMode) {
            endlessWaveCount++;
            List<Bubble> newRow = generateEndlessRow();
            grid.shiftDownAndInsertRow(newRow);

            if (listener != null) {
                listener.onScoreUpdated(scoreManager.getScore(), getEffectiveStars(), scoreManager.getStarProgress());
                listener.onShotsUpdated(endlessWaveCount);
            }
        } else {
            shotsRemaining--;
            if (listener != null) {
                listener.onScoreUpdated(scoreManager.getScore(), getEffectiveStars(), scoreManager.getStarProgress());
                listener.onShotsUpdated(shotsRemaining);
            }
        }

        notifyObjectiveUpdated();
        activeProjectile = null;
    }

    private void handleFireballPiercing(float prevX, float prevY, float currX, float currY) {
        float hitThresholdSq = (bubbleRadius * 1.85f) * (bubbleRadius * 1.85f);
        List<GridPosition> newHits = new ArrayList<>();
        Set<GridPosition> bombsToExplode = new HashSet<>();
        Set<GridPosition> lightningToTrigger = new HashSet<>();

        for (int r = 0; r < BubbleGrid.MAX_ROWS; r++) {
            int cols = grid.getCols(r);
            for (int c = 0; c < cols; c++) {
                Bubble b = grid.getBubble(r, c);
                if (b != null && !b.isPopping() && !b.isFalling()) {
                    // Only visible bubbles inside the active playing field
                    if (isBubbleVisibleOnBoard(b)) {
                        float bx = b.getX();
                        float by = b.getY();
                        float dSq = distanceSqToSegment(bx, by, prevX, prevY, currX, currY);
                        if (dSq <= hitThresholdSq) {
                            GridPosition pos = new GridPosition(r, c);
                            newHits.add(pos);
                            if (b.getType() == BubbleType.BOMB || b.getColor() == BubbleColor.BOMB) {
                                bombsToExplode.add(pos);
                            } else if (b.getType() == BubbleType.LIGHTNING || b.getColor() == BubbleColor.LIGHTNING) {
                                lightningToTrigger.add(pos);
                            }
                        }
                    }
                }
            }
        }

        // Secondary chain reactions from hit specials
        if (!bombsToExplode.isEmpty()) {
            newHits.addAll(board.getBombExplosionPositions(bombsToExplode));
        }
        if (!lightningToTrigger.isEmpty()) {
            newHits.addAll(board.getLightningExplosionPositions(lightningToTrigger, null));
        }

        boolean poppedAny = false;
        for (GridPosition pos : newHits) {
            if (!fireballPoppedPositions.contains(pos)) {
                fireballPoppedPositions.add(pos);
                Bubble popped = grid.removeBubble(pos);
                if (popped != null) {
                    popped.startPop();
                    poppingBubbles.add(popped);
                    int pColor;
                    if (popped.getColor() == BubbleColor.BOMB || popped.getType() == BubbleType.BOMB) {
                        pColor = Color.parseColor("#FF6D00");
                    } else if (popped.getColor() == BubbleColor.LIGHTNING || popped.getType() == BubbleType.LIGHTNING) {
                        pColor = Color.parseColor("#FFEB3B");
                    } else if (popped.getColor() == BubbleColor.TRANSPARENT || popped.getType() == BubbleType.TRANSPARENT) {
                        pColor = Color.parseColor("#E0F7FA");
                    } else {
                        pColor = Color.parseColor("#FF5722");
                    }
                    confettiSystem.spawnPopParticles(popped.getX(), popped.getY(), pColor, 16);
                    poppedAny = true;
                }
            }
        }

        if (poppedAny) {
            soundManager.playPop(1);
        }
    }

    private void finishFireballFlight() {
        state = GameState.RESOLVING;
        resolveTimer = 0.28f;

        if (activeProjectile != null) {
            float burstY = Math.max(boardTop + bubbleRadius, activeProjectile.getY());
            confettiSystem.spawnCelebrationBurst(activeProjectile.getX(), burstY, 30);
        }
        soundManager.playBomb();

        // 1. Identify and drop unsupported floating bubbles
        List<Bubble> floating = board.findFloatingBubbles();
        for (Bubble fb : floating) {
            float vx = (float) ((Math.random() - 0.5) * 450.0);
            float vy = (float) (-150 - Math.random() * 200.0);
            fb.startFalling(vx, vy);
            fallingBubbles.add(fb);
        }

        // 2. Combo & Score calculation
        comboManager.registerSuccess();
        int multiplier = comboManager.getMultiplier();
        int popCount = fireballPoppedPositions.size();
        int popScore = scoreManager.addPoppedBubbles(Math.max(1, popCount), multiplier);
        int dropScore = scoreManager.addDroppedBubbles(floating.size(), multiplier);
        int totalTurnScore = popScore + dropScore;

        float textX = (activeProjectile != null) ? activeProjectile.getX() : (boardLeft + boardRight) * 0.5f;
        float textY = boardTop + bubbleRadius * 2.5f;
        floatingTexts.add(new FloatingText("FIREBURST! +" + totalTurnScore, textX, textY, Color.parseColor("#FF5722"), 48f, 1.4f));

        String praise = comboManager.getPraiseText();
        if (!praise.isEmpty()) {
            floatingTexts.add(new FloatingText(praise, textX, textY - 60f, Color.parseColor("#FF4081"), 48f, 1.4f));
        }

        // 3. Update objective progress
        if (currentLevel != null) {
            LevelObjective obj = currentLevel.getObjective();
            if (obj.getType() == LevelObjective.Type.DROP_COUNT) {
                obj.addProgress(floating.size());
            }
        }

        checkEndlessAesthetics();

        // 4. Wave / Shot progression
        if (isEndlessMode) {
            endlessWaveCount++;
            List<Bubble> newRow = generateEndlessRow();
            grid.shiftDownAndInsertRow(newRow);

            if (listener != null) {
                listener.onScoreUpdated(scoreManager.getScore(), getEffectiveStars(), scoreManager.getStarProgress());
                listener.onShotsUpdated(endlessWaveCount);
            }
        } else {
            shotsRemaining--;
            if (listener != null) {
                listener.onScoreUpdated(scoreManager.getScore(), getEffectiveStars(), scoreManager.getStarProgress());
                listener.onShotsUpdated(shotsRemaining);
            }
        }

        notifyObjectiveUpdated();
        activeProjectile = null;
        fireballPoppedPositions.clear();
    }

    private void checkEndlessAesthetics() {
        if (isEndlessMode && endlessHighScore > 0 && !hasCelebratedNewBest && scoreManager.getScore() > endlessHighScore) {
            hasCelebratedNewBest = true;
            soundManager.playWin();
            floatingTexts.add(new FloatingText("🎉 NEW BEST SCORE!", (boardLeft + boardRight) * 0.5f, boardTop + bubbleRadius * 3.2f, Color.parseColor("#4ADE80"), 48f, 2.2f));
            confettiSystem.spawnCelebrationBurst(boardRight, boardBottom, 35);
        }
    }



    private boolean isBubbleVisibleOnBoard(Bubble b) {
        if (b == null) return false;
        float y = b.getY();
        float x = b.getX();
        float r = b.getRadius();
        return (y + r * 0.4f >= boardTop) && (y - r <= boardBottom) && (x + r >= boardLeft) && (x - r <= boardRight);
    }

    private static float distanceSqToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float l2 = dx * dx + dy * dy;
        if (l2 == 0f) {
            return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        }
        float t = Math.max(0f, Math.min(1f, ((px - x1) * dx + (py - y1) * dy) / l2));
        float projX = x1 + t * dx;
        float projY = y1 + t * dy;
        return (px - projX) * (px - projX) + (py - projY) * (py - projY);
    }

    private void finishResolution() {
        if (isEndlessMode) {
            // If board is wiped clean in Endless Mode, give big bonus and refill top rows
            if (grid.getBubbleCount() == 0) {
                soundManager.playWin();
                confettiSystem.spawnCelebrationBurst(boardRight, boardBottom, 50);
                scoreManager.addScore(500);
                floatingTexts.add(new FloatingText("BOARD CLEARED! +500", (boardLeft + boardRight) * 0.5f, boardTop + bubbleRadius * 3, Color.parseColor("#FFD54F"), 52f, 1.5f));
                List<BubbleColor> activeColors = EndlessPatternGenerator.getActiveColors(endlessWaveCount, endlessColorsPool);
                EndlessPatternGenerator.populateInitialBoard(grid, 4, activeColors, random);
            }

            // Check danger line breach
            boolean touchedBottomLine = false;
            if (deadlineY > 0) {
                for (Bubble b : grid.getAllBubbles()) {
                    if (b != null && (b.getY() + b.getRadius()) >= (deadlineY - 2.0f)) {
                        touchedBottomLine = true;
                        break;
                    }
                }
            }

            if (touchedBottomLine) {
                state = GameState.LOSE;
                if (listener != null) {
                    listener.onGameLost(scoreManager.getScore(), "The bubbles breached the danger line!");
                }
                return;
            }

            sanitizeNextBubble();
            state = GameState.READY;
            updateTrajectory();
            return;
        }

        // Check win condition
        boolean won = false;
        if (currentLevel != null && currentLevel.getObjective().isMet(board, scoreManager)) {
            won = true;
        } else if (grid.getBubbleCount() == 0) {
            won = true;
        }

        if (won) {
            state = GameState.WIN;
            soundManager.playWin();
            confettiSystem.spawnCelebrationBurst(boardRight, boardBottom, 70);
            int shotsLeft = Math.max(0, shotsRemaining);
            int shotBonus = shotsLeft * ScoreManager.REMAINING_SHOT_BONUS;
            int victoryBonus = scoreManager.addVictoryBonus(shotsRemaining);
            int finalScore = scoreManager.getScore();
            int starsEarned = isEndlessMode
                    ? Math.max(1, scoreManager.getStarsEarned())
                    : scoreManager.calculateStars(shotsRemaining, initialShots);
            if (shotsLeft > 0 && !isEndlessMode) {
                floatingTexts.add(new FloatingText("+" + shotBonus + " SHOT BONUS!", launcherX, launcherY - bubbleRadius * 1.6f, Color.parseColor("#FFD54F"), 44f, 2.2f));
            }
            if (listener != null) {
                listener.onScoreUpdated(finalScore, starsEarned, scoreManager.getStarProgress());
                listener.onGameWon(finalScore, starsEarned, getObjectiveCompletedSummary(), shotsLeft, shotBonus);
            }
            return;
        }

        // Check lose condition 1: Bubbles crossed or touched bottom deadline line
        boolean touchedBottomLine = false;
        if (deadlineY > 0) {
            for (Bubble b : grid.getAllBubbles()) {
                if (b != null && (b.getY() + b.getRadius()) >= (deadlineY - 2.0f)) {
                    touchedBottomLine = true;
                    break;
                }
            }
        }

        if (touchedBottomLine) {
            state = GameState.LOSE;
            if (listener != null) {
                listener.onGameLost(scoreManager.getScore(), "Bubbles reached the danger line!");
            }
            return;
        }

        // Check lose condition 2: Out of shots
        if (shotsRemaining <= 0) {
            state = GameState.LOSE;
            currentBubble = null;
            nextBubble = null;
            if (listener != null) {
                listener.onGameLost(scoreManager.getScore(), "Out of shots! Don't give up!");
            }
            return;
        }

        sanitizeNextBubble();
        state = GameState.READY;
        updateTrajectory();
    }

    public void draw(Canvas canvas, Paint paint) {
        // 0. Draw Bottom Danger Deadline
        drawDeadLine(canvas, paint);

        // 1. Draw Bold Shining Colored Laser Trajectory Line (ONLY when actively AIMING and not canceled)
        if (state == GameState.AIMING && !isAimCancelled && trajectoryPoints != null && !trajectoryPoints.isEmpty()) {
            laserPath.rewind();
            laserPath.moveTo(launcherX, launcherY);
            for (PointF pt : trajectoryPoints) {
                laserPath.lineTo(pt.x, pt.y);
            }

            int laserColor = (currentBubble != null) ? currentBubble.getColor().primaryColor : Color.parseColor("#4FC3F7");
            int glowColor = (currentBubble != null) ? currentBubble.getColor().lightColor : Color.WHITE;

            if (isFireballBlocked) {
                laserColor = Color.parseColor("#EF4444");
                glowColor = Color.parseColor("#FF5252");
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            // Layer 1: Broad Shining Outer Glow / Shadow
            paint.setColor(glowColor);
            paint.setAlpha(70);
            paint.setStrokeWidth(bubbleRadius * 0.48f);
            canvas.drawPath(laserPath, paint);

            // Layer 2: Bold Colored Laser Line
            paint.setColor(laserColor);
            paint.setAlpha(230);
            paint.setStrokeWidth(bubbleRadius * 0.22f);
            canvas.drawPath(laserPath, paint);

            // Layer 3: Intense White Core Beam
            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
            paint.setStrokeWidth(bubbleRadius * 0.09f);
            canvas.drawPath(laserPath, paint);

            paint.setStyle(Paint.Style.FILL);

            // If fireball trajectory is blocked by exceeding 1 bounce, draw red X on the head
            if (isFireballBlocked && !trajectoryPoints.isEmpty()) {
                PointF headPt = trajectoryPoints.get(trajectoryPoints.size() - 1);
                drawBlockedHeadX(canvas, paint, headPt.x, headPt.y);
            }
        }

        // 2. Draw Board Bubbles
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null) {
                b.draw(canvas, paint);
            }
        }

        // 3. Draw Popping and Falling Bubbles
        for (Bubble b : poppingBubbles) {
            b.draw(canvas, paint);
        }
        for (Bubble b : fallingBubbles) {
            b.draw(canvas, paint);
        }

        // 4. Draw Projectile
        if (activeProjectile != null) {
            activeProjectile.draw(canvas, paint);
        }

        // 5. Draw Launcher Base & Bubbles
        drawLauncher(canvas, paint);

        // 6. Draw Particles and Confetti
        confettiSystem.draw(canvas, paint);

        // 7. Draw Floating Texts
        for (FloatingText ft : floatingTexts) {
            ft.draw(canvas, paint);
        }
    }

    private void drawDeadLine(Canvas canvas, Paint paint) {
        if (deadlineY <= 0) return;

        float lowestBubbleBottom = -1f;
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null) {
                float bBottom = b.getY() + b.getRadius();
                if (bBottom > lowestBubbleBottom) {
                    lowestBubbleBottom = bBottom;
                }
            }
        }

        boolean inDanger = (lowestBubbleBottom > 0 && (deadlineY - lowestBubbleBottom) <= (bubbleRadius * 2.5f));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        if (inDanger) {
            // Warning pulsation
            float pulse = (float) (0.55 + 0.45 * Math.sin(dangerPulseTimer * 10.0));

            // Outer warning glow
            paint.setPathEffect(null);
            paint.setColor(Color.parseColor("#FF1744"));
            paint.setAlpha((int) (pulse * 85));
            paint.setStrokeWidth(bubbleRadius * 0.38f);
            canvas.drawLine(boardLeft, deadlineY, boardRight, deadlineY, paint);

            // Dashed Danger Line
            paint.setPathEffect(dangerDashEffect != null ? dangerDashEffect : new DashPathEffect(new float[]{18f, 8f}, 0));
            paint.setColor(Color.parseColor("#FF5252"));
            paint.setAlpha((int) (160 + pulse * 95));
            paint.setStrokeWidth(4.5f);
            canvas.drawLine(boardLeft, deadlineY, boardRight, deadlineY, paint);
            paint.setPathEffect(null);

            // Small pulsing Danger Tag
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#FF5252"));
            paint.setAlpha((int) (180 + pulse * 75));
            paint.setTextSize(bubbleRadius * 0.34f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⚠ DANGER LINE", (boardLeft + boardRight) * 0.5f, deadlineY - 8f, paint);
        } else {
            // Calm subtle dashed guideline
            paint.setPathEffect(normalDashEffect != null ? normalDashEffect : new DashPathEffect(new float[]{16f, 12f}, 0));
            paint.setColor(Color.WHITE);
            paint.setAlpha(45);
            paint.setStrokeWidth(2.5f);
            canvas.drawLine(boardLeft, deadlineY, boardRight, deadlineY, paint);
            paint.setPathEffect(null);
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBlockedHeadX(Canvas canvas, Paint paint, float hx, float hy) {
        // Dark crimson circular backer
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#450A0A"));
        paint.setAlpha(220);
        float radius = bubbleRadius * 0.55f;
        canvas.drawCircle(hx, hy, radius, paint);

        // Warning red border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#EF4444"));
        paint.setStrokeWidth(bubbleRadius * 0.08f);
        canvas.drawCircle(hx, hy, radius, paint);

        // Bold Red 'X' cross
        paint.setColor(Color.parseColor("#FF1744"));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(bubbleRadius * 0.18f);
        float arm = bubbleRadius * 0.28f;
        canvas.drawLine(hx - arm, hy - arm, hx + arm, hy + arm, paint);
        canvas.drawLine(hx - arm, hy + arm, hx + arm, hy - arm, paint);

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLauncher(Canvas canvas, Paint paint) {
        // Launcher Stand / Pedestal (cute wooden / metallic casual design)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#424242"));
        canvas.drawCircle(launcherX, launcherY, bubbleRadius * 1.35f, paint);

        paint.setColor(Color.parseColor("#FFD54F"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(bubbleRadius * 0.18f);
        canvas.drawCircle(launcherX, launcherY, bubbleRadius * 1.3f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Preview Bubble Pedestal (metallic rim + recessed tray)
        paint.setColor(Color.parseColor("#424242"));
        canvas.drawCircle(previewX, previewY, bubbleRadius * 0.95f, paint);
        paint.setColor(Color.parseColor("#263238"));
        canvas.drawCircle(previewX, previewY, bubbleRadius * 0.82f, paint);

        // Preview Bubble & Current Bubble (drawn with natural depth during swap)
        if (nextBubble != null) {
            nextBubble.draw(canvas, paint);
        }
        if (currentBubble != null) {
            currentBubble.draw(canvas, paint);
        }

        // Shots badge - repositioned to bottom-right corner of preview station
        drawNextBubbleShotsBadge(canvas, paint);

        // Swap Icon Indicator (only shown when there is a refiller ball to swap with)
        if (currentBubble != null && nextBubble != null) {
            float midX = (launcherX + previewX) / 2f;
            float midY = (launcherY + previewY) / 2f;
            drawSwapIcon(canvas, paint, midX, midY, bubbleRadius * 0.65f);
        }
    }

    private void drawNextBubbleShotsBadge(Canvas canvas, Paint paint) {
        // Anchored at bottom-right corner of preview pedestal / bubble so bubble face is fully visible
        float badgeX = previewX + bubbleRadius * 0.44f;
        float badgeY = previewY + bubbleRadius * 0.44f;
        float badgeRadius = bubbleRadius * 0.38f;

        boolean isLowShots = !isEndlessMode && shotsRemaining <= 5;
        String countStr = isEndlessMode ? "∞" : String.valueOf(Math.max(0, shotsRemaining));

        // 1. Low shots urgency glow pulse or clean dark obsidian disc
        if (isLowShots) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.008) * 0.5 + 0.5);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(bubbleRadius * 0.07f);
            paint.setColor(Color.argb((int) (100 + 100 * pulse), 255, 23, 68));
            canvas.drawCircle(badgeX, badgeY, badgeRadius + (bubbleRadius * 0.09f * pulse), paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#D32F2F"));
            canvas.drawCircle(badgeX, badgeY, badgeRadius, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#0F172A"));
            canvas.drawCircle(badgeX, badgeY, badgeRadius, paint);
        }

        // 2. Crisp ring border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2.0f, bubbleRadius * 0.06f));
        if (isLowShots) {
            paint.setColor(Color.parseColor("#FFCDD2"));
        } else {
            paint.setColor(Color.WHITE);
        }
        canvas.drawCircle(badgeX, badgeY, badgeRadius, paint);

        // 3. Centered count text
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float fontSize;
        if (countStr.length() >= 3) {
            fontSize = badgeRadius * 0.95f;
        } else if (countStr.length() == 2) {
            fontSize = badgeRadius * 1.15f;
        } else {
            fontSize = badgeRadius * 1.35f;
        }
        paint.setTextSize(fontSize);

        float textY = badgeY - ((paint.descent() + paint.ascent()) / 2f);

        // Drop shadow for ultra high readability
        paint.setColor(Color.parseColor("#B0000000"));
        canvas.drawText(countStr, badgeX, textY + 1.5f, paint);

        // Text foreground
        paint.setColor(Color.WHITE);
        canvas.drawText(countStr, badgeX, textY, paint);

        // Reset Paint attributes
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSwapIcon(Canvas canvas, Paint paint, float cx, float cy, float size) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        float strokeW = size * 0.13f;
        float rx = size * 0.72f;
        float ry = size * 0.42f;

        // Top curved arrow path (left to right with arrowhead on right)
        Path topPath = new Path();
        topPath.moveTo(cx - rx * 0.70f, cy - ry * 0.15f);
        topPath.cubicTo(cx - rx * 0.40f, cy - ry * 1.35f, cx + rx * 0.40f, cy - ry * 1.35f, cx + rx * 0.75f, cy - ry * 0.35f);
        // Arrowhead at (cx + rx * 0.75f, cy - ry * 0.35f)
        float headH = ry * 0.85f;
        float headW = rx * 0.42f;
        topPath.moveTo(cx + rx * 0.75f, cy - ry * 0.35f - headH);
        topPath.lineTo(cx + rx * 0.75f, cy - ry * 0.35f);
        topPath.lineTo(cx + rx * 0.75f - headW, cy - ry * 0.35f);

        // Bottom curved arrow path (right to left with arrowhead on left)
        Path botPath = new Path();
        botPath.moveTo(cx + rx * 0.70f, cy + ry * 0.15f);
        botPath.cubicTo(cx + rx * 0.40f, cy + ry * 1.35f, cx - rx * 0.40f, cy + ry * 1.35f, cx - rx * 0.75f, cy + ry * 0.35f);
        // Arrowhead at (cx - rx * 0.75f, cy + ry * 0.35f)
        botPath.moveTo(cx - rx * 0.75f, cy + ry * 0.35f + headH);
        botPath.lineTo(cx - rx * 0.75f, cy + ry * 0.35f);
        botPath.lineTo(cx - rx * 0.75f + headW, cy + ry * 0.35f);

        // 1. Dark Shadow Pass
        paint.setStrokeWidth(strokeW + 1.5f);
        paint.setColor(Color.argb(130, 0, 0, 0));
        canvas.save();
        canvas.translate(0, 2f);
        canvas.drawPath(topPath, paint);
        canvas.drawPath(botPath, paint);
        canvas.restore();

        // 2. Crisp White Foreground Pass
        paint.setStrokeWidth(strokeW);
        paint.setColor(Color.WHITE);
        canvas.drawPath(topPath, paint);
        canvas.drawPath(botPath, paint);

        paint.setStyle(Paint.Style.FILL);
    }

    public GameState getState() {
        return state;
    }

    private GameState stateBeforePause = GameState.READY;

    public void pause() {
        if (state != GameState.WIN && state != GameState.LOSE && state != GameState.PAUSED) {
            stateBeforePause = (state == GameState.AIMING) ? GameState.READY : state;
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = (stateBeforePause != null && stateBeforePause != GameState.PAUSED) ? stateBeforePause : GameState.READY;
        }
    }

    public int getShotsRemaining() {
        return shotsRemaining;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public BubbleBoard getBoard() {
        return board;
    }

    public boolean isEndlessMode() {
        return isEndlessMode;
    }

    public int getEndlessWaveCount() {
        return endlessWaveCount;
    }

    public int getEndlessHighScore() {
        return endlessHighScore;
    }
}
