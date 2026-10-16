package com.redcodersgroup.bubbleshooter.ads;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdManagerBossLevelTest {

    @Test
    public void testBossLevelIntervalConstant() {
        assertEquals(5, AdManager.BOSS_LEVEL_INTERVAL);
    }

    @Test
    public void testIsBossLevel() {
        AdManager adManager = AdManager.getInstance();

        // Normal non-boss levels
        assertFalse(adManager.isBossLevel(0));
        assertFalse(adManager.isBossLevel(-5));
        assertFalse(adManager.isBossLevel(1));
        assertFalse(adManager.isBossLevel(2));
        assertFalse(adManager.isBossLevel(3));
        assertFalse(adManager.isBossLevel(4));
        assertFalse(adManager.isBossLevel(6));
        assertFalse(adManager.isBossLevel(7));
        assertFalse(adManager.isBossLevel(8));
        assertFalse(adManager.isBossLevel(9));
        assertFalse(adManager.isBossLevel(11));
        assertFalse(adManager.isBossLevel(14));
        assertFalse(adManager.isBossLevel(99));

        // Boss levels: 5, 10, 15, 20, 25, 30, etc.
        assertTrue(adManager.isBossLevel(5));
        assertTrue(adManager.isBossLevel(10));
        assertTrue(adManager.isBossLevel(15));
        assertTrue(adManager.isBossLevel(20));
        assertTrue(adManager.isBossLevel(25));
        assertTrue(adManager.isBossLevel(50));
        assertTrue(adManager.isBossLevel(100));
        assertTrue(adManager.isBossLevel(490));
    }

    @Test
    public void testOnEndlessModeQuitFallback() {
        AdManager adManager = AdManager.getInstance();
        final boolean[] executed = {false};
        adManager.onEndlessModeQuit(null, () -> executed[0] = true);
        assertTrue("onComplete should be invoked even when interstitial ad is not yet loaded", executed[0]);
    }
}
