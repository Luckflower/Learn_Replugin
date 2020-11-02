package com.chanjalun.hwadplugin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chanjalun.hwadplugin.R;
import com.huawei.openalliance.ad.views.PPSSplashView;

public class SplashAdView extends RelativeLayout {

    private Context context;

    private RelativeLayout rlLogoLayout;
    private PPSSplashView ppsSplashView;

    public SplashAdView(Context context) {
        super(context);
        init(context);
    }

    public SplashAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SplashAdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        View rootView = LayoutInflater.from(context).inflate(R.layout.huawei_ad_screen_layout, this, true);
        rlLogoLayout = rootView.findViewById(R.id.logo);
        ppsSplashView = rootView.findViewById(R.id.pps_splash_view);
        rlLogoLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "点击了插件广告", Toast.LENGTH_LONG).show();
            }
        });
    }

    public View getLogoView(){
        return rlLogoLayout;
    }

    public View getPPSView(){
        return ppsSplashView;
    }

}
