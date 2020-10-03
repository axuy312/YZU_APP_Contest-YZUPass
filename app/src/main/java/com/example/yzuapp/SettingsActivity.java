package com.example.yzuapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.suke.widget.SwitchButton;

public class SettingsActivity extends AppCompatActivity {

    ImageView backButton;
    com.suke.widget.SwitchButton darkButton;
    LinearLayout modifySystemSettings, modifyNotification, modifyNFC;

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

        //建立XML背景
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //讀取XML ID
        backButton = findViewById(R.id.backBtn);
        darkButton = findViewById(R.id.dark_theme_btn);
        modifySystemSettings = findViewById(R.id.modify_system_btn);
        modifyNotification = findViewById(R.id.modify_notify_btn);
        modifyNFC = findViewById(R.id.modify_nfc_btn);

        //調整dark theme switch
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_YES:
                darkButton.setChecked(true);
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                darkButton.setChecked(false);
                break;
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        darkButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                SharedPreferences preferences = getSharedPreferences("Theme", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);

                    setTheme(R.style.darkTheme);

                    darkButton.setChecked(true);

                    editor.putBoolean("themeColor", true);


                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);

                    setTheme(R.style.AppTheme);
                    darkButton.setChecked(false);

                    editor.putBoolean("themeColor", false);
                }
                editor.apply();
            }
        });

        modifySystemSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(SettingsActivity.this, "This settings is not support in your Android version.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        modifyNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getApplicationContext().getPackageName());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.putExtra("app_package", getApplicationContext().getPackageName());
                    intent.putExtra("app_uid", getApplicationContext().getApplicationInfo().uid);
                } else {
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                }
                startActivity(intent);
            }
        });

        modifyNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backButton.callOnClick();
    }
}
