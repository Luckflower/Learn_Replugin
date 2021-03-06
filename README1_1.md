# Replugin插件编译期做了什么？

预备知识，Replugin的host和plugin插件，都是依赖APK编译期的一些task进行插桩的生成、插件信息的获取、继承关系的改造等操作，所以需要提前了解一些APK的打包任务，可以参考下面这篇文章：  
[【Android 修炼手册】Android Gradle Plugin 主要 Task 分析](https://zhuanlan.zhihu.com/p/67049158)

在这里主要讲解一下Replugin在编译期做了哪些事情，才能使宿主去加载插件的四大组件。Replugin在编译期做的，分别通过replugin-host-gradle、replugin-plugin-gradle这两个gradle插件，对宿主和插件项目进行编译期的改造。  
接下来会从宿主和插件两个纬度去分析一下编译期的具体工作。

  
## 宿主的编译期

replugin-host-gradle，针对宿主应用编译期的注入任务：
* ComponentsGenerator - 生成带 RePlugin 插件坑位的 AndroidManifest.xml（允许自定义数量）
* RePluginHostConfigCreator - 生成 RepluginHostConfig 类，方便插件框架读取并自定义其属性
* PluginBuiltinJsonCreator - 生成 plugins-builtin.json，json中含有插件应用的信息，包名，插件名，插件路径等。  

下面是replugin-host-gradle中 Replugin.groovy 插件的部分代码：
```
    @Override
    public void apply(Project project) {
        println "${TAG} Welcome to replugin world ! "

        this.project = project

        /* Extensions */
        project.extensions.create(AppConstant.USER_CONFIG, RepluginConfig)

        if (project.plugins.hasPlugin(AppPlugin)) {

            def android = project.extensions.getByType(AppExtension)
            android.applicationVariants.all { variant ->

                addShowPluginTask(variant)

                if (config == null) {
                    config = project.extensions.getByName(AppConstant.USER_CONFIG)
                    checkUserConfig(config)
                }

                def generateBuildConfigTask = VariantCompat.getGenerateBuildConfigTask(variant)
                def appID = generateBuildConfigTask.appPackageName
                def newManifest = ComponentsGenerator.generateComponent(appID, config)
                println "${TAG} countTask=${config.countTask}"

                def variantData = variant.variantData
                def scope = variantData.scope

                //1.1、创建读取宿主插件配置信息的的Task - rpGenerateHostConfig。
                //host generate task
                def generateHostConfigTaskName = scope.getTaskName(AppConstant.TASK_GENERATE, "HostConfig")
                def generateHostConfigTask = project.task(generateHostConfigTaskName)

                generateHostConfigTask.doLast {
                    FileCreators.createHostConfig(project, variant, config)
                }
                generateHostConfigTask.group = AppConstant.TASKS_GROUP

                //1.2、rpGenerateHostConfig 依赖系统的 generateBuildConfigTask。
                //depends on build config task
                if (generateBuildConfigTask) {
                    generateHostConfigTask.dependsOn generateBuildConfigTask
                    generateBuildConfigTask.finalizedBy generateHostConfigTask
                }

                //3.1、创建读取宿主assets中插件信息，并生成Json文件的Task - rpGenerateBuiltinJson。
                //json generate task
                def generateBuiltinJsonTaskName = scope.getTaskName(AppConstant.TASK_GENERATE, "BuiltinJson")
                def generateBuiltinJsonTask = project.task(generateBuiltinJsonTaskName)

                generateBuiltinJsonTask.doLast {
                    FileCreators.createBuiltinJson(project, variant, config)
                }
                generateBuiltinJsonTask.group = AppConstant.TASKS_GROUP

                //3.2、rpGenerateBuiltinJson 依赖系统 mergeAssetsTask 执行之后执行（因为生成插件Json信息，需要读取assets下面的插件信息）。
                //depends on mergeAssets Task
                def mergeAssetsTask = VariantCompat.getMergeAssetsTask(variant)
                if (mergeAssetsTask) {
                    generateBuiltinJsonTask.dependsOn mergeAssetsTask
                    mergeAssetsTask.finalizedBy generateBuiltinJsonTask
                }

                variant.outputs.each { output ->
                    //2、在宿主的 Manifest 合并完成之后，需要进行 Manifest 的插桩操作。
                    VariantCompat.getProcessManifestTask(output).doLast {
                        println "${AppConstant.TAG} processManifest: ${it.outputs.files}"
                        it.outputs.files.each { File file ->
                            updateManifest(file, newManifest)
                        }
                    }
                }
            }
        }
    }
```

详细内容可以自己阅读源码，或者看下面的参考资料：  
参考资料：[《Replugin源码解析之replugin-host-gradle（宿主的gradle插件）》](https://www.jianshu.com/p/ca3bda0800b6)

## 插件的编译期

replugin-plugin-gradle，针对插件应用编译期的注入任务：  
动态修改插件中的调用代码，改为调用replugin-plugin-lib中的代码（如Activity的继承、Provider的重定向等）
* LoaderActivityInjector - 动态将插件中的 Activity 的继承相关代码修改为 replugin-plugin-lib 中的XXPluginActivity父类
* LocalBroadcastInjector - 替换插件中的 LocalBroadcastManager 调用代码 为 插件库的调用代码。
* ProviderInjector - 替换插件中的 ContentResolver 调用代码 为 插件库的调用代码
* ProviderInjector2 - 替换插件中的 ContentProviderClient 调用代码 为 插件库的调用代码
* GetIdentifierInjector - 替换插件中的 Resource.getIdentifier 调用代码的参数为动态适配的参数

下面是 replugin-plugin-gradle 中的 ReClassPlugin.groovy 部分源码：
```
    @Override
    public void apply(Project project) {
            
            ...
       
            //这里会生成 ReClassTransform
            def transform = new ReClassTransform(project)
            // 将 transform 注册到 android
            android.registerTransform(transform)
        }
    }
}

```

再看看 ReClassTransform 里面的部分源码：
```
    @Override
    void transform(Context context,
                   Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {
        
        ...
        
        //这里会遍历所有的 Injectors，这些Injectors会对Activity进行依赖关系的继承改造、Provider进行重定向等操作。
        def injectors = includedInjectors(config, variantDir)
        if (injectors.isEmpty()) {
            copyResult(inputs, outputProvider) // 跳过 reclass
        } else {
            doTransform(inputs, outputProvider, config, injectors) // 执行 reclass
        }
    }
    
   /**
     * 返回用户未忽略的注入器的集合
     */
    def includedInjectors(def cfg, String variantDir) {
        def injectors = []
        Injectors.values().each {
            //设置project
            it.injector.setProject(project)
            //设置variant关键dir
            it.injector.setVariantDir(variantDir)
            if (!(it.nickName in cfg.ignoredInjectors)) {
                injectors << it.nickName
            }
        }
        injectors
    }

```

再看看Injectors里面有哪些注入器：

```
public enum Injectors {

    LOADER_ACTIVITY_CHECK_INJECTOR('LoaderActivityInjector', new LoaderActivityInjector(), '替换 Activity 为 LoaderActivity'),
    LOCAL_BROADCAST_INJECTOR('LocalBroadcastInjector', new LocalBroadcastInjector(), '替换 LocalBroadcast 调用'),
    PROVIDER_INJECTOR('ProviderInjector', new ProviderInjector(), '替换 Provider 调用'),
    PROVIDER_INJECTOR2('ProviderInjector2', new ProviderInjector2(), '替换 ContentProviderClient 调用'),
    GET_IDENTIFIER_INJECTOR('GetIdentifierInjector', new GetIdentifierInjector(), '替换 Resource.getIdentifier 调用')

    IClassInjector injector
    String nickName
    String desc

    Injectors(String nickName, IClassInjector injector, String desc) {
        this.injector = injector
        this.nickName = nickName
        this.desc = desc;
    }
}
```

我们看看 LoaderActivityInjector 做了什么工作：
```
    /* LoaderActivity 替换规则 */
    def private static loaderActivityRules = [
            'android.app.Activity'                    : 'com.qihoo360.replugin.loader.a.PluginActivity',
            'android.app.TabActivity'                 : 'com.qihoo360.replugin.loader.a.PluginTabActivity',
            'android.app.ListActivity'                : 'com.qihoo360.replugin.loader.a.PluginListActivity',
            'android.app.ActivityGroup'               : 'com.qihoo360.replugin.loader.a.PluginActivityGroup',
            'androidx.fragment.app.FragmentActivity'  : 'com.qihoo360.replugin.loader.a.PluginFragmentActivity',
            'androidx.appcompat.app.AppCompatActivity': 'com.qihoo360.replugin.loader.a.PluginAppCompatActivity',
            'android.preference.PreferenceActivity'   : 'com.qihoo360.replugin.loader.a.PluginPreferenceActivity',
            'android.app.ExpandableListActivity'      : 'com.qihoo360.replugin.loader.a.PluginExpandableListActivity'
    ]
    
    /**
      * 该方法修改Activity的继承关系，将之前的继承关系对照上面的替换规则进行替换。
      */
    private def handleActivity(ClassPool pool, String activity, String classesDir) {
        def clsFilePath = classesDir + File.separatorChar + activity.replaceAll('\\.', '/') + '.class'
        if (!new File(clsFilePath).exists()) {
            return
        }

        println ">>> Handle $activity"

        def stream, ctCls
        try {
            stream = new FileInputStream(clsFilePath)
            ctCls = pool.makeClass(stream);
            
            // ctCls 之前的父类
            def originSuperCls = ctCls.superclass

            /* 从当前 Activity 往上回溯，直到找到需要替换的 Activity */
            def superCls = originSuperCls
            while (superCls != null && !(superCls.name in loaderActivityRules.keySet())) {
                // println ">>> 向上查找 $superCls.name"
                ctCls = superCls
                superCls = ctCls.superclass
            }

            // 如果 ctCls 已经是 LoaderActivity，则不修改
            if (ctCls.name in loaderActivityRules.values()) {
                // println "    跳过 ${ctCls.getName()}"
                return
            }

            /* 找到需要替换的 Activity, 修改 Activity 的父类为 LoaderActivity */
            if (superCls != null) {
                def targetSuperClsName = loaderActivityRules.get(superCls.name)
                // println "    ${ctCls.getName()} 的父类 $superCls.name 需要替换为 ${targetSuperClsName}"
                CtClass targetSuperCls = pool.get(targetSuperClsName)

                if (ctCls.isFrozen()) {
                    ctCls.defrost()
                }
                ctCls.setSuperclass(targetSuperCls)

                // 修改声明的父类后，还需要方法中所有的 super 调用。
                ctCls.getDeclaredMethods().each { outerMethod ->
                    outerMethod.instrument(new ExprEditor() {
                        @Override
                        void edit(MethodCall call) throws CannotCompileException {
                            if (call.isSuper()) {
                                if (call.getMethod().getReturnType().getName() == 'void') {
                                    call.replace('{super.' + call.getMethodName() + '($$);}')
                                } else {
                                    call.replace('{$_ = super.' + call.getMethodName() + '($$);}')
                                }
                            }
                        }
                    })
                }

                ctCls.writeFile(CommonData.getClassPath(ctCls.name))
                println "    Replace ${ctCls.name}'s SuperClass ${superCls.name} to ${targetSuperCls.name}"
            }

        } catch (Throwable t) {
            println "    [Warning] --> ${t.toString()}"
        } finally {
            if (ctCls != null) {
                ctCls.detach()
            }
            if (stream != null) {
                stream.close()
            }
        }
    }

```

这里可以看到，LoaderActivityInjector 的 handleActivity() 方法会在编译期将 Activity 的继承关系参照映射表规则进行修改。比如 MainActivity 之前继承的是 FragmentActivity，按照映射表规则，会将 MainActivity 修改为继承 PluginFragmentActivity。而修改后所继承的 PluginFragmentActivity，则会复写它所继承的 FragmentActivity 的一些方法，保证在插件里面的 Activiy 功能的正常使用。

```
public abstract class PluginFragmentActivity extends FragmentActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        newBase = RePluginInternal.createActivityContext(this, newBase);
        super.attachBaseContext(newBase);
    }

    @Override
    public Context getBaseContext() {

        return super.getBaseContext();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //
        RePluginInternal.handleActivityCreateBefore(this, savedInstanceState);

        super.onCreate(savedInstanceState);

        //
        RePluginInternal.handleActivityCreate(this, savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        //
        RePluginInternal.handleActivityDestroy(this);

        super.onDestroy();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //
        RePluginInternal.handleRestoreInstanceState(this, savedInstanceState);

        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Throwable e) {
            // Added by Jiongxuan Zhang
            // Crash Hash: B1F67129BC6A67C882AF2BBE62202BF0
            // java.lang.IllegalArgumentException: Wrong state class异常
            // 原因：恢复现场时，Activity坑位找错了。通常是用于占坑的Activity的层级过深导致
            // 举例：假如我们只有一个坑位可用，A和B分别是清理和通讯录的两个Activity
            //      如果进程重启，系统原本恢复B，却走到了A，从而出现此问题
            // 解决：将其Catch住，这样系统在找ViewState时不会出错。
            // 后遗症：
            // 1、可能无法恢复系统级View的保存的状态；
            // 2、如果自己代码处理不当，可能会出现异常。故自己代码一定要用SecExtraUtils来获取Bundle数据
            if (LogRelease.LOGR) {
                LogRelease.e("PluginFragmentActivity", "o r i s: p=" + getPackageCodePath() + "; " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void startActivity(Intent intent) {
        //
        if (RePluginInternal.startActivity(this, intent)) {
            // 这个地方不需要回调startActivityAfter，因为RePluginInternal最终还是会回调回来，最终还是要走super.startActivity()
            return;
        }

        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode, null);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        //
        if (RePluginInternal.startActivityForResult(this, intent, requestCode, options)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 16) {
            super.startActivityForResult(intent, requestCode, options);
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 这里的做法是支持Fragment中调用startActivityForResult特性
     * <p>
     * 由于卫士的插件需要hook住XXX-Activity的startActivity和startActivityForResult接口
     * 但早期版本的support-v4在startActivityFromFragment中直接调用了super.startActivityForResult, 因此这里还需要hook住这个点
     * 但新版的support-v4中Fragment最终的调用链还是会走到本XXX-Activity的startActivityForResult接口，因此不需要适配startActivityFromFragment(Fragment fragment, Intent intent, int requestCode, @Nullable Bundle options)接口
     *
     * @param fragment
     * @param intent
     * @param requestCode
     */
    @Override
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
        if (requestCode == -1) {
            startActivityForResult(intent, -1);
        } else if ((requestCode & -65536) != 0) {
            throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
        } else {
            int newRequestCode = -1;
            try {
                Field f = Fragment.class.getDeclaredField("mIndex");
                boolean acc = f.isAccessible();
                if (!acc) {
                    f.setAccessible(true);
                }
                Object o = f.get(fragment);
                if (!acc) {
                    f.setAccessible(acc);
                }
                int index = (Integer) o;
                newRequestCode = ((index + 1) << 16) + (requestCode & '\uffff');
            } catch (Throwable e) {
                // Do Noting
            }
            startActivityForResult(intent, newRequestCode);
        }
    }

    @Override
    public String getPackageCodePath() {
        return super.getPackageCodePath();
    }
}

```


详细内容可以自己阅读源码，或者看下面的参考资料：  
参考资料：[《Replugin源码解析之replugin-plugin-gradle（插件的gradle插件）》](https://www.jianshu.com/p/a9b3aaba8e45)  
