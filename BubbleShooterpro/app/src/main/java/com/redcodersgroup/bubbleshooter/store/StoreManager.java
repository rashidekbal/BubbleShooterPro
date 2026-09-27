package com.redcodersgroup.bubbleshooter.store;

public class StoreManager {

    // Rate: 0.5 Rs per Diamond (₹0.50)
    public static final float RATE_INR_PER_DIAMOND = 0.5f;

    // Diamond Acquisition Packs
    public static final int DIAMONDS_DAILY_FREE = 5;
    public static final int DIAMONDS_POUCH = 150;      // 150 * 0.5 = ₹75
    public static final int DIAMONDS_SACK = 500;       // 500 * 0.5 = ₹250
    public static final int DIAMONDS_CHEST = 1500;     // 1500 * 0.5 = ₹750

    // Google Play IAP Product IDs & Prices (INR)
    public static final String SKU_DIAMONDS_150 = "diamonds_150";
    public static final String SKU_DIAMONDS_500 = "diamonds_500";
    public static final String SKU_DIAMONDS_1500 = "diamonds_1500";

    public static final String PRICE_LABEL_150 = "₹75";
    public static final String PRICE_LABEL_500 = "₹250";
    public static final String PRICE_LABEL_1500 = "₹750";

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
