package com.chanjalun.hwadhost;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.chanjalun.commonlibrary.IAdManager;
import com.chanjalun.commonlibrary.INativeAdCallback;
import com.qihoo360.replugin.RePlugin;

import java.lang.reflect.Method;

public class HwAdManager implements IAdManager {

    public static final String TAG = "HwAdManager-log";
//    public static final String TAG = "hostApp-log";


    private static IAdManager sInstance;

    private HwAdManager() {
    }

    public static IAdManager getInstance() {
        Log.i(TAG, "host call");

        if (sInstance != null) {
            return sInstance;
        }
        synchronized (HwAdManager.class) {
            if (sInstance == null) {
                if (!PluginManager.isInstall(Constants.PLUGIN_PATH)) {
                    Log.e(TAG, "plugin not install");
                    boolean isSuccess = PluginManager.simulateInstallExternalPlugin(App.getInstance(), Constants.PLUGIN_PATH);
                    Log.i(TAG, "plugin install success "+isSuccess);
                    if (isSuccess) {
                        initAdManager();
                    }
                } else {
                    Log.e(TAG, "plugin installed");
                    initAdManager();
                }
            }
        }
        return sInstance;
    }

    private static void initAdManager() {
        RePlugin.fetchContext(Constants.PLUGIN_NAME);
        ClassLoader pluginClassLoader = RePlugin.fetchClassLoader(Constants.PLUGIN_NAME);
        try {
            Class<?> adManagerClass = pluginClassLoader.loadClass("com.chanjalun.hwadplugin.AdManager");
            Method getInstance = adManagerClass.getDeclaredMethod("getInstance", new Class[0]);
            sInstance = (IAdManager) getInstance.invoke(null,  new Object[]{});
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getInstance error is "+e.toString());
        }
    }

    @Override
    public void init() {
        Log.i(TAG, "init call");
    }

    @Override
    public void requestSplashAd(Context context, Context context1, ViewGroup viewGroup) {
        sInstance.requestSplashAd(context, context1,viewGroup);
    }

    @Override
    public void requestNativeAd(Context context, Context context1, INativeAdCallback iNativeAdCallback) {
        sInstance.requestNativeAd(context, context1, iNativeAdCallback);
    }
}
