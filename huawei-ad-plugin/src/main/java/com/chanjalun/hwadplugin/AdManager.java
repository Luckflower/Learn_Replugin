package com.chanjalun.hwadplugin;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.chanjalun.commonlibrary.IAdManager;
import com.chanjalun.hwadplugin.view.SplashAdView;
import com.huawei.openalliance.ad.beans.parameter.AdSlotParam;
import com.huawei.openalliance.ad.inter.HiAdSplash;
import com.huawei.openalliance.ad.inter.listeners.AdListener;
import com.huawei.openalliance.ad.views.PPSSplashView;

import java.util.ArrayList;
import java.util.List;

public class AdManager implements IAdManager {

    public static final String TAG = "AdManager-log";


    public static final String SPLASH_AD_ID = "5cd1c663263511e6af7500163e291137";

    private static AdManager sInstance;

    private AdManager(){}

    public static AdManager getInstance()  {
        Log.d(TAG, "plugin constructor call");
        if (sInstance == null) {
            synchronized (AdManager.class) {
                if (sInstance == null) {
                    sInstance = new AdManager();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void init() {

    }

    @Override
    public void requestSplashAd(Context pluginContext, Context hostContext, ViewGroup viewContainer){
        // 构造获取广告请求参数
        AdSlotParam.Builder slotParamBuilder = new AdSlotParam.Builder();
        List<String> adIds = new ArrayList<String>(1);
        adIds.add(AdManager.SPLASH_AD_ID);

        // 设置屏幕方向
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        slotParamBuilder
                .setAdIds(adIds) // 设置广告位ID
                .setDeviceType(4) // 设置终端类型，取值：4：手机，5：平板
                .setOrientation(orientation) // 设置屏幕方向
                .setTest(false); // 是否请求测试广告，上线版本必须是false

        // 设置logan展示时间，云端可配置。
        HiAdSplash.getInstance(pluginContext).setSloganDefTime(2000);


        // 判断是否存在广告和Slogan
        if (!HiAdSplash.getInstance(pluginContext).isAvailable(slotParamBuilder.build())) {
            Log.d(TAG, "isAvailable: false");
            return;
        }

        // 获取PPSSplashView控件
//        PPSSplashView splashAdView = new PPSSplashView(pluginContext);
        SplashAdView splashAdViewContainer = new SplashAdView(pluginContext);
        PPSSplashView splashAdView = (PPSSplashView) splashAdViewContainer.getPPSView();
        splashAdView.setAdSlotParam(slotParamBuilder.build()); // 设置广告位参数
        splashAdView.setSloganResId(R.mipmap.ic_launcher); // 设置默认Slogan图片资源ID
        splashAdView.setLogo(splashAdViewContainer.getLogoView()); // 设置默认logo图片资源

        splashAdView.setLogoResId(R.mipmap.ic_launcher); // 全屏开屏广告，显示在左上角的应用图标
        splashAdView.setMediaNameResId(R.string.app_name); // 全屏开屏广告，显示在左上角的应用名称

        // 设置广告记载事件监听器
        splashAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                // 广告加载失败
                Log.d(TAG, "onAdFailedToLoad: " + errorCode);
            }

            @Override
            public void onAdLoaded() {
                // 广告加载成功
                Log.d(TAG, "onAdLoaded");
            }

            @Override
            public void onAdDismissed() {
                // 广告消失，进入应用主界面
                Log.d(TAG, "onAdDismissed");
            }
        });

        // 加载广告
        splashAdView.loadAd();
        viewContainer.removeAllViews();
        viewContainer.addView(splashAdViewContainer);
    }

}
