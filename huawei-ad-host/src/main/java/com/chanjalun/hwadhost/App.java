package com.chanjalun.hwadhost;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.RePluginConfig;

public class App extends Application {

    public static final String TAG = "hostApp-log";

    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        RePlugin.App.onCreate();
        Log.d(TAG, "host application onCreate call");
        HwAdManager.getInstance().init();
    }

    public App() {
        this.sInstance = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        RePluginConfig rePluginConfig = new RePluginConfig();
        rePluginConfig.setUseHostClassIfNotFound(true);
        rePluginConfig.setPrintDetailLog(true);
        RePlugin.App.attachBaseContext(this, rePluginConfig);
        Log.d(TAG, "host application attachBaseContext call");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        RePlugin.App.onLowMemory();
    }

    public static App getInstance() {
        return sInstance;
    }
}
