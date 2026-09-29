package com.redcodersgroup.bubbleshooter.data;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "bubble_shooter_prefs";
    private static final String KEY_SOUND = "key_sound_enabled";
    private static final String KEY_MUSIC = "key_music_enabled";
    private static final String KEY_HAPTIC = "key_haptic_enabled";
    private static final String KEY_HIGHEST_LEVEL = "key_highest_unlocked_level";
    private static final String KEY_STARS_PREFIX = "key_stars_lvl_";
    private static final String KEY_SCORE_PREFIX = "key_score_lvl_";
    private static final String KEY_BOOSTER_BOMB = "key_booster_bomb";
    private static final String KEY_BOOSTER_RAINBOW = "key_booster_rainbow";
    private static final String KEY_BOOSTER_LIGHTNING = "key_booster_lightning";
    private static final String KEY_BOOSTER_FIREBALL = "key_booster_fireball";
    private static final String KEY_ENDLESS_HIGH_SCORE = "key_endless_high_score";
    private static final String KEY_PLAYER_NAME = "key_player_name";
    private static final String KEY_PLAYER_AVATAR = "key_player_avatar";
    private static final String KEY_DIAMONDS = "key_player_diamonds";
    private static final String KEY_LIVES = "key_player_lives";
    private static final String KEY_LAST_LIFE_LOST_TIMESTAMP = "key_last_life_lost_timestamp";
    private static final String KEY_FREE_DIAMONDS_CLAIM_DATE = "key_free_diamonds_claim_date";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public boolean isMusicEnabled() {
        return prefs.getBoolean(KEY_MUSIC, true);
    }

    public void setMusicEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply();
    }

    public boolean isHapticEnabled() {
        return prefs.getBoolean(KEY_HAPTIC, true);
    }

    public void setHapticEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply();
    }

    public int getHighestUnlockedLevel() {
        return prefs.getInt(KEY_HIGHEST_LEVEL, 1);
    }

    public void unlockLevel(int level) {
        if (level > getHighestUnlockedLevel()) {
            prefs.edit().putInt(KEY_HIGHEST_LEVEL, level).apply();
        }
    }

    public int getStarsForLevel(int level) {
        return prefs.getInt(KEY_STARS_PREFIX + level, 0);
    }

    public void setStarsForLevel(int level, int stars) {
        int current = getStarsForLevel(level);
        if (stars > current) {
            prefs.edit().putInt(KEY_STARS_PREFIX + level, stars).apply();
        }
    }

    public int getHighScoreForLevel(int level) {
        return prefs.getInt(KEY_SCORE_PREFIX + level, 0);
    }

    public void setHighScoreForLevel(int level, int score) {
        int current = getHighScoreForLevel(level);
        if (score > current) {
            prefs.edit().putInt(KEY_SCORE_PREFIX + level, score).apply();
        }
    }

    public static final int UNLOCK_LEVEL_BOMB = 21;
    public static final int UNLOCK_LEVEL_RAINBOW = 41;
    public static final int UNLOCK_LEVEL_FIREBALL = 61;
    public static final int UNLOCK_LEVEL_LIGHTNING = 81;

    public static final int FREE_BOOSTERS_ON_UNLOCK = 5;

    public int getBombBoosters() {
        return prefs.getInt(KEY_BOOSTER_BOMB, 0);
    }

    public void setBombBoosters(int count) {
        prefs.edit().putInt(KEY_BOOSTER_BOMB, Math.max(0, count)).apply();
    }

    public int getRainbowBoosters() {
        return prefs.getInt(KEY_BOOSTER_RAINBOW, 0);
    }

    public void setRainbowBoosters(int count) {
        prefs.edit().putInt(KEY_BOOSTER_RAINBOW, Math.max(0, count)).apply();
    }

    public int getLightningBoosters() {
        return prefs.getInt(KEY_BOOSTER_LIGHTNING, 0);
    }

    public void setLightningBoosters(int count) {
        prefs.edit().putInt(KEY_BOOSTER_LIGHTNING, Math.max(0, count)).apply();
    }

    public int getFireballBoosters() {
        return prefs.getInt(KEY_BOOSTER_FIREBALL, 0);
    }

    public void setFireballBoosters(int count) {
        prefs.edit().putInt(KEY_BOOSTER_FIREBALL, Math.max(0, count)).apply();
    }

    public boolean isBoosterUnlocked(com.redcodersgroup.bubbleshooter.bubble.BubbleType type, int levelNumber) {
        if (type == null) return true;
        switch (type) {
            case BOMB:
                return levelNumber >= UNLOCK_LEVEL_BOMB;
            case RAINBOW:
                return levelNumber >= UNLOCK_LEVEL_RAINBOW;
            case FIREBALL:
                return levelNumber >= UNLOCK_LEVEL_FIREBALL;
            case LIGHTNING:
                return levelNumber >= UNLOCK_LEVEL_LIGHTNING;
            default:
                return true;
        }
    }

    public boolean isBoosterUnlockedGlobally(com.redcodersgroup.bubbleshooter.bubble.BubbleType type) {
        return isBoosterUnlocked(type, getHighestUnlockedLevel());
    }

    public boolean hasSeenBoosterIntro(String boosterType) {
        if (boosterType == null) return true;
        return prefs.getBoolean("seen_booster_intro_" + boosterType.toUpperCase(java.util.Locale.ROOT), false);
    }

    public void setSeenBoosterIntro(String boosterType, boolean seen) {
        if (boosterType == null) return;
        prefs.edit().putBoolean("seen_booster_intro_" + boosterType.toUpperCase(java.util.Locale.ROOT), seen).apply();
    }

    public void grantBoosterUnlockReward(com.redcodersgroup.bubbleshooter.bubble.BubbleType type) {
        if (type == null) return;
        switch (type) {
            case BOMB:
                addBombBoosters(FREE_BOOSTERS_ON_UNLOCK);
                break;
            case RAINBOW:
                addRainbowBoosters(FREE_BOOSTERS_ON_UNLOCK);
                break;
            case FIREBALL:
                addFireballBoosters(FREE_BOOSTERS_ON_UNLOCK);
                break;
            case LIGHTNING:
                addLightningBoosters(FREE_BOOSTERS_ON_UNLOCK);
                break;
            default:
                break;
        }
    }

    public int getEndlessHighScore() {
        return prefs.getInt(KEY_ENDLESS_HIGH_SCORE, 0);
    }

    public void setEndlessHighScore(int score) {
        int current = getEndlessHighScore();
        if (score > current) {
            prefs.edit().putInt(KEY_ENDLESS_HIGH_SCORE, score).apply();
        }
    }

    public boolean hasClaimedWorldGift(int world, int giftIndex) {
        return prefs.getBoolean("world_gift_" + world + "_" + giftIndex, false);
    }

    public void setClaimedWorldGift(int world, int giftIndex, boolean claimed) {
        prefs.edit().putBoolean("world_gift_" + world + "_" + giftIndex, claimed).apply();
    }

    public String getPlayerName() {
        return prefs.getString(KEY_PLAYER_NAME, "Player");
    }

    public void setPlayerName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            prefs.edit().putString(KEY_PLAYER_NAME, name.trim()).apply();
        }
    }

    public String getPlayerAvatar() {
        return prefs.getString(KEY_PLAYER_AVATAR, "avatar_hero");
    }

    public void setPlayerAvatar(String avatarId) {
        if (avatarId != null && !avatarId.trim().isEmpty()) {
            prefs.edit().putString(KEY_PLAYER_AVATAR, avatarId.trim()).apply();
        }
    }

    public static final int INITIAL_DIAMONDS = 25;

    public int getDiamonds() {
        if (!prefs.contains(KEY_DIAMONDS)) {
            prefs.edit().putInt(KEY_DIAMONDS, INITIAL_DIAMONDS).apply();
            return INITIAL_DIAMONDS;
        }
        return prefs.getInt(KEY_DIAMONDS, INITIAL_DIAMONDS);
    }

    public void setDiamonds(int count) {
        prefs.edit().putInt(KEY_DIAMONDS, Math.max(0, count)).apply();
    }

    public void addDiamonds(int count) {
        if (count > 0) {
            setDiamonds(getDiamonds() + count);
        }
    }

    public boolean spendDiamonds(int cost) {
        if (cost <= 0) return true;
        int current = getDiamonds();
        if (current >= cost) {
            setDiamonds(current - cost);
            return true;
        }
        return false;
    }

    public int getLives() {
        int lives = prefs.getInt(KEY_LIVES, 5);
        if (lives >= 5) {
            return 5;
        }
        long lastLost = prefs.getLong(KEY_LAST_LIFE_LOST_TIMESTAMP, 0);
        if (lastLost > 0) {
            long elapsed = System.currentTimeMillis() - lastLost;
            long REGEN_INTERVAL_MS = 20L * 60 * 1000;
            int recovered = (int) (elapsed / REGEN_INTERVAL_MS);
            if (recovered > 0) {
                int newLives = Math.min(5, lives + recovered);
                long remainder = elapsed % REGEN_INTERVAL_MS;
                prefs.edit()
                        .putInt(KEY_LIVES, newLives)
                        .putLong(KEY_LAST_LIFE_LOST_TIMESTAMP, newLives >= 5 ? 0 : System.currentTimeMillis() - remainder)
                        .apply();
                return newLives;
            }
        }
        return lives;
    }

    public void setLives(int lives) {
        int capped = Math.max(0, Math.min(5, lives));
        if (capped == 5) {
            prefs.edit().putInt(KEY_LIVES, 5).putLong(KEY_LAST_LIFE_LOST_TIMESTAMP, 0).apply();
        } else {
            if (!prefs.contains(KEY_LAST_LIFE_LOST_TIMESTAMP) || prefs.getLong(KEY_LAST_LIFE_LOST_TIMESTAMP, 0) == 0) {
                prefs.edit().putInt(KEY_LIVES, capped).putLong(KEY_LAST_LIFE_LOST_TIMESTAMP, System.currentTimeMillis()).apply();
            } else {
                prefs.edit().putInt(KEY_LIVES, capped).apply();
            }
        }
    }

    public void addLives(int count) {
        setLives(getLives() + count);
    }

    public void refillLives() {
        setLives(5);
    }

    public void deductLife() {
        int current = getLives();
        if (current > 0) {
            setLives(current - 1);
        }
    }

    public long getSecondsUntilNextLife() {
        if (getLives() >= 5) return 0;
        long lastLost = prefs.getLong(KEY_LAST_LIFE_LOST_TIMESTAMP, 0);
        if (lastLost == 0) return 0;
        long REGEN_INTERVAL_MS = 20L * 60 * 1000;
        long elapsed = System.currentTimeMillis() - lastLost;
        long remMs = REGEN_INTERVAL_MS - (elapsed % REGEN_INTERVAL_MS);
        return Math.max(0, remMs / 1000);
    }

    public void addBombBoosters(int count) {
        setBombBoosters(getBombBoosters() + count);
    }

    public void addRainbowBoosters(int count) {
        setRainbowBoosters(getRainbowBoosters() + count);
    }

    public void addLightningBoosters(int count) {
        setLightningBoosters(getLightningBoosters() + count);
    }

    public void addFireballBoosters(int count) {
        setFireballBoosters(getFireballBoosters() + count);
    }

    public boolean consumeBombBooster() {
        int current = getBombBoosters();
        if (current > 0) {
            setBombBoosters(current - 1);
            return true;
        }
        return false;
    }

    public boolean consumeRainbowBooster() {
        int current = getRainbowBoosters();
        if (current > 0) {
            setRainbowBoosters(current - 1);
            return true;
        }
        return false;
    }

    public boolean consumeLightningBooster() {
        int current = getLightningBoosters();
        if (current > 0) {
            setLightningBoosters(current - 1);
            return true;
        }
        return false;
    }

    public boolean consumeFireballBooster() {
        int current = getFireballBoosters();
        if (current > 0) {
            setFireballBoosters(current - 1);
            return true;
        }
        return false;
    }

    public void refundBooster(com.redcodersgroup.bubbleshooter.bubble.BubbleType type) {
        if (type == null) return;
        switch (type) {
            case BOMB:
                addBombBoosters(1);
                break;
            case RAINBOW:
                addRainbowBoosters(1);
                break;
            case FIREBALL:
                addFireballBoosters(1);
                break;
            case LIGHTNING:
                addLightningBoosters(1);
                break;
            default:
                break;
        }
    }

    public boolean canClaimDailyFreeDiamonds() {
        long lastClaim = prefs.getLong(KEY_FREE_DIAMONDS_CLAIM_DATE, 0);
        long now = System.currentTimeMillis();
        // Allow once every 24 hours (or if never claimed)
        return (now - lastClaim) >= (24L * 60 * 60 * 1000);
    }

    public void markDailyFreeDiamondsClaimed() {
        prefs.edit().putLong(KEY_FREE_DIAMONDS_CLAIM_DATE, System.currentTimeMillis()).apply();
    }

    public void resetProgress() {
        prefs.edit().clear().apply();
    }
}
