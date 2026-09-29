package com.redcodersgroup.bubbleshooter.data;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WorldConfigManager {
    private static final String TAG = "WorldConfigManager";
    private static final String CONFIG_FILE_NAME = "worlds_config.json";

    private static WorldConfigManager instance;
    private final Context context;
    private final List<WorldModel> worlds = new ArrayList<>();

    public static class LevelCoord {
        public final int level;
        public final float x;
        public final float y;

        public LevelCoord(int level, float x, float y) {
            this.level = level;
            this.x = x;
            this.y = y;
        }
    }

    public static class GiftConfig {
        public final int giftIndex;
        public final String name;
        public final float x;
        public final float y;
        public final int requiredLevelOffset;
        public final int rewardDiamonds;

        public GiftConfig(int giftIndex, String name, float x, float y, int requiredLevelOffset, int rewardDiamonds) {
            this.giftIndex = giftIndex;
            this.name = name;
            this.x = x;
            this.y = y;
            this.requiredLevelOffset = requiredLevelOffset;
            this.rewardDiamonds = rewardDiamonds;
        }
    }

    public static class WorldModel {
        public final int worldNumber;
        public final String name;
        public final String subtitle;
        public final String mapBackground;
        public final String gameBackground;
        public final int levelsCount;
        public final int startLevel;
        public final List<LevelCoord> levels;
        public final List<GiftConfig> gifts;

        public WorldModel(int worldNumber, String name, String subtitle, String mapBackground,
                          String gameBackground, int levelsCount, int startLevel,
                          List<LevelCoord> levels, List<GiftConfig> gifts) {
            this.worldNumber = worldNumber;
            this.name = name;
            this.subtitle = subtitle;
            this.mapBackground = mapBackground;
            this.gameBackground = gameBackground;
            this.levelsCount = levelsCount;
            this.startLevel = startLevel;
            this.levels = levels;
            this.gifts = gifts;
        }

        public int getEndLevel() {
            return startLevel + levelsCount - 1;
        }
    }

    private WorldConfigManager(Context context) {
        this.context = context.getApplicationContext();
        loadConfig();
    }

    public static synchronized WorldConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new WorldConfigManager(context);
        }
        return instance;
    }

    public void reload() {
        worlds.clear();
        loadConfig();
    }

    public List<WorldModel> getWorlds() {
        return worlds;
    }

    public int getTotalWorlds() {
        return Math.max(1, worlds.size());
    }

    public WorldModel getWorldByIndex(int index) {
        if (index >= 0 && index < worlds.size()) {
            return worlds.get(index);
        }
        return worlds.isEmpty() ? createDefaultWorld(1, 1) : worlds.get(0);
    }

    public WorldModel getWorld(int worldNumber) {
        for (WorldModel w : worlds) {
            if (w.worldNumber == worldNumber) {
                return w;
            }
        }
        return getWorldByIndex(worldNumber - 1);
    }

    public WorldModel getWorldForLevel(int levelNumber) {
        for (WorldModel w : worlds) {
            if (levelNumber >= w.startLevel && levelNumber <= w.getEndLevel()) {
                return w;
            }
        }
        // Fallback: if higher than all defined worlds, return the last world
        if (!worlds.isEmpty()) {
            return worlds.get(worlds.size() - 1);
        }
        return createDefaultWorld(1, 1);
    }

    public int getWorldIndexForLevel(int levelNumber) {
        for (int i = 0; i < worlds.size(); i++) {
            WorldModel w = worlds.get(i);
            if (levelNumber >= w.startLevel && levelNumber <= w.getEndLevel()) {
                return i;
            }
        }
        return Math.max(0, worlds.size() - 1);
    }

    private void loadConfig() {
        try {
            InputStream is = context.getAssets().open(CONFIG_FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray worldsArray = root.getJSONArray("worlds");

            int runningStartLevel = 1;
            for (int i = 0; i < worldsArray.length(); i++) {
                JSONObject wObj = worldsArray.getJSONObject(i);
                int worldNumber = wObj.optInt("worldNumber", i + 1);
                String name = wObj.optString("name", "World " + worldNumber);
                String subtitle = wObj.optString("subtitle", "WORLD " + worldNumber + " • " + name.toUpperCase());
                String mapBg = wObj.optString("mapBackground", "bg_map_world_" + worldNumber);
                String gameBg = wObj.optString("gameBackground", "bg_game_world_" + worldNumber);
                int levelsCount = wObj.optInt("levelsCount", 10);
                int startLevel = wObj.optInt("startLevel", runningStartLevel);

                // Parse Level Coordinates (supports both [{"level": 1, "x": .., "y": ..}] and [[x, y], ...])
                List<LevelCoord> coords = new ArrayList<>();
                JSONArray levelsArr = wObj.optJSONArray("levels");
                if (levelsArr == null) {
                    levelsArr = wObj.optJSONArray("levelCoords");
                }

                if (levelsArr != null) {
                    for (int j = 0; j < levelsArr.length(); j++) {
                        Object item = levelsArr.get(j);
                        float x = 0.5f;
                        float y = 0.5f;
                        int lvl = startLevel + j;

                        if (item instanceof JSONObject) {
                            JSONObject cObj = (JSONObject) item;
                            x = (float) cObj.optDouble("x", 0.5);
                            y = (float) cObj.optDouble("y", 0.5);
                            lvl = cObj.optInt("level", startLevel + j);
                        } else if (item instanceof JSONArray) {
                            JSONArray cArr = (JSONArray) item;
                            x = (float) cArr.optDouble(0, 0.5);
                            y = (float) cArr.optDouble(1, 0.5);
                        }
                        coords.add(new LevelCoord(lvl, x, y));
                    }
                }

                // If coordinates weren't specified, use default 10-bump coords
                if (coords.isEmpty()) {
                    float[][] defaults = DEFAULT_MAP_COORDS;
                    for (int k = 0; k < Math.min(levelsCount, defaults.length); k++) {
                        coords.add(new LevelCoord(startLevel + k, defaults[k][0], defaults[k][1]));
                    }
                }

                // Parse Gifts
                List<GiftConfig> gifts = new ArrayList<>();
                JSONArray giftsArr = wObj.optJSONArray("gifts");
                if (giftsArr != null) {
                    for (int g = 0; g < giftsArr.length(); g++) {
                        JSONObject gObj = giftsArr.getJSONObject(g);
                        int giftIndex = gObj.optInt("giftIndex", g + 1);
                        String gName = gObj.optString("name", "Gift " + giftIndex);
                        float gx = (float) gObj.optDouble("x", 0.5);
                        float gy = (float) gObj.optDouble("y", 0.5);
                        int reqOffset = gObj.optInt("requiredLevelOffset", g == 0 ? 5 : 10);
                        int diamonds = gObj.optInt("rewardDiamonds", 5);
                        gifts.add(new GiftConfig(giftIndex, gName, gx, gy, reqOffset, diamonds));
                    }
                }

                worlds.add(new WorldModel(worldNumber, name, subtitle, mapBg, gameBg, levelsCount, startLevel, coords, gifts));
                runningStartLevel = startLevel + levelsCount;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading worlds_config.json, using built-in defaults: " + e.getMessage());
            loadDefaults();
        }

        if (worlds.isEmpty()) {
            loadDefaults();
        }
    }

    private void loadDefaults() {
        worlds.clear();
        worlds.add(createDefaultWorld(1, 1));
        worlds.add(createDefaultWorld(2, 11));
    }

    private static final float[][] DEFAULT_MAP_COORDS = {
            {0.5032f, 0.7122f}, // Level 1
            {0.6334f, 0.6017f}, // Level 2
            {0.6768f, 0.5458f}, // Level 3
            {0.6326f, 0.4760f}, // Level 4
            {0.5206f, 0.4375f}, // Level 5
            {0.4125f, 0.4208f}, // Level 6
            {0.4688f, 0.2798f}, // Level 7
            {0.5701f, 0.2544f}, // Level 8
            {0.5831f, 0.2057f}, // Level 9
            {0.5063f, 0.1789f}  // Level 10
    };

    private static final float[][] DEFAULT_MAP_GIFTS = {
            {0.3370f, 0.3547f}, // 🎁 Gift 1 (Viaduct Chest)
            {0.5583f, 0.1331f}  // 🎁 Gift 2 (Castle Gate Chest)
    };

    private WorldModel createDefaultWorld(int worldNumber, int startLevel) {
        String name = (worldNumber == 1) ? "Bubble Meadows" : "Whispering Woods";
        String subtitle = "WORLD " + worldNumber + " • " + name.toUpperCase();
        String mapBg = "bg_map_world_" + worldNumber;
        String gameBg = "bg_game_world_" + worldNumber;

        List<LevelCoord> coords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            coords.add(new LevelCoord(startLevel + i, DEFAULT_MAP_COORDS[i][0], DEFAULT_MAP_COORDS[i][1]));
        }

        List<GiftConfig> gifts = new ArrayList<>();
        gifts.add(new GiftConfig(1, "Viaduct Chest", DEFAULT_MAP_GIFTS[0][0], DEFAULT_MAP_GIFTS[0][1], 5, 5));
        gifts.add(new GiftConfig(2, "Castle Gate Chest", DEFAULT_MAP_GIFTS[1][0], DEFAULT_MAP_GIFTS[1][1], 10, 5));

        return new WorldModel(worldNumber, name, subtitle, mapBg, gameBg, 10, startLevel, coords, gifts);
    }
}
