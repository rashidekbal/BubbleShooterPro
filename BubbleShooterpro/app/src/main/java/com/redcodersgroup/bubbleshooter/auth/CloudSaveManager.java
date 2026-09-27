package com.redcodersgroup.bubbleshooter.auth;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudSaveManager {

    private static final String TAG = "CloudSaveManager";
    public static final String SNAPSHOT_NAME = "bubbleshooter_cloud_save";

    private static volatile CloudSaveManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface SyncCallback {
        void onSyncComplete(boolean success, String message);
    }

    private CloudSaveManager() {}

    public static CloudSaveManager getInstance() {
        if (instance == null) {
            synchronized (CloudSaveManager.class) {
                if (instance == null) {
                    instance = new CloudSaveManager();
                }
            }
        }
        return instance;
    }

    /**
     * Serializes local player progress (levels, stars, highscores, diamonds, boosters, avatar, intros) into JSON.
     */
    @NonNull
    public JSONObject buildCloudSaveJson(@NonNull PreferencesManager prefs) {
        JSONObject json = new JSONObject();
        try {
            int highestLevel = prefs.getHighestUnlockedLevel();
            json.put("highestUnlockedLevel", highestLevel);
            json.put("diamonds", prefs.getDiamonds());
            json.put("booster_bomb", prefs.getBombBoosters());
            json.put("booster_rainbow", prefs.getRainbowBoosters());
            json.put("booster_fireball", prefs.getFireballBoosters());
            json.put("booster_lightning", prefs.getLightningBoosters());
            json.put("endlessHighScore", prefs.getEndlessHighScore());
            json.put("playerName", prefs.getPlayerName());
            json.put("playerAvatar", prefs.getPlayerAvatar());
            json.put("lastSavedTimestamp", System.currentTimeMillis());

            // Track claimed booster intros so unlock rewards are never re-granted on reinstall
            json.put("seen_booster_intro_BOMB", prefs.hasSeenBoosterIntro("BOMB") || highestLevel >= PreferencesManager.UNLOCK_LEVEL_BOMB);
            json.put("seen_booster_intro_RAINBOW", prefs.hasSeenBoosterIntro("RAINBOW") || highestLevel >= PreferencesManager.UNLOCK_LEVEL_RAINBOW);
            json.put("seen_booster_intro_FIREBALL", prefs.hasSeenBoosterIntro("FIREBALL") || highestLevel >= PreferencesManager.UNLOCK_LEVEL_FIREBALL);
            json.put("seen_booster_intro_LIGHTNING", prefs.hasSeenBoosterIntro("LIGHTNING") || highestLevel >= PreferencesManager.UNLOCK_LEVEL_LIGHTNING);

            // Track claimed world gifts
            JSONObject giftsObj = new JSONObject();
            for (int w = 1; w <= 5; w++) {
                for (int g = 0; g <= 2; g++) {
                    if (prefs.hasClaimedWorldGift(w, g)) {
                        giftsObj.put(w + "_" + g, true);
                    }
                }
            }
            json.put("claimedWorldGifts", giftsObj);

            // Star ratings and High Scores per level
            JSONObject starsObj = new JSONObject();
            JSONObject scoresObj = new JSONObject();
            for (int i = 1; i <= Math.max(100, highestLevel + 5); i++) {
                int s = prefs.getStarsForLevel(i);
                if (s > 0) {
                    starsObj.put(String.valueOf(i), s);
                }
                int sc = prefs.getHighScoreForLevel(i);
                if (sc > 0) {
                    scoresObj.put(String.valueOf(i), sc);
                }
            }
            json.put("levelStars", starsObj);
            json.put("levelScores", scoresObj);
        } catch (Exception e) {
            Log.e(TAG, "Error building cloud save JSON", e);
        }
        return json;
    }

    /**
     * Merges remote cloud JSON data with local PreferencesManager.
     * Always preserves the higher progress (higher level, highest stars, max diamonds & boosters).
     *
     * @return true if local data was upgraded/changed from cloud.
     */
    public boolean mergeCloudDataWithLocal(@NonNull JSONObject remoteJson, @NonNull PreferencesManager prefs) {
        boolean changed = false;
        try {
            int remoteLevel = remoteJson.optInt("highestUnlockedLevel", 1);
            int localLevel = prefs.getHighestUnlockedLevel();
            int finalLevel = Math.max(remoteLevel, localLevel);
            if (remoteLevel > localLevel) {
                prefs.unlockLevel(remoteLevel);
                changed = true;
            }

            int remoteDiamonds = remoteJson.optInt("diamonds", 0);
            int localDiamonds = prefs.getDiamonds();
            if (remoteDiamonds > localDiamonds) {
                prefs.setDiamonds(remoteDiamonds);
                changed = true;
            }

            // Restore exact saved boosters from cloud
            int remoteBomb = remoteJson.optInt("booster_bomb", 0);
            if (remoteBomb > prefs.getBombBoosters()) {
                prefs.setBombBoosters(remoteBomb);
                changed = true;
            }
            int remoteRainbow = remoteJson.optInt("booster_rainbow", 0);
            if (remoteRainbow > prefs.getRainbowBoosters()) {
                prefs.setRainbowBoosters(remoteRainbow);
                changed = true;
            }
            int remoteFireball = remoteJson.optInt("booster_fireball", 0);
            if (remoteFireball > prefs.getFireballBoosters()) {
                prefs.setFireballBoosters(remoteFireball);
                changed = true;
            }
            int remoteLightning = remoteJson.optInt("booster_lightning", 0);
            if (remoteLightning > prefs.getLightningBoosters()) {
                prefs.setLightningBoosters(remoteLightning);
                changed = true;
            }

            // Mark booster intros as seen so unlock rewards (+5 free boosters) are NOT re-granted on reinstall
            if (finalLevel >= PreferencesManager.UNLOCK_LEVEL_BOMB || remoteJson.optBoolean("seen_booster_intro_BOMB", false)) {
                prefs.setSeenBoosterIntro("BOMB", true);
            }
            if (finalLevel >= PreferencesManager.UNLOCK_LEVEL_RAINBOW || remoteJson.optBoolean("seen_booster_intro_RAINBOW", false)) {
                prefs.setSeenBoosterIntro("RAINBOW", true);
            }
            if (finalLevel >= PreferencesManager.UNLOCK_LEVEL_FIREBALL || remoteJson.optBoolean("seen_booster_intro_FIREBALL", false)) {
                prefs.setSeenBoosterIntro("FIREBALL", true);
            }
            if (finalLevel >= PreferencesManager.UNLOCK_LEVEL_LIGHTNING || remoteJson.optBoolean("seen_booster_intro_LIGHTNING", false)) {
                prefs.setSeenBoosterIntro("LIGHTNING", true);
            }

            // Restore claimed world gifts so duplicate mystery gifts cannot be re-claimed on reinstall
            JSONObject remoteGifts = remoteJson.optJSONObject("claimedWorldGifts");
            if (remoteGifts != null) {
                Iterator<String> gKeys = remoteGifts.keys();
                while (gKeys.hasNext()) {
                    String key = gKeys.next();
                    String[] parts = key.split("_");
                    if (parts.length == 2) {
                        try {
                            int w = Integer.parseInt(parts[0]);
                            int g = Integer.parseInt(parts[1]);
                            prefs.setClaimedWorldGift(w, g, true);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Endless High Score
            int remoteEndless = remoteJson.optInt("endlessHighScore", 0);
            if (remoteEndless > prefs.getEndlessHighScore()) {
                prefs.setEndlessHighScore(remoteEndless);
                changed = true;
            }

            // Profile info
            String remoteAvatar = remoteJson.optString("playerAvatar", "");
            if (!remoteAvatar.isEmpty() && "avatar_hero".equals(prefs.getPlayerAvatar())) {
                prefs.setPlayerAvatar(remoteAvatar);
                changed = true;
            }

            // Level Stars & Scores
            JSONObject remoteStars = remoteJson.optJSONObject("levelStars");
            if (remoteStars != null) {
                Iterator<String> keys = remoteStars.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    try {
                        int lvl = Integer.parseInt(k);
                        int rStars = remoteStars.getInt(k);
                        int lStars = prefs.getStarsForLevel(lvl);
                        if (rStars > lStars) {
                            prefs.setStarsForLevel(lvl, rStars);
                            changed = true;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            JSONObject remoteScores = remoteJson.optJSONObject("levelScores");
            if (remoteScores != null) {
                Iterator<String> keys = remoteScores.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    try {
                        int lvl = Integer.parseInt(k);
                        int rScore = remoteScores.getInt(k);
                        int lScore = prefs.getHighScoreForLevel(lvl);
                        if (rScore > lScore) {
                            prefs.setHighScoreForLevel(lvl, rScore);
                            changed = true;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error merging cloud data", e);
        }
        return changed;
    }

    /**
     * Saves the current local player state to Google Play Games Cloud Snapshot.
     */
    public void saveToCloud(@NonNull Activity activity) {
        if (!PlayGamesAuthManager.getInstance().isAuthenticated()) {
            Log.d(TAG, "Not signed in to Play Games. Cloud save skipped.");
            return;
        }

        executor.execute(() -> {
            try {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(activity);
                Task<SnapshotsClient.DataOrConflict<Snapshot>> openTask = snapshotsClient.open(
                        SNAPSHOT_NAME,
                        true,
                        SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
                );
                Tasks.await(openTask);

                if (!openTask.isSuccessful() || openTask.getResult() == null) {
                    Log.w(TAG, "Failed to open cloud save snapshot for writing");
                    return;
                }

                Snapshot snapshot = openTask.getResult().getData();
                if (snapshot == null) {
                    Log.w(TAG, "Snapshot is null");
                    return;
                }

                PreferencesManager prefs = new PreferencesManager(activity);
                JSONObject json = buildCloudSaveJson(prefs);
                byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);

                snapshot.getSnapshotContents().writeBytes(bytes);

                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                        .setDescription("Level " + prefs.getHighestUnlockedLevel() + " • " + prefs.getDiamonds() + " 💎")
                        .setPlayedTimeMillis(System.currentTimeMillis())
                        .build();

                Task<SnapshotMetadata> commitTask = snapshotsClient.commitAndClose(snapshot, metadataChange);
                Tasks.await(commitTask);
                Log.d(TAG, "Cloud save committed successfully! Level " + prefs.getHighestUnlockedLevel() + ", " + prefs.getDiamonds() + " 💎");
            } catch (Exception e) {
                Log.e(TAG, "Error writing cloud save snapshot", e);
            }
        });
    }

    /**
     * Reads cloud snapshot from Play Games, merges it with local data, and restores progress.
     */
    public void loadAndSyncFromCloud(@NonNull Activity activity, @Nullable SyncCallback callback) {
        if (!PlayGamesAuthManager.getInstance().isAuthenticated()) {
            Log.d(TAG, "Not signed in to Play Games. Cloud load skipped.");
            if (callback != null) {
                activity.runOnUiThread(() -> callback.onSyncComplete(false, "Not signed in to Google Play Games"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(activity);
                Task<SnapshotsClient.DataOrConflict<Snapshot>> openTask = snapshotsClient.open(
                        SNAPSHOT_NAME,
                        true,
                        SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
                );
                Tasks.await(openTask);

                if (!openTask.isSuccessful() || openTask.getResult() == null) {
                    Log.w(TAG, "Failed to open cloud save snapshot for reading");
                    if (callback != null) {
                        activity.runOnUiThread(() -> callback.onSyncComplete(false, "Failed to open cloud save"));
                    }
                    return;
                }

                Snapshot snapshot = openTask.getResult().getData();
                if (snapshot == null) {
                    Log.w(TAG, "Snapshot data is null");
                    if (callback != null) {
                        activity.runOnUiThread(() -> callback.onSyncComplete(false, "Snapshot data is null"));
                    }
                    return;
                }

                byte[] contents = snapshot.getSnapshotContents().readFully();
                PreferencesManager prefs = new PreferencesManager(activity);

                if (contents == null || contents.length == 0) {
                    Log.d(TAG, "Cloud snapshot is currently empty. Uploading local state to initialize cloud save...");
                    JSONObject json = buildCloudSaveJson(prefs);
                    snapshot.getSnapshotContents().writeBytes(json.toString().getBytes(StandardCharsets.UTF_8));
                    SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                            .setDescription("Level " + prefs.getHighestUnlockedLevel() + " • " + prefs.getDiamonds() + " 💎")
                            .setPlayedTimeMillis(System.currentTimeMillis())
                            .build();
                    snapshotsClient.commitAndClose(snapshot, metadataChange);
                    if (callback != null) {
                        activity.runOnUiThread(() -> callback.onSyncComplete(true, "Cloud save initialized"));
                    }
                    return;
                }

                String jsonStr = new String(contents, StandardCharsets.UTF_8);
                JSONObject remoteJson = new JSONObject(jsonStr);

                boolean updatedLocal = mergeCloudDataWithLocal(remoteJson, prefs);

                // Commit back merged state to cloud
                JSONObject mergedJson = buildCloudSaveJson(prefs);
                snapshot.getSnapshotContents().writeBytes(mergedJson.toString().getBytes(StandardCharsets.UTF_8));
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                        .setDescription("Level " + prefs.getHighestUnlockedLevel() + " • " + prefs.getDiamonds() + " 💎")
                        .setPlayedTimeMillis(System.currentTimeMillis())
                        .build();
                snapshotsClient.commitAndClose(snapshot, metadataChange);

                Log.d(TAG, "Cloud sync complete! Local updated: " + updatedLocal + ", Current level: " + prefs.getHighestUnlockedLevel());
                if (callback != null) {
                    activity.runOnUiThread(() -> callback.onSyncComplete(true, "Cloud progress synced successfully!"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing cloud save snapshot", e);
                if (callback != null) {
                    activity.runOnUiThread(() -> callback.onSyncComplete(false, e.getMessage()));
                }
            }
        });
    }
}
