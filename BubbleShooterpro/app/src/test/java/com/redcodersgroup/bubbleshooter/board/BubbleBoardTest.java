package com.redcodersgroup.bubbleshooter.board;

import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BubbleBoardTest {

    private BubbleGrid grid;
    private BubbleBoard board;

    @Before
    public void setUp() {
        grid = new BubbleGrid();
        board = new BubbleBoard(grid);
    }

    @Test
    public void testMatchThreeSameColor() {
        // Place 3 RED bubbles adjacent to each other
        grid.setBubble(0, 0, new Bubble(BubbleColor.RED, new GridPosition(0, 0)));
        grid.setBubble(0, 1, new Bubble(BubbleColor.RED, new GridPosition(0, 1)));
        grid.setBubble(1, 0, new Bubble(BubbleColor.RED, new GridPosition(1, 0)));

        List<GridPosition> matches = board.findMatches(new GridPosition(0, 0));
        assertEquals(3, matches.size());
        assertTrue(matches.contains(new GridPosition(0, 0)));
        assertTrue(matches.contains(new GridPosition(0, 1)));
        assertTrue(matches.contains(new GridPosition(1, 0)));
    }

    @Test
    public void testMatchLessThanThreeReturnsEmpty() {
        // Place only 2 RED bubbles
        grid.setBubble(0, 0, new Bubble(BubbleColor.RED, new GridPosition(0, 0)));
        grid.setBubble(0, 1, new Bubble(BubbleColor.RED, new GridPosition(0, 1)));
        grid.setBubble(1, 0, new Bubble(BubbleColor.BLUE, new GridPosition(1, 0))); // Different color

        List<GridPosition> matches = board.findMatches(new GridPosition(0, 0));
        assertEquals(0, matches.size()); // Rule: requires 3 or more!
    }

    @Test
    public void testFloatingBubblesDetection() {
        // Row 0 has a ceiling bubble
        grid.setBubble(0, 0, new Bubble(BubbleColor.BLUE, new GridPosition(0, 0)));

        // Row 2 and 3 have disconnected bubbles (no connection to row 0)
        grid.setBubble(2, 2, new Bubble(BubbleColor.RED, new GridPosition(2, 2)));
        grid.setBubble(2, 3, new Bubble(BubbleColor.RED, new GridPosition(2, 3)));

        List<Bubble> floating = board.findFloatingBubbles();
        assertEquals(2, floating.size());

        // The ceiling bubble in (0, 0) should remain connected
        assertNotNull(grid.getBubble(0, 0));
        // Disconnected bubbles should have been detached
        assertNull(grid.getBubble(2, 2));
        assertNull(grid.getBubble(2, 3));
    }

    @Test
    public void testShiftDownAndInsertRow() {
        // Place bubbles in row 0
        grid.setBubble(0, 0, new Bubble(BubbleColor.BLUE, new GridPosition(0, 0)));
        grid.setBubble(0, 1, new Bubble(BubbleColor.BLUE, new GridPosition(0, 1)));
        assertEquals(0, grid.getRowParity());

        // Prepare new top row with 8 bubbles (since parity toggles to 1, row 0 will have 8 columns)
        java.util.List<Bubble> newTopRow = new java.util.ArrayList<>();
        for (int c = 0; c < BubbleGrid.COLS_ODD; c++) {
            newTopRow.add(new Bubble(BubbleColor.YELLOW, new GridPosition(0, c)));
        }

        grid.shiftDownAndInsertRow(newTopRow);

        // Parity is now 1 (odd)
        assertEquals(1, grid.getRowParity());

        // Row 0 should have the yellow bubbles
        for (int c = 0; c < BubbleGrid.COLS_ODD; c++) {
            Bubble b = grid.getBubble(0, c);
            assertNotNull(b);
            assertEquals(BubbleColor.YELLOW, b.getColor());
            assertEquals(0, b.getGridPosition().row);
            assertEquals(c, b.getGridPosition().col);
        }

        // Previous row 0 blue bubbles are now shifted to row 1
        Bubble b0 = grid.getBubble(1, 0);
        Bubble b1 = grid.getBubble(1, 1);
        assertNotNull(b0);
        assertNotNull(b1);
        assertEquals(BubbleColor.BLUE, b0.getColor());
        assertEquals(BubbleColor.BLUE, b1.getColor());
        assertEquals(1, b0.getGridPosition().row);
        assertEquals(0, b0.getGridPosition().col);
    }

    @Test
    public void testTransparentBubbleBurstByAnyColor() {
        // Transparent bubble at (0, 1)
        grid.setBubble(0, 1, new Bubble(BubbleColor.TRANSPARENT, BubbleType.TRANSPARENT, new GridPosition(0, 1)));
        // Non-matching Green bubble at (0, 0)
        grid.setBubble(0, 0, new Bubble(BubbleColor.GREEN, new GridPosition(0, 0)));

        // Snap a BLUE bubble at (1, 0) which neighbors (0, 1)
        grid.setBubble(1, 0, new Bubble(BubbleColor.BLUE, new GridPosition(1, 0)));

        List<GridPosition> matches = board.findMatches(new GridPosition(1, 0));
        assertEquals(1, matches.size());
        assertTrue(matches.contains(new GridPosition(0, 1)));
    }

    @Test
    public void testConnectedTransparentBubblesChainBurst() {
        // Two connected transparent bubbles at (0, 1) and (0, 2)
        grid.setBubble(0, 1, new Bubble(BubbleColor.TRANSPARENT, BubbleType.TRANSPARENT, new GridPosition(0, 1)));
        grid.setBubble(0, 2, new Bubble(BubbleColor.TRANSPARENT, BubbleType.TRANSPARENT, new GridPosition(0, 2)));

        // Snap a RED bubble at (1, 0) touching (0, 1)
        grid.setBubble(1, 0, new Bubble(BubbleColor.RED, new GridPosition(1, 0)));

        List<GridPosition> matches = board.findMatches(new GridPosition(1, 0));
        assertEquals(2, matches.size());
        assertTrue(matches.contains(new GridPosition(0, 1)));
        assertTrue(matches.contains(new GridPosition(0, 2)));
    }

    @Test
    public void testTransparentBurstAlongWithMatchThree() {
        // Two RED bubbles at (0, 0) and (0, 1) + Transparent bubble at (0, 2)
        grid.setBubble(0, 0, new Bubble(BubbleColor.RED, new GridPosition(0, 0)));
        grid.setBubble(0, 1, new Bubble(BubbleColor.RED, new GridPosition(0, 1)));
        grid.setBubble(0, 2, new Bubble(BubbleColor.TRANSPARENT, BubbleType.TRANSPARENT, new GridPosition(0, 2)));

        // Snap third RED bubble at (1, 1) touching both (0, 1) and (0, 2)
        grid.setBubble(1, 1, new Bubble(BubbleColor.RED, new GridPosition(1, 1)));

        List<GridPosition> matches = board.findMatches(new GridPosition(1, 1));
        assertEquals(4, matches.size());
        assertTrue(matches.contains(new GridPosition(0, 0)));
        assertTrue(matches.contains(new GridPosition(0, 1)));
        assertTrue(matches.contains(new GridPosition(1, 1)));
        assertTrue(matches.contains(new GridPosition(0, 2)));
    }

    @Test
    public void testOccupiedRowsFromBottomAndClear() {
        // Place bubbles on rows 0, 2, 4, 6, 8, 10
        grid.setBubble(0, 0, new Bubble(BubbleColor.RED, new GridPosition(0, 0)));
        grid.setBubble(2, 0, new Bubble(BubbleColor.BLUE, new GridPosition(2, 0)));
        grid.setBubble(4, 0, new Bubble(BubbleColor.GREEN, new GridPosition(4, 0)));
        grid.setBubble(6, 0, new Bubble(BubbleColor.YELLOW, new GridPosition(6, 0)));
        grid.setBubble(8, 0, new Bubble(BubbleColor.PURPLE, new GridPosition(8, 0)));
        grid.setBubble(10, 0, new Bubble(BubbleColor.CYAN, new GridPosition(10, 0)));

        List<Integer> occupied = grid.getOccupiedRowsFromBottom();
        assertEquals(6, occupied.size());
        assertEquals(Integer.valueOf(10), occupied.get(0));
        assertEquals(Integer.valueOf(8), occupied.get(1));
        assertEquals(Integer.valueOf(6), occupied.get(2));
        assertEquals(Integer.valueOf(4), occupied.get(3));
        assertEquals(Integer.valueOf(2), occupied.get(4));
        assertEquals(Integer.valueOf(0), occupied.get(5));

        // Clear 5 rows from bottom (10, 8, 6, 4, 2)
        int toClear = Math.min(5, occupied.size());
        for (int i = 0; i < toClear; i++) {
            int r = occupied.get(i);
            grid.removeBubble(r, 0);
        }

        List<Integer> remaining = grid.getOccupiedRowsFromBottom();
        assertEquals(1, remaining.size());
        assertEquals(Integer.valueOf(0), remaining.get(0));
    }
}
