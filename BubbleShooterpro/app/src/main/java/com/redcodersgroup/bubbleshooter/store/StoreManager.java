package com.redcodersgroup.bubbleshooter.store;

public class StoreManager {

    // Diamond Acquisition Packs
    public static final int DIAMONDS_DAILY_FREE = 5;
    public static final int DIAMONDS_POUCH = 150;
    public static final int DIAMONDS_SACK = 500;
    public static final int DIAMONDS_CHEST = 1500;

    // Power-Up & Booster Costs (in Diamonds)
    public static final int COST_BOMB_PACK = 40;
    public static final int COST_FIREBALL_PACK = 40;
    public static final int COST_LIGHTNING_PACK = 40;
    public static final int COST_RAINBOW_PACK = 50;
    public static final int COST_MEGA_BUNDLE = 120;

    // Hearts / Lives Refill Cost
    public static final int COST_LIVES_REFILL = 25;
    public static final int MAX_LIVES = 5;

    // Items quantities per purchase
    public static final int QTY_BOOSTER_PACK = 3;
    public static final int QTY_MEGA_BUNDLE_BOOSTERS = 2;

    public static boolean canAfford(int diamondBalance, int cost) {
        return cost >= 0 && diamondBalance >= cost;
    }

    public static int deductDiamonds(int diamondBalance, int cost) {
        if (!canAfford(diamondBalance, cost)) {
            throw new IllegalArgumentException("Insufficient diamonds: balance=" + diamondBalance + ", cost=" + cost);
        }
        return diamondBalance - cost;
    }
}
