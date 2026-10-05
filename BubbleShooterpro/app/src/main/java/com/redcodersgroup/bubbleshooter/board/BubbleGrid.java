package com.redcodersgroup.bubbleshooter.board;

import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import java.util.ArrayList;
import java.util.List;

public class BubbleGrid {
    public static final int COLS_EVEN = NeighborCalculator.COLS_EVEN;
    public static final int COLS_ODD = NeighborCalculator.COLS_ODD;
    public static final int MAX_ROWS = NeighborCalculator.MAX_ROWS;
    public static final float ROW_HEIGHT_RATIO = 1.7320508f; // sqrt(3)

    private final Bubble[][] grid;
    private float boardLeft = 0f;
    private float boardTop = 0f;
    private float bubbleRadius = 40f;
    private int rowParity = 0; // 0: even row is 9 cols; 1: even row is 8 cols

    // Unified 60 FPS descent animation
    private boolean isDescending = false;
    private float descentElapsed = 0f;
    private float descentDuration = 0.28f;
    private float currentDescentOffsetY = 0f;

    public BubbleGrid() {
        this.grid = new Bubble[MAX_ROWS][COLS_EVEN];
    }

    public boolean isDescending() {
        return isDescending;
    }

    public float getCurrentDescentOffsetY() {
        return currentDescentOffsetY;
    }

    public void update(float dt) {
        if (isDescending) {
            descentElapsed += dt;
            float t = Math.min(1.0f, descentElapsed / descentDuration);
            // Quartic Ease-Out curve for ultra-smooth gliding descent with zero velocity kink
            float p = 1.0f - t;
            float ease = 1.0f - (p * p * p * p);

            currentDescentOffsetY = -(bubbleRadius * ROW_HEIGHT_RATIO) * (1.0f - ease);

            for (int r = 0; r < MAX_ROWS; r++) {
                int cols = getCols(r);
                for (int c = 0; c < cols; c++) {
                    Bubble b = grid[r][c];
                    if (b != null && !b.isFalling() && !b.isPopping()) {
                        b.setX(getCenterX(r, c));
                        b.setY(getCenterY(r) + currentDescentOffsetY);
                        if (r == 0) {
                            b.setAlpha(Math.min(1.0f, ease * 1.5f));
                        } else {
                            b.setAlpha(1.0f);
                        }
                    }
                }
            }

            if (t >= 1.0f) {
                isDescending = false;
                currentDescentOffsetY = 0f;
                for (int r = 0; r < MAX_ROWS; r++) {
                    int cols = getCols(r);
                    for (int c = 0; c < cols; c++) {
                        Bubble b = grid[r][c];
                        if (b != null && !b.isFalling() && !b.isPopping()) {
                            b.setX(getCenterX(r, c));
                            b.setY(getCenterY(r));
                            b.setAlpha(1.0f);
                        }
                    }
                }
            }
        }
    }

    public void finishDescent() {
        if (isDescending) {
            isDescending = false;
            currentDescentOffsetY = 0f;
            for (int r = 0; r < MAX_ROWS; r++) {
                int cols = getCols(r);
                for (int c = 0; c < cols; c++) {
                    Bubble b = grid[r][c];
                    if (b != null && !b.isFalling() && !b.isPopping()) {
                        b.setX(getCenterX(r, c));
                        b.setY(getCenterY(r));
                        b.setAlpha(1.0f);
                    }
                }
            }
        }
    }

