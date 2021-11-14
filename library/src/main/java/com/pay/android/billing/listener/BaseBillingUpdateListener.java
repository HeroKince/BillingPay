package com.pay.android.billing.listener;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

/**
 * @author on 2019/08/14.
 */
public abstract class BaseBillingUpdateListener {

    /**
     * BillingClient 初始化完成
     */
    public abstract void onBillingClientSetupFinished();

    /**
     * BillingClient 断开连接
     */
    public abstract void onBillingServiceDisconnected();

    /**
     * 商品信息查询成功
     */
    public abstract void onQuerySkuDetailSuccess(@NonNull String skuType, List<SkuDetails> skuDetailsList);

    /**
     * 商品信息查询失败
     */
    public abstract void onQuerySkuDetailFailure(int errorCode, String message);

    /**
     * 消耗一次型商品完成，代表购买成功
     */
    public abstract void onConsumeFinished(String token, BillingResult result);

    /**
     * 消耗-订阅型型商品完成，代表订阅成功
     */
    public abstract void onAcknowledgeSubsFinish(BillingResult result);

    /**
     * 消耗-永久型商品完成，代表购买成功
     */
    public abstract void onAcknowledgeInappFinish(BillingResult result);

    /**
     * 查询已经购买的订阅
     */
    public abstract void onPurchaseSubsOwned(List<Purchase> purchases);

    /**
     * 查询已经购买的永久性商品
     */
    public abstract void onPurchaseInappOwned(List<Purchase> purchases);

    /**
     * 商品购买更新
     */
    public abstract void onPurchasesUpdated(List<Purchase> purchases);

    /**
     * 查询最近购买历史
     */
    public abstract void onPurchaseHistoryResponse(BillingResult billingResult, List<PurchaseHistoryRecord> list);

    /**
     * 内购取消
     */
    public abstract void onPurchasesCancel();

    /**
     * 内购失败
     */
    public abstract void onPurchasesFailure(int errorCode, String message);

}