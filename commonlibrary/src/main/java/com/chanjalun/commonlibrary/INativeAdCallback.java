package com.chanjalun.commonlibrary;

import java.util.List;
import java.util.Map;

public interface INativeAdCallback {
    public void onAdsLoaded(Object ads);
    public void onAdFailed(int errorCode);
}
