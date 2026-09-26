package com.redcodersgroup.bubbleshooter.game;

import com.redcodersgroup.bubbleshooter.board.BubbleGrid;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class EndlessPatternGeneratorTest {

    @Test
    public void testActiveColorsScaling() {
        // Wave 1-5: 3 active colors
        List<BubbleColor> wave1Colors = EndlessPatternGenerator.getActiveColors(1, null);
        assertEquals(3, wave1Colors.size());

        // Wave 6-16: 4 active colors
        List<BubbleColor> wave10Colors = EndlessPatternGenerator.getActiveColors(10, null);
        assertEquals(4, wave10Colors.size());

        // Wave 17-32: 5 active colors
        List<BubbleColor> wave25Colors = EndlessPatternGenerator.getActiveColors(25, null);
        assertEquals(5, wave25Colors.size());

        // Wave 33+: 6 active colors
        List<BubbleColor> wave40Colors = EndlessPatternGenerator.getActiveColors(40, null);
        assertEquals(6, wave40Colors.size());
    }

    @Test
    public void testGenerateRowCohesiveness() {
        Random random = new Random(42);
        List<BubbleColor> colors = EndlessPatternGenerator.getActiveColors(1, null);
        List<Bubble> row = EndlessPatternGenerator.generateRow(1, BubbleGrid.COLS_EVEN, 0, colors, random);

        assertEquals(BubbleGrid.COLS_EVEN, row.size());
        for (Bubble b : row) {
            assertNotNull(b);
            assertNotNull(b.getColor());
            assertNotEquals(BubbleColor.NONE, b.getColor());
        }
    }

    @Test
    public void testPopulateInitialBoard() {
        BubbleGrid grid = new BubbleGrid();
        Random random = new Random(123);
        List<BubbleColor> colors = EndlessPatternGenerator.getActiveColors(1, null);

        EndlessPatternGenerator.populateInitialBoard(grid, 5, colors, random);

        // 5 rows populated: (9 + 8 + 9 + 8 + 9) = 43 bubbles
        assertEquals(43, grid.getBubbleCount());
    }

    @Test
    public void testGenerateMultiRowChunk() {
        Random random = new Random(99);
        List<BubbleColor> colors = EndlessPatternGenerator.getActiveColors(1, null);
        List<List<Bubble>> chunk = EndlessPatternGenerator.generateMultiRowChunk(1, 0, 3, colors, random);

        assertEquals(3, chunk.size());
        assertEquals(BubbleGrid.COLS_EVEN, chunk.get(0).size()); // row 0 has 9
        assertEquals(BubbleGrid.COLS_ODD, chunk.get(1).size());  // row 1 has 8
        assertEquals(BubbleGrid.COLS_EVEN, chunk.get(2).size()); // row 2 has 9

        for (List<Bubble> row : chunk) {
            for (Bubble b : row) {
                assertNotNull(b);
                assertNotNull(b.getColor());
            }
        }
    }
}
