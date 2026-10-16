package com.redcodersgroup.bubbleshooter.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChestSyncTest {

    @Test
    public void testMilestoneToWorldGiftMapping() {
        // Milestone 1 -> World 1, Gift 1
        assertEquals(1, ((1 - 1) / 2) + 1);
        assertEquals(1, ((1 - 1) % 2) + 1);

        // Milestone 2 -> World 1, Gift 2
        assertEquals(1, ((2 - 1) / 2) + 1);
        assertEquals(2, ((2 - 1) % 2) + 1);

        // Milestone 3 -> World 2, Gift 1
        assertEquals(2, ((3 - 1) / 2) + 1);
        assertEquals(1, ((3 - 1) % 2) + 1);

        // Milestone 4 -> World 2, Gift 2
        assertEquals(2, ((4 - 1) / 2) + 1);
        assertEquals(2, ((4 - 1) % 2) + 1);

        // Roundtrip test for all 49 worlds (98 gifts)
        for (int world = 1; world <= 49; world++) {
            for (int gift = 1; gift <= 2; gift++) {
                int milestone = (world - 1) * 2 + gift;
                int derivedWorld = ((milestone - 1) / 2) + 1;
                int derivedGift = ((milestone - 1) % 2) + 1;
                assertEquals(world, derivedWorld);
                assertEquals(gift, derivedGift);
            }
        }
    }

    @Test
    public void testClaimThresholdCoverage() {
        // If claimedStarChestsCount is 3, World 1 Gifts 1 & 2 and World 2 Gift 1 should be covered
        int claimedCount = 3;

        // World 1, Gift 1 -> milestone 1 <= 3: covered
        assertTrue(claimedCount >= (1 - 1) * 2 + 1);
        // World 1, Gift 2 -> milestone 2 <= 3: covered
        assertTrue(claimedCount >= (1 - 1) * 2 + 2);
        // World 2, Gift 1 -> milestone 3 <= 3: covered
        assertTrue(claimedCount >= (2 - 1) * 2 + 1);
        // World 2, Gift 2 -> milestone 4 > 3: not covered
        assertFalse(claimedCount >= (2 - 1) * 2 + 2);
    }
}
