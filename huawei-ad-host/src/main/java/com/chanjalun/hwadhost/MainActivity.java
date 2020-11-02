package com.chanjalun.hwadhost;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.chanjalun.commonlibrary.IAdManager;
import com.qihoo360.replugin.RePlugin;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewGroup adContainer = findViewById(R.id.fl_ad_container);

        findViewById(R.id.load_ad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                RePlugin.startActivity(MainActivity.this, RePlugin.createIntent(Constants.PLUGIN_NAME, "com.chanjalun.hwadplugin.SplashActivity"));
                HwAdManager.getInstance().requestSplashAd(RePlugin.fetchContext(Constants.PLUGIN_NAME), MainActivity.this, adContainer);
//                RePlugin.startActivity(MainActivity.this, new Intent())
            }
        });
    }
}