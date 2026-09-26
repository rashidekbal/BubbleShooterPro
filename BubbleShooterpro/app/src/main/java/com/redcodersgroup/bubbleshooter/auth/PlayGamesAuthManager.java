package com.redcodersgroup.bubbleshooter.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.Task;
import java.lang.reflect.Proxy;

public class PlayGamesAuthManager {

    private static final String TAG = "PlayGamesAuth";
    public static final int RC_GOOGLE_SIGN_IN = 9001;

    private static volatile PlayGamesAuthManager instance;

    private boolean isAuthenticated = false;
    private String playerId;
    private String displayName;
    private String email;
    private String iconImageUri;

    public interface AuthCallback {
        void onSuccess(@NonNull Player player);
        void onFailure(Exception exception);
    }

    private PlayGamesAuthManager() {}

    public static PlayGamesAuthManager getInstance() {
        if (instance == null) {
            synchronized (PlayGamesAuthManager.class) {
                if (instance == null) {
                    instance = new PlayGamesAuthManager();
                }
            }
        }
        return instance;
    }

    public static void initialize(@NonNull Context context) {
        try {
            PlayGamesSdk.initialize(context);
            Log.d(TAG, "PlayGamesSdk initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize PlayGamesSdk", e);
        }
    }

    public GoogleSignInOptions getGoogleSignInOptions() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
    }

    public GoogleSignInClient getGoogleSignInClient(@NonNull Context context) {
        return GoogleSignIn.getClient(context, getGoogleSignInOptions());
    }

    public Intent getGoogleSignInIntent(@NonNull Context context) {
        return getGoogleSignInClient(context).getSignInIntent();
    }

    public void handleSignInResult(@Nullable Intent data, @Nullable AuthCallback callback) {
        if (data == null) {
            Log.e(TAG, "Google Sign-In returned null data");
            if (callback != null) {
                callback.onFailure(new Exception("Sign-in cancelled or returned no data"));
            }
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                this.isAuthenticated = true;
                this.playerId = account.getId();
                this.displayName = account.getDisplayName();
                this.email = account.getEmail();
                if (account.getPhotoUrl() != null) {
                    this.iconImageUri = account.getPhotoUrl().toString();
                }
                Log.d(TAG, "Google Sign-In successful: " + displayName + " (" + email + ")");
                Player player = createPlayerProxy(this.playerId, this.displayName, account.getPhotoUrl());
                if (callback != null) {
                    callback.onSuccess(player);
                }
            } else {
                this.isAuthenticated = false;
                if (callback != null) {
                    callback.onFailure(new Exception("Google Account was null"));
                }
            }
        } catch (ApiException e) {
            this.isAuthenticated = false;
            logAuthError("Google Sign-In result failed", e);
            if (callback != null) {
                callback.onFailure(e);
            }
        } catch (Exception e) {
            this.isAuthenticated = false;
            Log.e(TAG, "Error processing sign-in result", e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    public void checkSilentSignIn(@NonNull Activity activity, @Nullable AuthCallback callback) {
        // 1. Check Google Sign-In Account first
        GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(activity);
        if (lastAccount != null) {
            this.isAuthenticated = true;
            this.playerId = lastAccount.getId();
            this.displayName = lastAccount.getDisplayName();
            this.email = lastAccount.getEmail();
            if (lastAccount.getPhotoUrl() != null) {
                this.iconImageUri = lastAccount.getPhotoUrl().toString();
            }
            Log.d(TAG, "Silent sign in found cached Google Account: " + displayName);
            Player player = createPlayerProxy(this.playerId, this.displayName, lastAccount.getPhotoUrl());
            if (callback != null) {
                callback.onSuccess(player);
            }
            return;
        }

        // 2. Otherwise check Play Games v2 silent sign-in
        try {
            GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(activity);
            gamesSignInClient.isAuthenticated().addOnCompleteListener(activity, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    AuthenticationResult authResult = task.getResult();
                    isAuthenticated = authResult.isAuthenticated();
                    if (isAuthenticated) {
                        Log.d(TAG, "Silent sign in successful via Play Games, fetching player info...");
                        fetchPlayerInfo(activity, callback);
                    } else {
                        Log.i(TAG, "Silent sign in: User is not currently authenticated");
                        if (callback != null) {
                            callback.onFailure(new Exception("User not authenticated"));
                        }
                    }
                } else {
                    isAuthenticated = false;
                    Exception exc = task.getException();
                    logAuthError("Silent sign-in check failed", exc);
                    if (callback != null) {
                        callback.onFailure(exc != null ? exc : new Exception("Silent sign in failed"));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Silent sign in check error", e);
            isAuthenticated = false;
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    public void signInWithPlayGames(@NonNull Activity activity, @Nullable AuthCallback callback) {
        try {
            GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(activity);
            gamesSignInClient.signIn().addOnCompleteListener(activity, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    AuthenticationResult authResult = task.getResult();
                    isAuthenticated = authResult.isAuthenticated();
                    if (isAuthenticated) {
                        Log.d(TAG, "Play Games v2 Sign-In successful, fetching player info...");
                        fetchPlayerInfo(activity, callback);
                    } else {
                        Log.w(TAG, "Play Games v2 returned isAuthenticated=false");
                        if (callback != null) {
                            callback.onFailure(new Exception("Play Games sign in required"));
                        }
                    }
                } else {
                    isAuthenticated = false;
                    Exception exc = task.getException();
                    logAuthError("Play Games v2 Sign-In failed", exc);
                    if (callback != null) {
                        callback.onFailure(exc != null ? exc : new Exception("Play Games sign in failed"));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Play Games v2 sign-in exception", e);
            isAuthenticated = false;
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    public void signIn(@NonNull Activity activity, @Nullable AuthCallback callback) {
        if (activity instanceof com.redcodersgroup.bubbleshooter.MainActivity) {
            ((com.redcodersgroup.bubbleshooter.MainActivity) activity).startGoogleSignIn(callback);
            return;
        }

        try {
            Intent signInIntent = getGoogleSignInIntent(activity);
            activity.startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Google Sign In Intent, falling back to Play Games v2", e);
            signInWithPlayGames(activity, callback);
        }
    }

    private void logAuthError(String prefix, @Nullable Exception exc) {
        if (exc instanceof ApiException) {
            ApiException apiException = (ApiException) exc;
            int statusCode = apiException.getStatusCode();
            String reason;
            switch (statusCode) {
                case 10: // DEVELOPER_ERROR
                    reason = "DEVELOPER_ERROR (10): Package name (com.redcodersgroup.bubbleshooter) or SHA-1 fingerprint is not configured in Google Play Console / Cloud Console credentials.";
                    break;
                case 4: // SIGN_IN_REQUIRED
                    reason = "SIGN_IN_REQUIRED (4): User needs to sign into the Google Play Games app on this device.";
                    break;
                case 7: // NETWORK_ERROR
                    reason = "NETWORK_ERROR (7): Check device internet connection.";
                    break;
                case 8: // INTERNAL_ERROR
                    reason = "INTERNAL_ERROR (8): Google Play Services internal error.";
                    break;
                case 16: // CANCELED
                    reason = "CANCELED (16): Sign in was canceled by the user.";
                    break;
                case 12500: // SIGN_IN_FAILED
                    reason = "SIGN_IN_FAILED (12500): Check if Play Games / OAuth is in Draft and your Google account is added as a Tester in Play Console.";
                    break;
                case 26700: // GAME_NOT_FOUND
                    reason = "GAME_NOT_FOUND (26700): The project ID does not match any configured Play Games Services game.";
                    break;
                default:
                    reason = "Status code " + statusCode + " - " + apiException.getMessage();
                    break;
            }
            Log.e(TAG, prefix + " -> " + reason, apiException);
        } else if (exc != null) {
            Log.e(TAG, prefix + " -> " + exc.getMessage(), exc);
        } else {
            Log.e(TAG, prefix);
        }
    }

    private void fetchPlayerInfo(@NonNull Activity activity, @Nullable AuthCallback callback) {
        try {
            PlayersClient playersClient = PlayGames.getPlayersClient(activity);
            playersClient.getCurrentPlayer().addOnCompleteListener(activity, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Player player = task.getResult();
                    this.playerId = player.getPlayerId();
                    this.displayName = player.getDisplayName();
                    if (player.getIconImageUri() != null) {
                        this.iconImageUri = player.getIconImageUri().toString();
                    }
                    Log.d(TAG, "Authenticated Play Games Player: " + displayName + " (" + playerId + ")");
                    if (callback != null) {
                        callback.onSuccess(player);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(task.getException());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching player details", e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    @NonNull
    public static Player createPlayerProxy(@Nullable String id, @Nullable String displayName, @Nullable Uri iconUri) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getDisplayName".equals(name) || "getTitle".equals(name)) {
                        return displayName != null ? displayName : "Player";
                    }
                    if ("getPlayerId".equals(name)) {
                        return id != null ? id : "0";
                    }
                    if ("getIconImageUri".equals(name) || "getHiResImageUri".equals(name)) {
                        return iconUri;
                    }
                    if ("toString".equals(name)) {
                        return displayName + " (" + id + ")";
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    return null;
                }
        );
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getIconImageUri() {
        return iconImageUri;
    }
}
