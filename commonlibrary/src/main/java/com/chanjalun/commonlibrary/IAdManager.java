package com.chanjalun.commonlibrary;

import android.content.Context;
import android.view.ViewGroup;

public interface IAdManager {
    public void init();
    public void requestSplashAd(Context pluginContext, Context hostContext, ViewGroup viewContainer);
    public void requestNativeAd(Context pluginContext, Context hostContext, INativeAdCallback callback);
}
