package com.chanjalun.hwadplugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.openalliance.ad.beans.parameter.AdSlotParam;
import com.huawei.openalliance.ad.inter.HiAdSplash;
import com.huawei.openalliance.ad.inter.listeners.AdListener;
import com.huawei.openalliance.ad.views.PPSSplashView;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    public static final String TAG = "pluginadsplash";

    /**
     * 终端设备类型：4：手机，5：平板
     */
    private static final int DEVICE_TYPE = 4;

    /**
     * 异常保护使用：广告展示超时时间：单位毫秒
     */
    private static final int AD_TIMEOUT = 10000;

    /**
     * 异常保护使用：广告超时消息标记
     */
    private static final int MSG_AD_TIMEOUT = 1001;

    /**
     * 返回键标志位 按返回键退出时应用不被重新拉起，但是在展示广告时下拉通知栏仍然需要正常跳转
     */
    private boolean hasPaused = false;

    /**
     * 超时消息回调handler
     */
    private Handler timeoutHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (SplashActivity.this.hasWindowFocus()) {
                jump();
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 构造获取广告请求参数
        super.onCreate(savedInstanceState);
        setContentView(R.layout.huawei_ad_screen_layout);
        AdSlotParam.Builder slotParamBuilder = new AdSlotParam.Builder();
        List<String> adIds = new ArrayList<String>(1);
        adIds.add(AdManager.SPLASH_AD_ID);

        // 设置屏幕方向
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        slotParamBuilder
                .setAdIds(adIds) // 设置广告位ID
                .setDeviceType(DEVICE_TYPE) // 设置终端类型，取值：4：手机，5：平板
                .setOrientation(orientation) // 设置屏幕方向
                .setTest(true); // 是否请求测试广告，上线版本必须是false

        // 设置logan展示时间，云端可配置。
        HiAdSplash.getInstance(this).setSloganDefTime(2000);

        // 判断是否存在广告和Slogan
        if (!HiAdSplash.getInstance(this).isAvailable(slotParamBuilder.build())) {
            jump();
            return;
        }

        // 获取PPSSplashView控件
        PPSSplashView splashAdView = (PPSSplashView) findViewById(R.id.pps_splash_view);

        splashAdView.setAdSlotParam(slotParamBuilder.build()); // 设置广告位参数
        splashAdView.setSloganResId(R.mipmap.ic_launcher); // 设置默认Slogan图片资源ID
        splashAdView.setLogo(findViewById(R.id.logo)); // 设置默认logo图片资源

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
                jump();
            }
        });

        // 加载广告
        splashAdView.loadAd();

        // 启动广告超时保护定时器，注意，在onStop时要移除此消息，否则会影响正常广告展示。
        timeoutHandler.removeMessages(MSG_AD_TIMEOUT);
        timeoutHandler.sendEmptyMessageDelayed(MSG_AD_TIMEOUT, AD_TIMEOUT);
    }

    public void jump() {
        // 进入应用主界面
        if (!hasPaused) {
            hasPaused = true;
//            startActivity(new Intent(SplashActivity.this, MainActivity.class));
//            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isMultiWin()) {
            jump();
            return;
        }
    }

    @Override
    protected void onStop() {
        // 移除超时消息
        timeoutHandler.removeMessages(MSG_AD_TIMEOUT);
        hasPaused = true;
        super.onStop();
    }

    @Override
    protected void onRestart() {
        // 从其他页面回到开屏页面时调用，进入应用
        super.onRestart();
        hasPaused = false;
        jump();
    }

    @TargetApi(24)
    private boolean isMultiWin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
            return true;
        }
        return false;
    }
}