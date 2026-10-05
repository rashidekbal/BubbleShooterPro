package com.redcodersgroup.bubbleshooter.store;

import org.junit.Test;
import static org.junit.Assert.*;

public class StoreTransactionTest {

    @Test
    public void testCatalogConstants() {
        assertEquals(3, StoreManager.DIAMONDS_DAILY_FREE);
        assertEquals(140, StoreManager.DIAMONDS_POUCH);
        assertEquals(500, StoreManager.DIAMONDS_SACK);
        assertEquals(1600, StoreManager.DIAMONDS_CHEST);

        assertEquals(60, StoreManager.COST_BOMB_PACK);
        assertEquals(60, StoreManager.COST_FIREBALL_PACK);
        assertEquals(60, StoreManager.COST_LIGHTNING_PACK);
        assertEquals(60, StoreManager.COST_RAINBOW_PACK);
        assertEquals(180, StoreManager.COST_MEGA_BUNDLE);
        assertEquals(40, StoreManager.COST_LIVES_REFILL);
        assertEquals(5, StoreManager.MAX_LIVES);
    }

    @Test
    public void testCanAfford() {
        assertTrue(StoreManager.canAfford(100, 60));
        assertTrue(StoreManager.canAfford(60, 60));
        assertFalse(StoreManager.canAfford(59, 60));
        assertFalse(StoreManager.canAfford(0, 40));
    }

    @Test
    public void testDeductDiamondsSuccess() {
        int startingBalance = 250;
        int remaining = StoreManager.deductDiamonds(startingBalance, StoreManager.COST_MEGA_BUNDLE);
        assertEquals(70, remaining);

        remaining = StoreManager.deductDiamonds(remaining, StoreManager.COST_LIVES_REFILL);
        assertEquals(30, remaining);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeductDiamondsInsufficientThrows() {
        StoreManager.deductDiamonds(50, StoreManager.COST_BOMB_PACK);
    }

    @Test
    public void testMegaBundleWorthCalculation() {
        // Individual prices: 40 + 40 + 40 + 50 = 170 for 3x each, or ~113 for 2x each + 25 for lives = 138 value for 120
        int individualBoostersCost = StoreManager.COST_BOMB_PACK + StoreManager.COST_FIREBALL_PACK + StoreManager.COST_LIGHTNING_PACK + StoreManager.COST_RAINBOW_PACK;
        assertTrue("Mega bundle cost should be less than buying separate boosters", StoreManager.COST_MEGA_BUNDLE < individualBoostersCost);
    }

    @Test
    public void testBoosterUnlockLevels() {
        assertEquals(21, com.redcodersgroup.bubbleshooter.data.PreferencesManager.UNLOCK_LEVEL_BOMB);
        assertEquals(41, com.redcodersgroup.bubbleshooter.data.PreferencesManager.UNLOCK_LEVEL_RAINBOW);
        assertEquals(61, com.redcodersgroup.bubbleshooter.data.PreferencesManager.UNLOCK_LEVEL_FIREBALL);
        assertEquals(81, com.redcodersgroup.bubbleshooter.data.PreferencesManager.UNLOCK_LEVEL_LIGHTNING);
    }
}
