package com.chanjalun.hwadplugin;

import android.app.Application;
import android.util.Log;

import com.huawei.openalliance.ad.inter.HiAd;

public class PluginApp extends Application {

    public static final String TAG = "PluginApp-log";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "plugin application onCreate call");
        HiAd.getInstance(this).initLog(true, Log.INFO);
        HiAd.getInstance(this).enableUserInfo(true);
    }
}
