package com.chanjalun.hwadhost;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.model.PluginInfo;
import com.qihoo360.replugin.utils.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 插件安装升级启动管理类
 */
public class PluginManager {

    public static final String TAG = "AdManager-log";

    /**
     * 判断一个插件是否已经安装
     * @param pluginName
     * @return
     */
    public static boolean isInstall(String pluginName) {
        return RePlugin.isPluginInstalled(pluginName);
    }

    /**
     * 模拟安装或升级（覆盖安装）外置插件
     * 注意：为方便演示，外置插件临时放置到Host的assets/external目录下，具体说明见README</p>
     */
    public static boolean simulateInstallExternalPlugin(Context context, String pluginApk) {
        String demoApk = pluginApk;
        String demoApkPath = demoApk;

        // 文件是否已经存在？直接删除重来
        String pluginFilePath = context.getFilesDir().getAbsolutePath() + File.separator + demoApkPath;
        File pluginFile = new File(pluginFilePath);
        if (pluginFile.exists()) {
            FileUtils.deleteQuietly(pluginFile);
        }

        // 开始复制
        copyAssetsFileToAppFiles(context, demoApk, pluginFilePath);
        PluginInfo info = null;
        if (pluginFile.exists()) {
            info = RePlugin.install(pluginFilePath);
            RePlugin.preload(info);
        }

        if (info != null) {
            return true;
        }

        return false;
    }

