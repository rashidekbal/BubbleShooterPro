package com.redcodersgroup.bubbleshooter.level;

import android.content.Context;
import android.util.Log;

import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelLoader {

    public static Level loadLevelFromAssets(Context context, int levelNumber) {
        String fileName = String.format("levels/level_%02d.json", levelNumber);
        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return parseJson(sb.toString());
        } catch (Exception e) {
            // Fallback generated level
            return createFallbackLevel(levelNumber);
        }
    }

    public static Level parseJson(String jsonString) throws Exception {
        JSONObject obj = new JSONObject(jsonString);
        int levelNum = obj.optInt("level", 1);
        int shots = obj.optInt("shots", 25);

        // Colors
        List<BubbleColor> colors = new ArrayList<>();
        if (obj.has("colors")) {
            JSONArray colorsArr = obj.getJSONArray("colors");
            for (int i = 0; i < colorsArr.length(); i++) {
                try {
                    colors.add(BubbleColor.valueOf(colorsArr.getString(i).toUpperCase()));
                } catch (Exception ignored) {
                    Log.d("parseJson",ignored.toString());
                }
            }
        }
        if (colors.isEmpty()) {
            colors.addAll(Arrays.asList(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.YELLOW, BubbleColor.GREEN));
        }

        // Objective
        LevelObjective objective = LevelObjective.clearAll();
        if (obj.has("objective")) {
            JSONObject objObj = obj.getJSONObject("objective");
            String typeStr = objObj.optString("type", "CLEAR_ALL");
            int target = objObj.optInt("target", 0);
            if ("DROP_COUNT".equalsIgnoreCase(typeStr)) {
                objective = LevelObjective.dropCount(target);
            } else if ("SCORE_TARGET".equalsIgnoreCase(typeStr)) {
                objective = LevelObjective.scoreTarget(target);
            } else if ("POP_COLOR".equalsIgnoreCase(typeStr)) {
                String colStr = objObj.optString("color", "RED");
                BubbleColor bc = BubbleColor.RED;
                try {
                    bc = BubbleColor.valueOf(colStr.toUpperCase());
                } catch (Exception ignored) {}
                objective = LevelObjective.popColor(bc, target);
            }
        }

        // Star Thresholds
        int[] stars = new int[]{1000, 2500, 4500};
        if (obj.has("starThresholds")) {
            JSONArray starsArr = obj.getJSONArray("starThresholds");
            if (starsArr.length() >= 3) {
                stars = new int[]{starsArr.getInt(0), starsArr.getInt(1), starsArr.getInt(2)};
            }
        }

        // Rows
        List<String> rows = new ArrayList<>();
        if (obj.has("rows")) {
            JSONArray rowsArr = obj.getJSONArray("rows");
            for (int i = 0; i < rowsArr.length(); i++) {
                rows.add(rowsArr.getString(i));
            }
        }

        return new Level(levelNum, shots, colors, objective, stars, rows);
    }

    public static Level createFallbackLevel(int levelNumber) {
        List<BubbleColor> colors = Arrays.asList(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW);
        List<String> rows = new ArrayList<>();
        rows.add("RRBBYYGGR");
        rows.add("RRBBYYGG");
        rows.add("BBYYGGRRB");
        rows.add("BBYYGGRR");
        rows.add("YYGGRRBBY");
        rows.add("YYGGRRBB");

        return new Level(
                levelNumber,
                26,
                colors,
                LevelObjective.clearAll(),
                new int[]{1200, 2400, 3800},
                rows
        );
    }
}
