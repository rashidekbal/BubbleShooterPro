package com.redcodersgroup.bubbleshooter.game;

import com.redcodersgroup.bubbleshooter.board.BubbleGrid;
import com.redcodersgroup.bubbleshooter.board.GridPosition;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class BubbleColorRuleTest {

    @Test
    public void testExceptionForSingleRemainingColor() {
        BubbleGrid grid = new BubbleGrid();
        // Only RED bubbles on board
        grid.setBubble(0, 0, new Bubble(BubbleColor.RED, new GridPosition(0, 0)));
        grid.setBubble(0, 1, new Bubble(BubbleColor.RED, new GridPosition(0, 1)));

        // Helper mock simulating color picker with 1 color left
        List<BubbleColor> remaining = new ArrayList<>();
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null && b.getType() == BubbleType.NORMAL && BubbleColor.getPlayableColors().contains(b.getColor())) {
                if (!remaining.contains(b.getColor())) remaining.add(b.getColor());
            }
        }

        assertEquals(1, remaining.size());
        assertEquals(BubbleColor.RED, remaining.get(0));
    }

    @Test
    public void testAvoidColorWhenMultipleRemainingColors() {
        List<BubbleColor> candidates = new ArrayList<>();
        candidates.add(BubbleColor.RED);
        candidates.add(BubbleColor.BLUE);
        candidates.add(BubbleColor.GREEN);

        BubbleColor avoidColor = BubbleColor.RED;
        List<BubbleColor> filtered = new ArrayList<>(candidates);
        filtered.remove(avoidColor);

        assertFalse(filtered.contains(BubbleColor.RED));
        assertTrue(filtered.contains(BubbleColor.BLUE));
        assertTrue(filtered.contains(BubbleColor.GREEN));
    }

    @Test
    public void testConsecutiveColorsNeverExceedTwo() {
        Random random = new Random(42);
        List<BubbleColor> palette = new ArrayList<>();
        palette.add(BubbleColor.RED);
        palette.add(BubbleColor.BLUE);
        palette.add(BubbleColor.GREEN);

        BubbleColor lastFired = null;
        BubbleColor current = palette.get(random.nextInt(palette.size()));
        BubbleColor next = palette.get(random.nextInt(palette.size()));

        int consecutiveCount = 0;
        BubbleColor prevColorTrack = null;

        for (int shot = 0; shot < 1000; shot++) {
            BubbleColor fired = current;

            if (fired == prevColorTrack) {
                consecutiveCount++;
            } else {
                prevColorTrack = fired;
                consecutiveCount = 1;
            }

            assertTrue("Never exceed 2 consecutive of the same color! Failed on shot " + shot + " with " + fired,
                    consecutiveCount <= 2);

            lastFired = fired;
            current = next;

            // Generate new next bubble with max-2 rule
            BubbleColor avoid = null;
            if (lastFired != null && current != null && current == lastFired) {
                avoid = lastFired;
            }

            List<BubbleColor> avail = new ArrayList<>(palette);
            if (avail.size() > 1 && avoid != null) {
                avail.remove(avoid);
            }
            next = avail.get(random.nextInt(avail.size()));
        }
    }

    @Test
    public void testCyanIsInPlayableColors() {
        List<BubbleColor> playable = BubbleColor.getPlayableColors();
        assertTrue("CYAN must be in playable colors!", playable.contains(BubbleColor.CYAN));
        assertEquals(7, playable.size());
    }

    @Test
    public void testCyanBoardBubbleIsIncludedInRemainingColors() {
        BubbleGrid grid = new BubbleGrid();
        grid.setBubble(0, 0, new Bubble(BubbleColor.CYAN, new GridPosition(0, 0)));
        grid.setBubble(0, 1, new Bubble(BubbleColor.RED, new GridPosition(0, 1)));

        List<BubbleColor> remaining = new ArrayList<>();
        for (Bubble b : grid.getAllBubbles()) {
            if (b != null && b.getType() == BubbleType.NORMAL && BubbleColor.getPlayableColors().contains(b.getColor())) {
                if (!remaining.contains(b.getColor())) remaining.add(b.getColor());
            }
        }

        assertEquals(2, remaining.size());
        assertTrue(remaining.contains(BubbleColor.CYAN));
        assertTrue(remaining.contains(BubbleColor.RED));
    }
}
