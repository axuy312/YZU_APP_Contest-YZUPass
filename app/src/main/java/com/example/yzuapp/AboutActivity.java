package com.example.yzuapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    TextView version;
    String ver;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);

        if (preferencesTheme.getBoolean("themeColor", false)) {
            //dark theme
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.darkTheme);
        } else {
            //light theme
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setTheme(R.style.AppTheme);
        }
        //endregion

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        version = findViewById(R.id.app_verison);
        backButton = findViewById(R.id.backBtnAbout);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            ver = "Version: " + versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        version.setText(ver);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
