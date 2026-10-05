package com.redcodersgroup.bubbleshooter.game;

import com.redcodersgroup.bubbleshooter.board.BubbleGrid;
import com.redcodersgroup.bubbleshooter.board.GridPosition;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EndlessPatternGenerator {

    public static final List<BubbleColor> DEFAULT_PALETTE = Arrays.asList(
            BubbleColor.RED,
            BubbleColor.BLUE,
            BubbleColor.GREEN,
            BubbleColor.YELLOW,
            BubbleColor.PURPLE,
            BubbleColor.ORANGE,
            BubbleColor.CYAN
    );

    /**
     * Returns an active color palette scaled by wave progression.
     * Starts with 3 colors for introductory flow and gradually unlocks 4th, 5th, 6th, and 7th colors.
     */
    public static List<BubbleColor> getActiveColors(int waveCount, List<BubbleColor> basePool) {
        List<BubbleColor> pool = (basePool != null && !basePool.isEmpty()) ? basePool : DEFAULT_PALETTE;
        int colorCount;
        if (waveCount <= 5) {
            colorCount = 3; // Waves 1-5: 3 colors (quick comfortable start)
        } else if (waveCount <= 16) {
            colorCount = 4; // Waves 6-16: 4 colors (moderate puzzle complexity)
        } else if (waveCount <= 28) {
            colorCount = 5; // Waves 17-28: 5 colors (healthy tactical challenge)
        } else if (waveCount <= 40) {
            colorCount = 6; // Waves 29-40: 6 colors
        } else {
            colorCount = Math.min(pool.size(), 7); // Wave 41+: full 7-color palette
        }
        colorCount = Math.min(colorCount, pool.size());

        List<BubbleColor> active = new ArrayList<>();
        for (int i = 0; i < colorCount; i++) {
            active.add(pool.get(i));
        }
        return active;
    }

    /**
     * Generates a single new top row using structured, playful patterns.
     */
    public static List<Bubble> generateRow(int waveCount, int cols, int rowParity, List<BubbleColor> activeColors, Random random) {
        if (activeColors == null || activeColors.isEmpty()) {
            activeColors = getActiveColors(waveCount, null);
        }

        BubbleColor[] rowColors = new BubbleColor[cols];
        BubbleType[] rowTypes = new BubbleType[cols];
        Arrays.fill(rowTypes, BubbleType.NORMAL);

        int patternType = random.nextInt(4);

        if (patternType == 0) {
            // Pattern 1: Cohesive Cluster Runs (groups of 2-3 bubbles)
            int c = 0;
            while (c < cols) {
                BubbleColor clusterColor = activeColors.get(random.nextInt(activeColors.size()));
                int clusterLen = (waveCount <= 10) ? (2 + random.nextInt(3)) : (2 + random.nextInt(2));
                for (int i = 0; i < clusterLen && c < cols; i++, c++) {
                    rowColors[c] = clusterColor;
                }
            }
        } else if (patternType == 1) {
            // Pattern 2: Symmetrical Mirror
            int half = (cols + 1) / 2;
            BubbleColor[] halfColors = new BubbleColor[half];
            int h = 0;
            while (h < half) {
                BubbleColor colColor = activeColors.get(random.nextInt(activeColors.size()));
                int run = 1 + random.nextInt(2);
                for (int i = 0; i < run && h < half; i++, h++) {
                    halfColors[h] = colColor;
                }
            }
            for (int c = 0; c < cols; c++) {
                int srcIdx = (c < half) ? c : (cols - 1 - c);
                rowColors[c] = halfColors[srcIdx];
            }
        } else if (patternType == 2) {
            // Pattern 3: Alternating Pairs [A, A, B, B, A, A, ...]
            BubbleColor colorA = activeColors.get(random.nextInt(activeColors.size()));
            BubbleColor colorB = activeColors.get(random.nextInt(activeColors.size()));
            if (colorB == colorA && activeColors.size() > 1) {
                colorB = activeColors.get((activeColors.indexOf(colorA) + 1) % activeColors.size());
            }
            for (int c = 0; c < cols; c++) {
                rowColors[c] = ((c / 2) % 2 == 0) ? colorA : colorB;
            }
        } else {
            // Pattern 4: Solid Split with Center Accent
            BubbleColor leftColor = activeColors.get(random.nextInt(activeColors.size()));
            BubbleColor rightColor = activeColors.get(random.nextInt(activeColors.size()));
            int mid = cols / 2;
            for (int c = 0; c < cols; c++) {
                if (c < mid) {
                    rowColors[c] = leftColor;
                } else if (c > mid) {
                    rowColors[c] = rightColor;
                } else {
                    rowColors[c] = activeColors.get(random.nextInt(activeColors.size()));
                }
            }
        }

        List<Bubble> result = new ArrayList<>(cols);
        for (int c = 0; c < cols; c++) {
            BubbleColor color = (rowColors[c] != null) ? rowColors[c] : activeColors.get(0);
            BubbleType type = rowTypes[c];
            result.add(new Bubble(color, type, new GridPosition(0, c)));
        }
        return result;
    }

    /**
     * Pre-generates a coherent 2-3 row chunk with 2D vertical & diagonal pattern continuity.
     */
    public static List<List<Bubble>> generateMultiRowChunk(int waveCount, int startParity, int numRows, List<BubbleColor> activeColors, Random random) {
        if (activeColors == null || activeColors.isEmpty()) {
            activeColors = getActiveColors(waveCount, null);
        }

        List<List<Bubble>> chunk = new ArrayList<>(numRows);
        int currentParity = startParity;
        int motif = random.nextInt(4);

        if (motif == 0) {
            // Motif 1: 2D Vertical Connected Color Blobs (2-3 row color clusters)
            BubbleColor col1 = activeColors.get(random.nextInt(activeColors.size()));
            BubbleColor col2 = activeColors.get((activeColors.indexOf(col1) + 1) % activeColors.size());
            BubbleColor col3 = (activeColors.size() > 2)
                    ? activeColors.get((activeColors.indexOf(col2) + 1) % activeColors.size())
                    : col1;

            for (int r = 0; r < numRows; r++) {
                int cols = (currentParity == 0) ? BubbleGrid.COLS_EVEN : BubbleGrid.COLS_ODD;
                BubbleColor[] rowColors = new BubbleColor[cols];
                int third = Math.max(1, cols / 3);
                for (int c = 0; c < cols; c++) {
                    if (c < third) {
                        rowColors[c] = col1;
                    } else if (c < third * 2) {
                        rowColors[c] = col2;
                    } else {
                        rowColors[c] = col3;
                    }
                }

                BubbleType[] rowTypes = new BubbleType[cols];
                Arrays.fill(rowTypes, BubbleType.NORMAL);

                List<Bubble> row = new ArrayList<>(cols);
                for (int c = 0; c < cols; c++) {
                    row.add(new Bubble(rowColors[c], BubbleType.NORMAL, new GridPosition(0, c)));
                }
                chunk.add(row);
                currentParity ^= 1;
            }
        } else if (motif == 1) {
            // Motif 2: Diagonal Striped Ribbons across rows
            BubbleColor colorA = activeColors.get(random.nextInt(activeColors.size()));
            BubbleColor colorB = activeColors.get(random.nextInt(activeColors.size()));
            if (colorB == colorA && activeColors.size() > 1) {
                colorB = activeColors.get((activeColors.indexOf(colorA) + 1) % activeColors.size());
            }

            for (int r = 0; r < numRows; r++) {
                int cols = (currentParity == 0) ? BubbleGrid.COLS_EVEN : BubbleGrid.COLS_ODD;
                List<Bubble> row = new ArrayList<>(cols);
                for (int c = 0; c < cols; c++) {
                    BubbleColor col = (((c + r) / 2) % 2 == 0) ? colorA : colorB;
                    row.add(new Bubble(col, BubbleType.NORMAL, new GridPosition(0, c)));
                }
                chunk.add(row);
                currentParity ^= 1;
            }
        } else if (motif == 2) {
            // Motif 3: Symmetrical Concentric Arcs across rows
            BubbleColor outer = activeColors.get(random.nextInt(activeColors.size()));
            BubbleColor inner = activeColors.get((activeColors.indexOf(outer) + 1) % activeColors.size());

            for (int r = 0; r < numRows; r++) {
                int cols = (currentParity == 0) ? BubbleGrid.COLS_EVEN : BubbleGrid.COLS_ODD;
                List<Bubble> row = new ArrayList<>(cols);
                int border = Math.max(1, 2 - r);
                for (int c = 0; c < cols; c++) {
                    boolean isOuter = (c < border || c >= (cols - border));
                    BubbleColor col = isOuter ? outer : inner;
                    row.add(new Bubble(col, BubbleType.NORMAL, new GridPosition(0, c)));
                }
                chunk.add(row);
                currentParity ^= 1;
            }
        } else {
            // Motif 4: Harmonious Cluster Runs
            for (int r = 0; r < numRows; r++) {
                int cols = (currentParity == 0) ? BubbleGrid.COLS_EVEN : BubbleGrid.COLS_ODD;
                List<Bubble> row = generateRow(waveCount + r, cols, currentParity, activeColors, random);
                chunk.add(row);
                currentParity ^= 1;
            }
        }

        return chunk;
    }

    /**
     * Populates the starting board with structured, satisfying puzzle rows.
     */
    public static void populateInitialBoard(BubbleGrid grid, int rowCount, List<BubbleColor> activeColors, Random random) {
        grid.clear();
        if (activeColors == null || activeColors.isEmpty()) {
            activeColors = getActiveColors(1, null);
        }

        List<List<Bubble>> initialChunk = generateMultiRowChunk(1, 0, rowCount, activeColors, random);
        for (int r = 0; r < initialChunk.size() && r < rowCount; r++) {
            List<Bubble> rowBubbles = initialChunk.get(r);
            int cols = grid.getCols(r);
            for (int c = 0; c < rowBubbles.size() && c < cols; c++) {
                Bubble b = rowBubbles.get(c);
                b.setGridPosition(new GridPosition(r, c));
                grid.setBubble(r, c, b);
            }
        }
    }
}