    public List<Integer> getOccupiedRowsFromBottom() {
        List<Integer> list = new ArrayList<>();
        for (int r = MAX_ROWS - 1; r >= 0; r--) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) {
                    list.add(r);
                    break;
                }
            }
        }
        return list;
    }

    public int getRowParity() {
        return rowParity;
    }

    public void setRowParity(int rowParity) {
        this.rowParity = rowParity & 1;
    }

    public int getCols(int row) {
        return ((row + rowParity) % 2 == 0) ? COLS_EVEN : COLS_ODD;
    }

    public void setDimensions(float boardLeft, float boardTop, float bubbleRadius) {
        this.boardLeft = boardLeft;
        this.boardTop = boardTop;
        this.bubbleRadius = bubbleRadius;

        // Update coordinates of all existing bubbles in the grid
        for (int r = 0; r < MAX_ROWS; r++) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                Bubble b = grid[r][c];
                if (b != null) {
                    b.setX(getCenterX(r, c));
                    b.setY(getCenterY(r));
                    b.setRadius(bubbleRadius);
                }
            }
        }
    }

    public float getBubbleRadius() {
        return bubbleRadius;
    }

    public float getBoardLeft() {
        return boardLeft;
    }

    public float getBoardTop() {
        return boardTop;
    }

    public float getCenterX(int row, int col) {
        boolean isEven = ((row + rowParity) % 2 == 0);
        float xOffset = isEven ? bubbleRadius : (bubbleRadius * 2f);
        return boardLeft + xOffset + (col * 2f * bubbleRadius);
    }

    public float getCenterY(int row) {
        return boardTop + bubbleRadius + (row * bubbleRadius * ROW_HEIGHT_RATIO);
    }

    public Bubble getBubble(int row, int col) {
        if (!NeighborCalculator.isValidPosition(row, col, rowParity)) {
            return null;
        }
        return grid[row][col];
    }

    public Bubble getBubble(GridPosition pos) {
        if (pos == null) return null;
        return getBubble(pos.row, pos.col);
    }

    public boolean setBubble(int row, int col, Bubble bubble) {
        if (!NeighborCalculator.isValidPosition(row, col, rowParity)) {
            return false;
        }
        grid[row][col] = bubble;
        if (bubble != null) {
            bubble.setGridPosition(new GridPosition(row, col));
            bubble.setX(getCenterX(row, col));
            bubble.setY(getCenterY(row));
            bubble.setRadius(bubbleRadius);
        }
        return true;
    }

    public boolean setBubble(GridPosition pos, Bubble bubble) {
        if (pos == null) return false;
        return setBubble(pos.row, pos.col, bubble);
    }

    public Bubble removeBubble(int row, int col) {
        if (!NeighborCalculator.isValidPosition(row, col, rowParity)) {
            return null;
        }
        Bubble b = grid[row][col];
        grid[row][col] = null;
        return b;
    }

    public Bubble removeBubble(GridPosition pos) {
        if (pos == null) return null;
        return removeBubble(pos.row, pos.col);
    }

    public boolean isEmpty(int row, int col) {
        return getBubble(row, col) == null;
    }

    public boolean isEmpty(GridPosition pos) {
        return getBubble(pos) == null;
    }

    public List<Bubble> getAllBubbles() {
        List<Bubble> list = new ArrayList<>();
        for (int r = 0; r < MAX_ROWS; r++) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) {
                    list.add(grid[r][c]);
                }
            }
        }
        return list;
    }

    public int getBubbleCount() {
        int count = 0;
        for (int r = 0; r < MAX_ROWS; r++) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getLowestOccupiedRow() {
        for (int r = MAX_ROWS - 1; r >= 0; r--) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) {
                    return r;
                }
            }
        }
        return -1;
    }

    /**
     * Shifts all rows down by 1 in Endless mode, flips parity, and inserts the new top row at row 0.
     * All bubbles glide down simultaneously with synchronized 60 FPS ease-out curve.
     */
    public void shiftDownAndInsertRow(List<Bubble> newTopRow) {
        float rowHeight = bubbleRadius * ROW_HEIGHT_RATIO;

        // 1. Shift all rows down by 1 from bottom to top
        for (int r = MAX_ROWS - 2; r >= 0; r--) {
            for (int c = 0; c < COLS_EVEN; c++) {
                grid[r + 1][c] = grid[r][c];
            }
        }

        // 2. Clear row 0
        for (int c = 0; c < COLS_EVEN; c++) {
            grid[0][c] = null;
        }

        // 3. Toggle parity
        rowParity ^= 1;

        // 4. Insert new top row
        if (newTopRow != null) {
            int topCols = getCols(0);
            for (int c = 0; c < newTopRow.size() && c < topCols; c++) {
                Bubble b = newTopRow.get(c);
                if (b != null) {
                    b.setGridPosition(new GridPosition(0, c));
                    b.setRadius(bubbleRadius);
                    b.setAlpha(0f);
                    grid[0][c] = b;
                }
            }
        }

        // 5. Update grid positions for shifted rows
        for (int r = 1; r < MAX_ROWS; r++) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                Bubble b = grid[r][c];
                if (b != null) {
                    b.setGridPosition(new GridPosition(r, c));
                    b.setRadius(bubbleRadius);
                }
            }
        }

        // 6. Start synchronized 60 FPS descent glide
        isDescending = true;
        descentElapsed = 0f;
        descentDuration = 0.28f;
        currentDescentOffsetY = -rowHeight;

        for (int r = 0; r < MAX_ROWS; r++) {
            int cols = getCols(r);
            for (int c = 0; c < cols; c++) {
                Bubble b = grid[r][c];
                if (b != null && !b.isFalling() && !b.isPopping()) {
                    b.setX(getCenterX(r, c));
                    b.setY(getCenterY(r) + currentDescentOffsetY);
                    b.setAlpha(r == 0 ? 0f : 1.0f);
                }
            }
        }
    }

    public void clear() {
        rowParity = 0;
        isDescending = false;
        currentDescentOffsetY = 0f;
        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < COLS_EVEN; c++) {
                grid[r][c] = null;
            }
        }
    }
}
