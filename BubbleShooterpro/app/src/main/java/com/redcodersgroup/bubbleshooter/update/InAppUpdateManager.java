package com.redcodersgroup.bubbleshooter.update;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;

public class InAppUpdateManager {

    private static final String TAG = "InAppUpdateManager";
    public static final int FLEXIBLE_UPDATE_REQUEST_CODE = 9001;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;
    private final InstallStateUpdatedListener installStateUpdatedListener;
    private boolean isListenerRegistered = false;

    public InAppUpdateManager(@NonNull Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
        this.installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Log.d(TAG, "Flexible update downloaded. Prompting user to complete installation.");
                notifyUpdateDownloaded();
            } else if (state.installStatus() == InstallStatus.DOWNLOADING) {
                long bytesDownloaded = state.bytesDownloaded();
                long totalBytesToDownload = state.totalBytesToDownload();
                Log.d(TAG, "Update downloading: " + bytesDownloaded + " / " + totalBytesToDownload);
            }
        };
    }

    public void checkForUpdate() {
        registerListener();
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            int availability = appUpdateInfo.updateAvailability();
            Log.d(TAG, "checkForUpdate result: availability=" + availability +
                    ", flexibleAllowed=" + appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE));

            if (availability == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                AnalyticsManager.getInstance(activity).logEvent("update_prompted", null);
                startFlexibleUpdate(appUpdateInfo);
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                notifyUpdateDownloaded();
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Failed to check for app update", e);
        });
    }

    public void onResume() {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                notifyUpdateDownloaded();
            }
        });
    }

    private void startFlexibleUpdate(@NonNull AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    FLEXIBLE_UPDATE_REQUEST_CODE
            );
        } catch (Exception e) {
            Log.e(TAG, "Error starting flexible update flow", e);
        }
    }

    private void notifyUpdateDownloaded() {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        Snackbar snackbar = Snackbar.make(
                rootView,
                "🎉 An update has just been downloaded!",
                Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction("RESTART", v -> {
            AnalyticsManager.getInstance(activity).logEvent("update_completed", null);
            appUpdateManager.completeUpdate();
        });
        snackbar.setActionTextColor(Color.parseColor("#38BDF8"));
        snackbar.show();
    }

    private void registerListener() {
        if (!isListenerRegistered) {
            appUpdateManager.registerListener(installStateUpdatedListener);
            isListenerRegistered = true;
        }
    }

    public void onDestroy() {
        if (isListenerRegistered) {
            try {
                appUpdateManager.unregisterListener(installStateUpdatedListener);
            } catch (Exception ignored) {}
            isListenerRegistered = false;
        }
    }
}
