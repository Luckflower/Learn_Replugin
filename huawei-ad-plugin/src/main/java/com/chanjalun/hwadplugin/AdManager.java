package com.chanjalun.hwadplugin;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chanjalun.commonlibrary.IAdManager;
import com.chanjalun.commonlibrary.INativeAdCallback;
import com.chanjalun.hwadplugin.view.SplashAdView;
import com.huawei.openalliance.ad.beans.parameter.AdSlotParam;
import com.huawei.openalliance.ad.constant.DeviceType;
import com.huawei.openalliance.ad.inter.HiAdSplash;
import com.huawei.openalliance.ad.inter.INativeAdLoader;
import com.huawei.openalliance.ad.inter.NativeAdLoader;
import com.huawei.openalliance.ad.inter.data.INativeAd;
import com.huawei.openalliance.ad.inter.listeners.AdListener;
import com.huawei.openalliance.ad.inter.listeners.NativeAdListener;
import com.huawei.openalliance.ad.views.PPSSplashView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdManager implements IAdManager {

    public static final String TAG = "AdManager-log";


    public static final String SPLASH_AD_ID = "5cd1c663263511e6af7500163e291137";

    private static AdManager sInstance;

    private INativeAdLoader adLoader;
    private boolean isTest = false;

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

    /**
     * 开屏广告，  PPSSplashView 内部请求会判断activity状态，
     * @param pluginContext
     * @param hostContext
     * @param viewContainer
     */
    @Override
    public void requestSplashAd(Context pluginContext, Context hostContext, ViewGroup viewContainer){

        Log.d(TAG, "plugin requestSplashAd");
        Log.d(TAG, "plugin requestSplashAd hostContext = "+hostContext.toString());
        Log.d(TAG, "plugin requestSplashAd pluginContext = "+pluginContext.toString());
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
                .setTest(true); // 是否请求测试广告，上线版本必须是false

        // 设置logan展示时间，云端可配置。
        HiAdSplash.getInstance(hostContext).setSloganDefTime(2000);


        // 判断是否存在广告和Slogan
        if (!HiAdSplash.getInstance(hostContext).isAvailable(slotParamBuilder.build())) {
            Log.d(TAG, "isAvailable: false");
            return;
        }

        //方式1：
//        View root = LayoutInflater.from(pluginContext).inflate(R.layout.huawei_ad_screen_layout, viewContainer, false);
//        PPSSplashView splashAdView = (PPSSplashView) root.findViewById(R.id.pps_splash_view);
//        splashAdView.setLogo(root.findViewById(R.id.logo)); // 设置默认logo图片资源

        //方式二：
        // 获取PPSSplashView控件

        float hiadwifi = hostContext.getResources().getDimensionPixelSize(R.dimen.hiad_wifi_preload_label_v_m);
        Log.i(TAG, "plugin requestSplashAd plugin inject resource hiadwifi "+hiadwifi);

        float hiad24 = hostContext.getResources().getDimensionPixelSize(R.dimen.hiad_24_dp);
        Log.i(TAG, "plugin requestSplashAd plugin inject resource hiad24 "+hiad24);

        PPSSplashView splashAdView = new PPSSplashView(hostContext);
//        SplashAdView splashAdViewContainer = new SplashAdView(hostContext);
//        PPSSplashView splashAdView = (PPSSplashView) splashAdViewContainer.getPPSView();
        splashAdView.setAdSlotParam(slotParamBuilder.build()); // 设置广告位参数
        splashAdView.setSloganResId(R.mipmap.ic_launcher); // 设置默认Slogan图片资源ID
//        splashAdView.setLogo(splashAdViewContainer.getLogoView()); // 设置默认logo图片资源

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
                viewContainer.removeAllViews();
                viewContainer.addView(splashAdView);
            }

            @Override
            public void onAdDismissed() {
                // 广告消失，进入应用主界面
                Log.d(TAG, "onAdDismissed");
            }
        });

        // 加载广告
        splashAdView.loadAd();
    }

    @Override
    public void requestNativeAd(Context context, Context context1, INativeAdCallback iNativeAdCallback) {
        // 实例化NativeAdLoader，传入原生广告位ID广告位ID列表。
        adLoader = new NativeAdLoader(context, new String[]{"f18j40bllx", "a7k1ds3s6o", "b65czjivt9","u7coq7f6e1"});
        adLoader.enableDirectReturnVideoAd(true); // 【可选】需要快速返回视频广告时调用，如果调用了此接口，必须给NativeVideoView加载视频预览图，详见“7.2.2.2 展示视频文类广告”章节。

        // 设置监听器
        adLoader.setListener(new NativeAdListener() {
            @Override
            public void onAdsLoaded(Map<String, List<INativeAd>> ads) {
                Log.d(TAG, "onAdsLoaded, ad.size:" + ads.size());
                iNativeAdCallback.onAdsLoaded(ads);
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
                iNativeAdCallback.onAdFailed(errorCode);
            }
        });
        adLoader.loadAds(DeviceType.PHONE, isTest);
    }
}