    /**
     * 从assets目录中复制某文件内容
     *
     * @param assetFileName assets目录下的Apk源文件路径
     * @param newFileName   复制到/data/data/package_name/files/目录下文件名
     */
    public static void copyAssetsFileToAppFiles(Context context, String assetFileName, String newFileName) {
        InputStream is = null;
        FileOutputStream fos = null;
        int buffsize = 1024;

        try {
            is = context.getAssets().open(assetFileName);
            fos = new FileOutputStream(new File(newFileName), true);
//            fos = context.openFileOutput(newFileName, Context.MODE_PRIVATE);
            int byteCount = 0;
            byte[] buffer = new byte[buffsize];
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean injectResource(Context context, String apkPath) {
        Log.i(TAG, "start inject resource from : "+apkPath);
        AssetManager assetManager = context.getAssets();
        try {
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            int ret = (Integer) addAssetPath.invoke(assetManager, apkPath);
            Log.d(TAG, "inject resource success path = "+ apkPath + ", ret=" + ret);
            return true;
        } catch (IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "inject resource failed : "+apkPath);
        return false;
    }

    /**
     * 合并插件和宿主的res
     * @param application
     * @param apkPath
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     */
    public static void mergePluginResources(Application application, String apkPath) {
        // 创建一个新的 AssetManager 对象
        try {
            AssetManager newAssetManagerObj = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            // 塞入原来宿主的资源
            addAssetPath.invoke(newAssetManagerObj, application.getBaseContext().getPackageResourcePath());
            // 塞入插件的资源
//        File optDexFile = application.getBaseContext().getFileStreamPath(apkPath);
            addAssetPath.invoke(newAssetManagerObj, apkPath);

            // ----------------------------------------------

            // 创建一个新的 Resources 对象
            Resources newResourcesObj = new Resources(newAssetManagerObj,
                    application.getBaseContext().getResources().getDisplayMetrics(),
                    application.getBaseContext().getResources().getConfiguration());

            // ----------------------------------------------

            // 获取 ContextImpl 中的 Resources 类型的 mResources 变量，并替换它的值为新的 Resources 对象
            Field resourcesField = application.getBaseContext().getClass().getDeclaredField("mResources");
            resourcesField.setAccessible(true);
            resourcesField.set(application.getBaseContext(), newResourcesObj);

            // ----------------------------------------------

            // 获取 ContextImpl 中的 LoadedApk 类型的 mPackageInfo 变量
            Field packageInfoField = application.getBaseContext().getClass().getDeclaredField("mPackageInfo");
            packageInfoField.setAccessible(true);
            Object packageInfoObj = packageInfoField.get(application.getBaseContext());

            // 获取 mPackageInfo 变量对象中类的 Resources 类型的 mResources 变量，，并替换它的值为新的 Resources 对象
            // 注意：这是最主要的需要替换的，如果不需要支持插件运行时更新，只留这一个就可以了
            Field resourcesField2 = packageInfoObj.getClass().getDeclaredField("mResources");
            resourcesField2.setAccessible(true);
            resourcesField2.set(packageInfoObj, newResourcesObj);

            // ----------------------------------------------

            // 获取 ContextImpl 中的 Resources.Theme 类型的 mTheme 变量，并至空它
            // 注意：清理mTheme对象，否则通过inflate方式加载资源会报错, 如果是activity动态加载插件，则需要把activity的mTheme对象也设置为null
            Field themeField = application.getBaseContext().getClass().getDeclaredField("mTheme");
            themeField.setAccessible(true);
            themeField.set(application.getBaseContext(), null);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    public synchronized static void preloadResource(Context context, String apkFilePath) {
        try {
            // 先创建AssetManager
            Class<? extends AssetManager> AssetManagerClass = AssetManager.class;
            AssetManager assetManager = AssetManagerClass.newInstance();
            // 将插件资源和宿主资源通过 addAssetPath方法添加进去
            Method addAssetPathMethod = AssetManagerClass.getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            String hostResourcePath = context.getPackageResourcePath();
            int result_1 = (int) addAssetPathMethod.invoke(assetManager, hostResourcePath);
            int result_2 = (int) addAssetPathMethod.invoke(assetManager, apkFilePath);
            // 接下来创建，合并资源后的Resource
            Resources resources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
            // 替换 ContextImpl 中Resource对象
            Class<?> contextImplClass = context.getClass();
            Field resourcesField1 = contextImplClass.getDeclaredField("mResources");
            resourcesField1.setAccessible(true);
            resourcesField1.set(context, resources);
            // 先获取到LoadApk对象
            Field loadedApkField = contextImplClass.getDeclaredField("mPackageInfo");
            loadedApkField.setAccessible(true);
            Object loadApk = loadedApkField.get(context);
            Class<?> loadApkClass = loadApk.getClass();
            // 替换掉LoadApk中的Resource对象。
            Field resourcesField2 = loadApkClass.getDeclaredField("mResources");
            resourcesField2.setAccessible(true);
            resourcesField2.set(loadApk, resources);

            //获取到ActivityThread
            Class<?> ActivityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = ActivityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object ActivityThread = sCurrentActivityThreadField.get(null);
            // 获取到ResourceManager对象
            Field ResourcesManagerField = ActivityThreadClass.getDeclaredField("mResourcesManager");
            ResourcesManagerField.setAccessible(true);
            Object resourcesManager = ResourcesManagerField.get(ActivityThread);
            // 替换掉ResourceManager中resource对象
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Class<?> resourcesManagerClass = resourcesManager.getClass();
                Field mActiveResourcesField = resourcesManagerClass.getDeclaredField("mActiveResources");
                mActiveResourcesField.setAccessible(true);
                Map<Object, WeakReference<Resources>> map = (Map<Object, WeakReference<Resources>>) mActiveResourcesField.get(resourcesManager);
                Object key = map.keySet().iterator().next();
                map.put(key, new WeakReference<>(resources));
            } else {
                // still hook Android N Resources, even though it's unnecessary, then nobody will be strange.
                Class<?> resourcesManagerClass = resourcesManager.getClass();
                Field mResourceImplsField = resourcesManagerClass.getDeclaredField("mResourceImpls");
                mResourceImplsField.setAccessible(true);
                Map map = (Map) mResourceImplsField.get(resourcesManager);
                Object key = map.keySet().iterator().next();
                Field mResourcesImplField = Resources.class.getDeclaredField("mResourcesImpl");
                mResourcesImplField.setAccessible(true);
                Object resourcesImpl = mResourcesImplField.get(resources);
                map.put(key, new WeakReference<>(resourcesImpl));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void copyApk2Inner(Context context,String apkName){
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        BufferedOutputStream bos = null;
        try {
            inputStream = assetManager.open(apkName);
            File plugin_odex_dir = context.getDir("plugin_odex", Context.MODE_PRIVATE);
            Log.i(TAG,"文件夹目录的路径是    "+plugin_odex_dir.getAbsolutePath());
            String filePath = plugin_odex_dir.getAbsolutePath()+File.separator+apkName;
            File file = new File(filePath);
            if(file.exists()){
                boolean delete = file.delete();
                Log.i(TAG, "删除存在的插件apk  "+delete);
            }
            //注意，这里是要传具体的文件路径构建的File对象，不是文件所在的文件夹的路径构建的File对象
            bos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len= inputStream.read(bytes))!=-1){
                bos.write(bytes,0,len);
            }
            //完成了将插件apk从assets目录复制到apk内部的odex目录下面
            if(file.exists()){
                Log.i(TAG,"文件复制成功       "+file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != bos){
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(null != inputStream){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
