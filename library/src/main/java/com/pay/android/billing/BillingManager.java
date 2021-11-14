package com.pay.android.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.pay.android.billing.listener.BaseBillingUpdateListener;
import com.pay.android.billing.utils.LogUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用Google Play结算版本2.0及以上，必须在3天内确认所有购买交易。
 * 如果没有正确确认，将导致系统对相应的购买交易按退款处理。
 * 确认购买交易有3种方式：
 * 1. 消耗型商品，客户端使用API consumeAsync()，比如购买钻石金币等
 * 2. 对于非消耗型商品，客户端使用API acknowledgePurchase()，包括订阅型商品、永久性商品VIP
 * 3. 还可以使用服务器API新增的acknowledge()方法进行消耗确认
 * <p>
 */
public class BillingManager implements PurchasesUpdatedListener {

    public static final String TYPE_INAPP = BillingClient.SkuType.INAPP;//内购
    public static final String TYPE_SUBS = BillingClient.SkuType.SUBS;//订阅

    private BillingClient mBillingClient;
    private boolean mIsServiceConnected;
    private WeakReference<Context> weakReference;
    private final Map<String, BaseBillingUpdateListener> onBillingListenerMap = new HashMap<>();

    private List<String> onTimeInAppSKUS = new ArrayList<>();//一次性内购ID
    private List<String> permanentInAppSKUS = new ArrayList<>();//永久性内购ID
    private List<String> subsSKUS = new ArrayList<>();//订阅ID

    private boolean isDebug = false;

    private static volatile BillingManager INSTANCE;

