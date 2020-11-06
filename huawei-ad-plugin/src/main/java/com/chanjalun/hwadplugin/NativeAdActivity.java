package com.chanjalun.hwadplugin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.openalliance.ad.constant.DeviceType;
import com.huawei.openalliance.ad.inter.INativeAdLoader;
import com.huawei.openalliance.ad.inter.NativeAdLoader;
import com.huawei.openalliance.ad.inter.data.INativeAd;
import com.huawei.openalliance.ad.inter.listeners.NativeAdListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NativeAdActivity extends AppCompatActivity {

    public static final String TAG = "NativeAdLog";

    private INativeAdLoader adLoader;
    private boolean isTest = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 构造获取广告请求参数
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_ad);
        findViewById(R.id.btn_native_ad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNativeAd();
            }
        });
    }

    private void loadNativeAd() {
        // 实例化NativeAdLoader，传入原生广告位ID广告位ID列表。
        adLoader = new NativeAdLoader(this, new String[]{"f18j40bllx", "a7k1ds3s6o", "b65czjivt9","u7coq7f6e1"});
        adLoader.enableDirectReturnVideoAd(true); // 【可选】需要快速返回视频广告时调用，如果调用了此接口，必须给NativeVideoView加载视频预览图，详见“7.2.2.2 展示视频文类广告”章节。

        // 设置监听器
        adLoader.setListener(new NativeAdListener() {
            @Override
            public void onAdsLoaded(Map<String, List<INativeAd>> ads) {
                Log.d(TAG, "onAdsLoaded, ad.size:" + ads.size());
                // 取出所有的广告内容对象，ads的key为广告位ID，value为广告位ID对应的广告内容对象列表
                Collection<String> nativeAdIds = ads.keySet();
                for (String adId : nativeAdIds) {
                    // 取出广告位ID对应的广告内容对象列表
                    List<INativeAd> nativeAdList = ads.get(adId);
                    if (null != nativeAdList && !nativeAdList.isEmpty()) {
                        // 取出广告内容对象
                        for (INativeAd nativeAd : nativeAdList) {
                            // 处理广告内容
                            Log.d(TAG, "onAdsLoaded, addAd, ad:" + nativeAd.getTitle());

                        }
                    }
                }
            }

            @Override
            public void onAdFailed(int errorCode) {
                Log.e(TAG, "fail to load ad, errorCode is:" + errorCode);
                // 获取广告失败
            }
        });
        adLoader.loadAds(DeviceType.PHONE, isTest);
    }



    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        // 从其他页面回到开屏页面时调用，进入应用
        super.onRestart();
    }

}