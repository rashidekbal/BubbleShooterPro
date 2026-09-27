package com.redcodersgroup.bubbleshooter.store;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;
import com.redcodersgroup.bubbleshooter.analytics.AnalyticsManager;
import com.redcodersgroup.bubbleshooter.auth.CloudSaveManager;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IapBillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "IapBillingManager";

    public static final String SKU_DIAMONDS_150 = StoreManager.SKU_DIAMONDS_150;
    public static final String SKU_DIAMONDS_500 = StoreManager.SKU_DIAMONDS_500;
    public static final String SKU_DIAMONDS_1500 = StoreManager.SKU_DIAMONDS_1500;

    private static volatile IapBillingManager instance;
    private BillingClient billingClient;
    private final Context context;
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private final List<BillingListener> listeners = new ArrayList<>();
    private boolean isConnected = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BillingListener {
        void onProductDetailsUpdated();
        void onPurchaseSuccess(String productId, int diamondsAdded);
        void onPurchaseFailed(String productId, String errorMessage);
    }

    private IapBillingManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        initializeBillingClient();
    }

    public static IapBillingManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (IapBillingManager.class) {
                if (instance == null) {
                    instance = new IapBillingManager(context);
                }
            }
        }
        return instance;
    }

    public void addListener(BillingListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(BillingListener listener) {
        listeners.remove(listener);
    }

    private void initializeBillingClient() {
        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();

        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .build();
        startConnection();
    }

    public void startConnection() {
        if (billingClient == null) return;
        if (isConnected) {
            queryProductDetails();
            return;
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isConnected = true;
                    Log.d(TAG, "Google Play Billing setup successful");
                    queryProductDetails();
                    queryPastPurchasesToConsume();
                } else {
                    isConnected = false;
                    Log.w(TAG, "Billing setup failed: " + billingResult.getDebugMessage() + " code: " + billingResult.getResponseCode());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isConnected = false;
                Log.w(TAG, "Billing service disconnected. Will retry on next request.");
            }
        });
    }

    public void queryProductDetails() {
        if (billingClient == null || !isConnected) return;

        List<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_DIAMONDS_150)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_DIAMONDS_500)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_DIAMONDS_1500)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && result != null) {
                productDetailsMap.clear();
                List<ProductDetails> detailsList = result.getProductDetailsList();
                if (detailsList != null) {
                    for (ProductDetails details : detailsList) {
                        productDetailsMap.put(details.getProductId(), details);
                        Log.d(TAG, "Fetched Product: " + details.getProductId() + " -> " +
                                (details.getOneTimePurchaseOfferDetails() != null ? details.getOneTimePurchaseOfferDetails().getFormattedPrice() : "N/A"));
                    }
                }
                mainHandler.post(() -> {
                    for (BillingListener listener : listeners) {
                        listener.onProductDetailsUpdated();
                    }
                });
            } else {
                Log.w(TAG, "Failed to query ProductDetails: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Returns the localized price formatted according to the player's country/currency.
     * If fetched from Google Play, uses the exact store price (e.g. $0.99, ₹75, €0.89).
     * Otherwise falls back to localized default representation based on device locale.
     */
    @NonNull
    public String getFormattedPrice(String productId) {
        ProductDetails details = productDetailsMap.get(productId);
        if (details != null && details.getOneTimePurchaseOfferDetails() != null) {
            return details.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }

        // Fallback localized currency based on country
        String country = Locale.getDefault().getCountry().toUpperCase(Locale.ROOT);
        boolean isIndia = "IN".equals(country);

        if (SKU_DIAMONDS_150.equals(productId)) {
            return isIndia ? "₹75" : "$0.99";
        } else if (SKU_DIAMONDS_500.equals(productId)) {
            return isIndia ? "₹250" : "$2.99";
        } else if (SKU_DIAMONDS_1500.equals(productId)) {
            return isIndia ? "₹750" : "$6.99";
        }
        return "$0.99";
    }

    public void launchPurchaseFlow(@NonNull Activity activity, @NonNull String productId) {
        if (!isConnected || billingClient == null) {
            Log.w(TAG, "Billing not connected when launching purchase flow for " + productId);
            startConnection();
            notifyPurchaseFailed(productId, "Connecting to Google Play... Please try again in a moment.");
            return;
        }

        ProductDetails details = productDetailsMap.get(productId);
        if (details == null) {
            Log.w(TAG, "Product details not available for " + productId);
            queryProductDetails();
            notifyPurchaseFailed(productId, "Store catalog is loading from Google Play. Please check your internet connection and try again.");
            return;
        }

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow returned error: " + billingResult.getDebugMessage() + " (Code: " + billingResult.getResponseCode() + ")");
            notifyPurchaseFailed(productId, billingResult.getDebugMessage());
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled purchase flow");
            // User voluntarily cancelled - do not show error dialog and DO NOT grant diamonds
        } else {
            String errorMsg = billingResult.getDebugMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "Payment could not be completed (Code: " + responseCode + ").";
            }
            Log.w(TAG, "Purchase failed: " + errorMsg + " (Code: " + responseCode + ")");
            final String finalMsg = errorMsg;
            notifyPurchaseFailed("", finalMsg);
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            ConsumeParams consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase consumed successfully: " + purchase.getProducts());
                    for (String productId : purchase.getProducts()) {
                        mainHandler.post(() -> grantPurchasedDiamonds(null, productId));
                    }
                } else {
                    Log.e(TAG, "Failed to consume purchase: " + billingResult.getDebugMessage());
                    notifyPurchaseFailed("", "Failed to verify purchase with store: " + billingResult.getDebugMessage());
                }
            });
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending approval");
            notifyPurchaseFailed("", "Payment is pending approval. Diamonds will be credited once confirmed by your bank / Google Play.");
        }
    }

    private void queryPastPurchasesToConsume() {
        if (billingClient == null || !isConnected) return;
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase p : purchases) {
                            handlePurchase(p);
                        }
                    }
                }
        );
    }

    private void notifyPurchaseFailed(String productId, String errorMessage) {
        mainHandler.post(() -> {
            for (BillingListener listener : listeners) {
                listener.onPurchaseFailed(productId, errorMessage);
            }
        });
    }

    public void grantPurchasedDiamonds(@Nullable Activity activity, @NonNull String productId) {
        PreferencesManager prefs = new PreferencesManager(context);
        int diamondsToAdd = 0;

        if (SKU_DIAMONDS_150.equals(productId)) {
            diamondsToAdd = StoreManager.DIAMONDS_POUCH;
        } else if (SKU_DIAMONDS_500.equals(productId)) {
            diamondsToAdd = StoreManager.DIAMONDS_SACK;
        } else if (SKU_DIAMONDS_1500.equals(productId)) {
            diamondsToAdd = StoreManager.DIAMONDS_CHEST;
        }

        if (diamondsToAdd > 0) {
            prefs.addDiamonds(diamondsToAdd);
            if (activity != null) {
                CloudSaveManager.getInstance().saveToCloud(activity);
            }
            AnalyticsManager.getInstance(context).logEvent("iap_diamond_purchase", null);

            final int finalAdded = diamondsToAdd;
            mainHandler.post(() -> {
                for (BillingListener listener : listeners) {
                    listener.onPurchaseSuccess(productId, finalAdded);
                }
            });
        }
    }
}