    public static BillingManager getInstance() {
        if (INSTANCE == null) {
            synchronized (BillingManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingManager();
                }
            }
        }
        return INSTANCE;
    }

    private BillingManager() {
    }

    /**
     * 设置一次性内购skus
     *
     * @param inAppSKUS 内购id
     */
    public void setOneTimeInappSkus(@Nullable String[] inAppSKUS) {
        if (inAppSKUS != null) {
            this.onTimeInAppSKUS = Arrays.asList(inAppSKUS);
        }
    }

    /**
     * 设置永久性内购skus
     *
     * @param inAppSKUS 内购id
     */
    public void setPermanentInappSkus(@Nullable String[] inAppSKUS) {
        if (inAppSKUS != null) {
            this.permanentInAppSKUS = Arrays.asList(inAppSKUS);
        }
    }

    /**
     * 设置订阅skus
     *
     * @param subsSKUS 订阅id
     */
    public void setSubsSkus(@Nullable String[] subsSKUS) {
        if (subsSKUS != null) {
            this.subsSKUS = Arrays.asList(subsSKUS);
        }
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    /**
     * 设置监听回调
     *
     * @param activity
     * @param tag
     * @param billingUpdatesListener
     */
    public void setupGooglePayListener(Context activity, String tag, BaseBillingUpdateListener billingUpdatesListener) {
        this.weakReference = new WeakReference<>(activity);
        onBillingListenerMap.put(tag, billingUpdatesListener);
    }

    public void removeGooglePayListener(String tag) {
        onBillingListenerMap.remove(tag);
    }

    /**
     * 连接谷歌商店(异步)
     */
    public void startServiceConnection() {
        mBillingClient = BillingClient
                .newBuilder(weakReference.get().getApplicationContext())
                .enablePendingPurchases()
                .setListener(this)
                .build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    confirmHistoryPurchase(TYPE_INAPP);
                    confirmHistoryPurchase(TYPE_SUBS);

                    for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                        BaseBillingUpdateListener listener = entry.getValue();
                        listener.onBillingClientSetupFinished();
                    }
                    mIsServiceConnected = true;
                    if (isDebug) LogUtils.e("Google billing service connect success!");
                } else {
                    mIsServiceConnected = false;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                mIsServiceConnected = false;
                for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                    BaseBillingUpdateListener listener = entry.getValue();
                    listener.onBillingServiceDisconnected();
                }
                if (isDebug) LogUtils.e("Google billing service connect fail!");
            }
        });
    }

    public boolean isServiceConnected() {
        return mIsServiceConnected;
    }

    /**
     * 异步查询商品信息
     *
     * @param skuId   商品唯一ID
     * @param skuType 商品类型 详见{@link BillingClient.SkuType}
     */
    public void querySkuDetailAsync(final String skuId, final String skuType) {
        List<String> skuList = new ArrayList<>();
        skuList.add(skuId);
        querySkuDetailAsync(skuList, skuType);
    }

    /**
     * 异步查询商品信息
     *
     * @param skuList 商品ID List
     * @param skuType 商品类型 详见{@link BillingClient.SkuType}
     */
    public void querySkuDetailAsync(final List<String> skuList, final String skuType) {
        if (isDebug) LogUtils.e("querySkuDetailAsyn >>> [" + skuList + ",type:" + skuType + "]");
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(skuType);
                mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        // Process the result.
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                            for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                                BaseBillingUpdateListener listener = entry.getValue();
                                listener.onQuerySkuDetailSuccess(skuType, skuDetailsList);
                            }
                            if (!skuDetailsList.isEmpty()) {
                                for (SkuDetails skuDetails : skuDetailsList) {
                                    if (isDebug)
                                        LogUtils.e("querySkuDetailAsyn success >>> [skuDetails:" + skuDetails.toString() + "]");
                                }
                            }
                        } else {
                            for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                                BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                                billingUpdatesListener.onQuerySkuDetailFailure(billingResult.getResponseCode(), billingResult.getDebugMessage());
                            }
                        }
                    }
                });
            }
        });
    }


    /**
     * 异步查询购买历史商品详情
     *
     * @param skuType 商品类型 {@link BillingClient.SkuType}
     */
    public void queryPurchaseHistoryAsync(final @BillingClient.SkuType String skuType) {
        if (isDebug) LogUtils.e("queryPurchaseHistoryAsync >>> [" + skuType + "]");
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                mBillingClient.queryPurchaseHistoryAsync(skuType, new PurchaseHistoryResponseListener() {
                    @Override
                    public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, List<PurchaseHistoryRecord> list) {
                        for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                            BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                            billingUpdatesListener.onPurchaseHistoryResponse(billingResult, list);
                        }
                    }
                });
            }
        });
    }

    /**
     * 确认历史购买，最好在每次启动应用前执行一次，防止有未正常确认的商品而导致三天后退款
     *
     * @param skuType 商品类型 {@link BillingClient.SkuType}
     */
    public void confirmHistoryPurchase(final String skuType) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                /* 同步查询历史购买 */
                Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(skuType);
                if (purchasesResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<Purchase> purchasesList = purchasesResult.getPurchasesList();
                    if (purchasesList != null && !purchasesList.isEmpty()) {
                        for (Purchase purchase : purchasesList) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                if (TYPE_SUBS.equals(skuType)) {
                                    if (!purchase.isAcknowledged()) {
                                        acknowledgePurchase(purchase.getPurchaseToken(), skuType);
                                    }
                                    if (isDebug)
                                        LogUtils.e("acknowledgePurchase success >>> [orderId：" + purchase.getOrderId() + "]");
                                } else if (TYPE_INAPP.equals(skuType)) {
                                    if (isPermanentProduct(purchase.getSku())) {
                                        if (!purchase.isAcknowledged()) {
                                            acknowledgePurchase(purchase.getPurchaseToken(), skuType);
                                        }
                                    } else {
                                        consumeAsync(purchase.getPurchaseToken());
                                    }
                                    if (isDebug)
                                        LogUtils.e("consumeAsync success >>> [orderId：" + purchase.getOrderId() + "]");
                                }
                            }
                        }
                    }

                    if (TYPE_SUBS.equals(skuType)) {
                        for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                            BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                            if (billingUpdatesListener != null) {
                                billingUpdatesListener.onPurchaseSubsOwned(purchasesList);
                            }
                        }
                    } else if (TYPE_INAPP.equals(skuType)) {
                        for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                            BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                            if (billingUpdatesListener != null) {
                                billingUpdatesListener.onPurchaseInappOwned(purchasesList);
                            }
                        }
                    }
                }
            }
        };
        executeServiceRequest(runnable);
    }

    /**
     * <p>
     * 启动内购流程
     * 商品查询成功后直接进入购买流程，如果查询商品失败则直接执行{@link BaseBillingUpdateListener#onPurchasesFailure(int, String)}
     * </p>
     *
     * @param skuId   商品ID
     * @param skuType 商品类型
     */
    public void launchBillingFlow(Activity activity, final String skuId, final String skuType) {
        if (isDebug)
            LogUtils.e("launchBillingFlow > querySkuDetailsAsync >>> [" + skuId + ",type:" + skuType + "]");
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                List<String> skuList = new ArrayList<>();
                skuList.add(skuId);
                final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(skuType);
                mBillingClient.querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                                // Process the result.
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                                    if (!skuDetailsList.isEmpty()) {
                                        for (SkuDetails skuDetails : skuDetailsList) {
                                            // 发起内购
                                            launchBillingFlow(activity, skuDetails);
                                            if (isDebug)
                                                LogUtils.e("querySkuDetailsAsync success >>> [skuDetails:" + skuDetails.toString() + "]");
                                        }
                                    }
                                } else {
                                    for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                                        BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                                        billingUpdatesListener.onPurchasesFailure(billingResult.getResponseCode(), billingResult.getDebugMessage());
                                    }
                                }
                            }
                        });
            }
        });
    }

    /**
     * 发起内购
     *
     * @param skuDetails 商品详情
     */
    public void launchBillingFlow(Activity activity, final SkuDetails skuDetails) {
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build();
                int responseCode = mBillingClient.launchBillingFlow(activity, flowParams).getResponseCode();
                if (isDebug)
                    LogUtils.e("launchBillingFlow >>> [responseCode:" + responseCode + "]");
            }
        };
        executeServiceRequest(runnable);
    }

    /**
     * 对消耗型商品进行确认购买处理
     */
    public void consumeAsync(final String purchaseToken) {
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                ConsumeParams consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchaseToken)
                        .build();
                mBillingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken) {
                        for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                            BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                            billingUpdatesListener.onConsumeFinished(purchaseToken, billingResult);
                        }
                    }
                });
            }
        });
    }

    /**
     * 对非消耗型商品进行确认购买处理
     */
    public void acknowledgePurchase(final String purchaseToken, final String skuType) {
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchaseToken)
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                            BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                            if (TYPE_SUBS.equals(skuType)) {
                                billingUpdatesListener.onAcknowledgeSubsFinish(billingResult);
                            } else if (TYPE_INAPP.equals(skuType)) {
                                billingUpdatesListener.onAcknowledgeInappFinish(billingResult);
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * 连接断开重试策略
     */
    private void executeServiceRequest(Runnable runnable) {
        if (mBillingClient != null) {
            runnable.run();
        } else {
            startServiceConnection();
        }
    }

    /**
     * 购买交易更新
     */
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                    BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        //是当前页面，并且商品状态为支付成功，才会进行消耗与确认的操作
                        String skuType = getSkuType(purchase.getSku());
                        if (TYPE_INAPP.equals(skuType)) {
                            if (isPermanentProduct(purchase.getSku())) {
                                if (!purchase.isAcknowledged()) {
                                    acknowledgePurchase(purchase.getPurchaseToken(), skuType);
                                }
                            } else {
                                consumeAsync(purchase.getPurchaseToken());
                            }
                        } else if (TYPE_SUBS.equals(skuType)) {
                            //进行确认购买
                            if (!purchase.isAcknowledged()) {
                                acknowledgePurchase(purchase.getPurchaseToken(), skuType);
                            }
                        }
                    } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                        if (isDebug) LogUtils.e("待处理的订单:" + purchase.getSku());
                    }
                    billingUpdatesListener.onPurchasesUpdated(purchases);
                }
            }
            if (isDebug) LogUtils.e("Payment success >>> [code："
                    + billingResult.getResponseCode() + ",message：" + billingResult.getDebugMessage() + "]");
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                billingUpdatesListener.onPurchasesCancel();
            }
            if (isDebug) LogUtils.e("Payment cancel >>> [code："
                    + billingResult.getResponseCode() + ",message：" + billingResult.getDebugMessage() + "]");
        } else {
            // Handle any other error codes.
            for (Map.Entry<String, BaseBillingUpdateListener> entry : onBillingListenerMap.entrySet()) {
                BaseBillingUpdateListener billingUpdatesListener = entry.getValue();
                billingUpdatesListener.onPurchasesFailure(billingResult.getResponseCode(), billingResult.getDebugMessage());
            }
            if (isDebug)
                LogUtils.e("Payment failure >>> [code：" + billingResult.getResponseCode() + ",message：" + billingResult.getDebugMessage() + "]");
        }
    }

    /**
     * 通过sku获取商品类型(订阅获取内购)
     *
     * @param sku sku
     * @return inapp内购，subs订阅
     */
    public String getSkuType(String sku) {
        if (onTimeInAppSKUS.contains(sku) || permanentInAppSKUS.contains(sku)) {
            return TYPE_INAPP;
        } else if (subsSKUS.contains(sku)) {
            return TYPE_SUBS;
        }
        return null;
    }

    private boolean isPermanentProduct(String sku) {
        return permanentInAppSKUS.contains(sku);
    }

    /**
     * google内购服务是否已经准备好
     *
     * @return boolean
     */
    public boolean isReady() {
        return mBillingClient != null && mBillingClient.isReady();
    }

    /**
     * 断开连接google服务
     * 注意！！！一般情况不建议调用该方法，让google保留连接是最好的选择。
     */
    public void endConnection() {
        //注意！！！一般情况不建议调用该方法，让google保留连接是最好的选择。
        if (mBillingClient != null) {
            if (mBillingClient.isReady()) {
                mBillingClient.endConnection();
                mBillingClient = null;
            }
        }
    }

    /**
     * 回收资源,退出应用调用
     */
    public void destroy() {
        if (isDebug) LogUtils.d("Destroying the manager.");
        onBillingListenerMap.clear();
        onTimeInAppSKUS.clear();
        permanentInAppSKUS.clear();
        subsSKUS.clear();
    }

}