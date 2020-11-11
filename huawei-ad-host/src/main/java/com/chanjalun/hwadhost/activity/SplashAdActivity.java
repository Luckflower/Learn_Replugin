package com.chanjalun.hwadhost.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.chanjalun.hwadhost.App;
import com.chanjalun.hwadhost.Constants;
import com.chanjalun.hwadhost.HwAdManager;
import com.chanjalun.hwadhost.PluginManager;
import com.chanjalun.hwadhost.R;
import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 闪屏广告
 * 流程： 广告请求和广告sdk都封装在plugin中，点击按钮发起广告请求， 请求广告时将宿主的view container作为参数传递，
 * 广告请求成功后将广告view添加到该view container即可
 */
public class SplashAdActivity extends AppCompatActivity {

    public static final String TAG = "AdManager-log";

    private AssetManager pluginAssetManager;
    private Resources pluginResource;
    private Resources.Theme pluginTheme;


    private boolean isLoadResource = false;

    /**
     * 当前运行在
     */
    private boolean isPluginRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_ad);
        ViewGroup adContainer = findViewById(R.id.fl_ad_container);

        findViewById(R.id.load_ad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "host requestSplashAd RePlugin.fetchContext(Constants.PLUGIN_NAME) = "+RePlugin.fetchContext(Constants.PLUGIN_NAME).toString());
                Log.d(TAG, "host requestSplashAd hostContext = "+SplashAdActivity.this.toString());

                if (isLoadResource) {
                    isLoadResource = true;
                    return;
                }
                //方案1：直接将插件的res注入到宿主
            boolean isSuccess = PluginManager.injectResource(SplashAdActivity.this, getApkPath());
//            Log.i(TAG, "inject resource is success "+isSuccess);

                //方案1：加载插件res，并且重写宿主activity的getResource
//            loadPluginResource(getApkPath());
//            Log.i(TAG, "inject resource is success "+isSuccess);

                //方案3：将插件的res和宿主的res直接合并，这样有资源冲突问题，后续再解决
//                PluginManager.mergePluginResources(App.getInstance(), getApkPath());


//                PluginManager.preloadResource(SplashAdActivity.this, getApkPath());
                printResLog();

                isPluginRunning = true;
                HwAdManager.getInstance().requestSplashAd(RePlugin.fetchContext(Constants.PLUGIN_NAME), SplashAdActivity.this, adContainer);

//                RePlugin.startActivity(MainActivity.this, RePlugin.createIntent(Constants.PLUGIN_NAME, "com.chanjalun.hwadplugin.SplashActivity"));
//                RePlugin.startActivity(MainActivity.this, new Intent())
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "plugin splash ad onResume");
    }

    private void printResLog(){
        int resID =getResources().getIdentifier("hiad_24_dp", "dimen", this.getPackageName());
        Log.i(TAG, "inject resource resID "+resID);
        float hiad24 = getResources().getDimension(resID);
        Log.i(TAG, "inject resource hiad24 "+hiad24);

//        float hiad24 = getResources().getDimension(R.dimen.hiad_24_dp);
//        Log.i(TAG, "inject resource hiad24 "+hiad24);

//        float hostAppHiad24 = App.getInstance().getResources().getDimension(R.dimen.hiad_24_dp);
//        Log.i(TAG, "inject resource hostApp Hiad24 "+hostAppHiad24);

//        float pluginHiad24 = RePlugin.fetchContext(Constants.PLUGIN_NAME).getResources().getDimension(R.dimen.hiad_24_dp);
//        Log.i(TAG, "inject resource pluginHiad24 "+pluginHiad24);
    }

    private String getApkPath() {
        // 文件是否已经存在？直接删除重来
        String pluginFilePath = this.getFilesDir().getAbsolutePath() + File.separator + Constants.PLUGIN_PATH;
        File pluginFile = new File(pluginFilePath);
        if (pluginFile.exists()) {
            FileUtils.deleteQuietly(pluginFile);
        }
        // 开始复制
        PluginManager.copyAssetsFileToAppFiles(this, Constants.PLUGIN_PATH, pluginFilePath);
        return pluginFilePath;
    }


    /**
     * 将插件apk资源解压，并使用assetmanager加载
     * @param dexPath
     */
    public void loadPluginResource(String dexPath) {
        try{
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            pluginAssetManager = assetManager;
        }catch (Exception e){
            e.printStackTrace();
        }
        pluginResource = new Resources(pluginAssetManager, super.getResources().getDisplayMetrics(), super.getResources().getConfiguration());
        pluginTheme = pluginResource.newTheme();
        pluginTheme.setTo(super.getTheme());
    }



//    @Override
//    public Resources getResources() {
//        if (pluginResource == null) {
//            return super.getResources();
//        }
//        return pluginResource;
//    }

//    @Override
//    public Resources.Theme getTheme() {
//        return super.getTheme();
////        if (pluginTheme == null) {
////            return super.getTheme();
////        }
////        return pluginTheme;
//    }

//    @Override
//    public AssetManager getAssets() {
//        if (pluginAssetManager == null) {
//            return super.getAssets();
//        }
//        return pluginAssetManager;
//    }


//    @Override
//    public ClassLoader getClassLoader() {
//        if (isPluginRunning) {
//            Context pluginContext = RePlugin.fetchContext(Constants.PLUGIN_NAME);
//            return pluginContext.getClassLoader();
//        }
//        return super.getClassLoader();
//    }
}