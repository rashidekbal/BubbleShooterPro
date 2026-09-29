package com.redcodersgroup.bubbleshooter.store;

public class StoreManager {

    // Rate: 0.5 Rs per Diamond (₹0.50)
    public static final float RATE_INR_PER_DIAMOND = 0.5f;

    // Diamond Acquisition Packs
    public static final int DIAMONDS_DAILY_FREE = 3;
    public static final int DIAMONDS_POCKET = 50;      // 50 Diamonds = ₹29 / $0.49
    public static final int DIAMONDS_POUCH = 140;      // 140 Diamonds = ₹75 / $0.99
    public static final int DIAMONDS_SACK = 500;       // 500 Diamonds = ₹249 / $2.99
    public static final int DIAMONDS_CHEST = 1600;     // 1,600 Diamonds = ₹699 / $7.99

    // Google Play IAP Product IDs & Prices (INR)
    public static final String SKU_DIAMONDS_50 = "diamonds_50";
    public static final String SKU_DIAMONDS_150 = "diamonds_150";
    public static final String SKU_DIAMONDS_500 = "diamonds_500";
    public static final String SKU_DIAMONDS_1500 = "diamonds_1500";

    // Power-Up & Booster Costs (in Diamonds)
    public static final int COST_BOMB_SINGLE = 15;
    public static final int COST_FIREBALL_SINGLE = 15;
    public static final int COST_LIGHTNING_SINGLE = 20;
    public static final int COST_RAINBOW_SINGLE = 20;

    public static final int COST_BOMB_PACK = 40;
    public static final int COST_FIREBALL_PACK = 40;
    public static final int COST_LIGHTNING_PACK = 45;
    public static final int COST_RAINBOW_PACK = 50;
    public static final int COST_MEGA_BUNDLE = 120;

    // Hearts / Lives Refill Cost
    public static final int COST_ONE_HEART = 6;
    public static final int COST_TRIPLE_HEARTS = 15;
    public static final int COST_LIVES_REFILL = 25;
    public static final int MAX_LIVES = 5;
    

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
