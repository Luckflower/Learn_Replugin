package com.chanjalun.hwadhost.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.chanjalun.commonlibrary.INativeAdCallback;
import com.chanjalun.hwadhost.Constants;
import com.chanjalun.hwadhost.HwAdManager;
import com.chanjalun.hwadhost.R;
//import com.huawei.openalliance.ad.inter.data.INativeAd;
import com.qihoo360.replugin.RePlugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NativeAdActivity extends AppCompatActivity {

    public static final String TAG = "AdManager-log";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_ad);

        findViewById(R.id.load_ad).setOnClickListener(v ->
                HwAdManager.getInstance().requestNativeAd(RePlugin.fetchContext(Constants.PLUGIN_NAME)
                        , NativeAdActivity.this, new INativeAdCallback() {
            @Override
            public void onAdsLoaded(Object o) {
                Log.d(TAG, "host receiver native ad");
//                Map<String, List<INativeAd>> ads = (Map<String, List<INativeAd>>) o;
//                Collection<String> nativeAdIds = ads.keySet();
//                for (String adId : nativeAdIds) {
//                    // 取出广告位ID对应的广告内容对象列表
//                    List<INativeAd> nativeAdList = ads.get(adId);
//                    if (null != nativeAdList && !nativeAdList.isEmpty()) {
//                        // 取出广告内容对象
//
//                        ClassLoader classLoader = RePlugin.fetchClassLoader(Constants.PLUGIN_NAME);
//                        for (INativeAd nativeAd : nativeAdList) {
//                            // 处理广告内容
//                            Log.d(TAG, "host onAdsLoaded, addAd, ad:" + nativeAd.getTitle());
//
//                        }
//                    }
//                }
            }

            @Override
            public void onAdFailed(int i) {
                Log.d(TAG, "host receiver error:"+i);
            }
        }));
    }
}