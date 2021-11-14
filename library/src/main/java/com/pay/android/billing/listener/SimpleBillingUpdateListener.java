package com.pay.android.billing.listener;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;

import java.util.List;

public abstract class SimpleBillingUpdateListener extends BaseBillingUpdateListener {

    @Override
    public void onBillingServiceDisconnected() {

    }

    @Override
    public void onQuerySkuDetailFailure(int errorCode, String message) {

    }

    @Override
    public void onPurchaseHistoryResponse(BillingResult billingResult, List<PurchaseHistoryRecord> list) {

    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {

    }

}