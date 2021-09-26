package com.pay.android.billing.sample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.pay.android.billing.BillingManager;
import com.pay.android.billing.listener.BaseBillingUpdateListener;
import com.pay.android.billing.listener.SimpleBillingUpdateListener;
import com.pay.android.billing.model.PurchaseInfo;
import com.pay.android.billing.receiver.BillingPurchasesReceiver;
import com.pay.android.billing.utils.LogUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

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
                billingManager.launchBillingFlow("90days", BillingClient.SkuType.SUBS);
            }
        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchases) {
            Toast.makeText(MainActivity.this, "购买成功", Toast.LENGTH_SHORT).show();
            LogUtils.e("购买成功：" + purchases.get(0).toString());
        }

        @Override
        public void onPurchasesCancel() {
            Toast.makeText(MainActivity.this, "取消购买", Toast.LENGTH_SHORT).show();
            LogUtils.e("取消购买");
        }

        @Override
        public void onPurchasesFailure(int errorCode, String message) {
            Toast.makeText(MainActivity.this,
                    "购买失败[code：" + errorCode + ",message：" + message + "]", Toast.LENGTH_SHORT).show();
            LogUtils.e("购买失败[code：" + errorCode + ",message：" + message + "]");
        }
    };

    // H5方式支付回调
    BillingPurchasesReceiver billingPurchasesReceiver = new BillingPurchasesReceiver() {
        @Override
        public void onPurchasesUpdated(PurchaseInfo purchaseInfo) {
            Toast.makeText(MainActivity.this, "购买成功", Toast.LENGTH_SHORT).show();
            LogUtils.e("购买成功：" + purchaseInfo.toString());
        }

        @Override
        public void onPurchasesCancel() {
            Toast.makeText(MainActivity.this, "取消购买", Toast.LENGTH_SHORT).show();
            LogUtils.e("取消购买");
        }

        @Override
        public void onPurchasesFailure(int errorCode, String message) {
            Toast.makeText(MainActivity.this,
                    "购买失败[code：" + errorCode + ",message：" + message + "]", Toast.LENGTH_SHORT).show();
            LogUtils.e("购买失败[code：" + errorCode + ",message：" + message + "]");
        }
    };

    /**
     * 原生支付
     */
    public void pay1() {
        billingManager = new BillingManager(this, billingUpdateListener);
        billingManager.startServiceConnection(null);
    }

    /**
     * H5支付
     */
    public void pay2() {
        BillingManager billingManager = new BillingManager(this, billingPurchasesReceiver);
        billingManager.quicknessPurchase("");
    }

}