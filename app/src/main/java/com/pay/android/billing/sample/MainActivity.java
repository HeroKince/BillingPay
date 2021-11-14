package com.pay.android.billing.sample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.pay.android.billing.BillingManager;
import com.pay.android.billing.listener.BaseBillingUpdateListener;
import com.pay.android.billing.listener.SimpleBillingUpdateListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 商品、订阅ID
    public static final String[] PRODUCT_ID = {"com.color.messenger.sms.pid.adfree"};//一次性购买
    public static final String[] SUBSCRIBE_ID = {"com.color.messenger.sms.subscribe.year"};// 年订阅

    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) {
            billingManager.destroy();
        }
    }

    // Google原生支付回调
    BaseBillingUpdateListener billingUpdateListener = new SimpleBillingUpdateListener() {
        @Override
        public void onBillingClientSetupFinished() {
            if (billingManager != null) {
                billingManager.launchBillingFlow(MainActivity.this, "90days", BillingClient.SkuType.SUBS);
            }
        }

        @Override
        public void onQuerySkuDetailSuccess(@NonNull String skuType, List<SkuDetails> skuDetailsList) {

        }

        @Override
        public void onConsumeFinished(String token, BillingResult result) {

        }

        @Override
        public void onAcknowledgeSubsFinish(BillingResult result) {

        }

        @Override
        public void onAcknowledgeInappFinish(BillingResult result) {

        }

        @Override
        public void onPurchasesCancel() {
            Toast.makeText(MainActivity.this, "取消购买", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPurchasesFailure(int errorCode, String message) {
            Toast.makeText(MainActivity.this,
                    "购买失败[code：" + errorCode + ",message：" + message + "]", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPurchaseSubsOwned(List<Purchase> purchases) {

        }

        @Override
        public void onPurchaseInappOwned(List<Purchase> purchases) {

        }
    };

    /**
     * 原生支付
     */
    public void pay() {
        billingManager = BillingManager.getInstance();
        billingManager.setupGooglePayListener(this, MainActivity.class.getSimpleName(), billingUpdateListener);
        billingManager.setPermanentInappSkus(PRODUCT_ID);
        billingManager.setSubsSkus(SUBSCRIBE_ID);
        billingManager.startServiceConnection();
    }

}