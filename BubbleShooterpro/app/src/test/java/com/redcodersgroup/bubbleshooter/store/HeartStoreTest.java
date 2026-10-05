package com.redcodersgroup.bubbleshooter.store;

import com.redcodersgroup.bubbleshooter.ui.dialogs.HeartStoreDialog;
import org.junit.Test;
import static org.junit.Assert.*;

public class HeartStoreTest {

    @Test
    public void testHeartStorePricing() {
        assertEquals(StoreManager.COST_ONE_HEART, HeartStoreDialog.COST_ONE_HEART);
        assertEquals(StoreManager.COST_TRIPLE_HEARTS, HeartStoreDialog.COST_TRIPLE_HEARTS);
        assertEquals(StoreManager.COST_LIVES_REFILL, HeartStoreDialog.COST_FULL_REFILL);
    }

    @Test
    public void testAffordability() {
        assertTrue(StoreManager.canAfford(10, HeartStoreDialog.COST_ONE_HEART));
        assertFalse(StoreManager.canAfford(9, HeartStoreDialog.COST_ONE_HEART));
        assertTrue(StoreManager.canAfford(25, HeartStoreDialog.COST_TRIPLE_HEARTS));
        assertFalse(StoreManager.canAfford(24, HeartStoreDialog.COST_TRIPLE_HEARTS));
        assertTrue(StoreManager.canAfford(40, HeartStoreDialog.COST_FULL_REFILL));
        assertFalse(StoreManager.canAfford(39, HeartStoreDialog.COST_FULL_REFILL));
    }

    @Test
    public void testTripleHeartsDiscount() {
        // 3 single hearts would cost 3 * 5 = 15 diamonds. Triple pack costs 12 diamonds (20% discount).
        int separateCost = 3 * HeartStoreDialog.COST_ONE_HEART;
        assertTrue(HeartStoreDialog.COST_TRIPLE_HEARTS < separateCost);
    }

    @Test
    public void testFullRefillDiscount() {
        // 5 single hearts would cost 5 * 5 = 25 diamonds. Full refill costs 20 diamonds (20% discount).
        int separateCost = 5 * HeartStoreDialog.COST_ONE_HEART;
        assertTrue(HeartStoreDialog.COST_FULL_REFILL < separateCost);
    }
}
