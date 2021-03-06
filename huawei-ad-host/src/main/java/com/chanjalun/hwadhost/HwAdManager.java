package com.chanjalun.hwadhost;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.chanjalun.commonlibrary.IAdManager;
import com.qihoo360.replugin.RePlugin;

import java.lang.reflect.Method;

public class HwAdManager implements IAdManager {

//    public static final String TAG = "HwAdManager-log";
    public static final String TAG = "hostApp-log";


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
                boolean isSuccess = PluginManager.simulateInstallExternalPlugin(App.getInstance(), Constants.PLUGIN_PATH);
                Log.i(TAG, "plugin install success "+isSuccess);
                if (isSuccess) {
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
            }
        }
        return sInstance;
    }

    @Override
    public void init() {
        Log.i(TAG, "init call");
    }

    @Override
    public void requestSplashAd(Context context, Context context1, ViewGroup viewGroup) {
        sInstance.requestSplashAd(context, context1,viewGroup);
    }
}
