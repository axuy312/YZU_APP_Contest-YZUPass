package com.example.yzuapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.telecom.Call;
import android.transition.Fade;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.yzuapp.nfc.OutcomingNfcManager;

import pl.droidsonroids.gif.GifImageView;

public class NFCActivity extends AppCompatActivity implements OutcomingNfcManager.NfcActivity {

    private ImageView back;
    private NfcAdapter nfcAdapter;
    private OutcomingNfcManager outcomingNfccallback;
    private String cardID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);
        SharedPreferences preferencesCode = getSharedPreferences("checkbox", MODE_PRIVATE);
        cardID = preferencesCode.getString("cardid", "");

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
        setContentView(R.layout.activity_nfc);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        GifImageView gib = findViewById(R.id.nfc_gif);

        if (preferencesTheme.getBoolean("themeColor", false))
            gib.setImageResource(R.drawable.gif_nfc_icon_dark);
        else
            gib.setImageResource(R.drawable.gif_nfc_icon);

        back = findViewById(R.id.backBtn);

        Fade fade = new Fade();
        //View decor = getWindow().getDecorView();
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);


        if (!isNfcSupported()) {
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show();
        } else if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC disabled on this device. Turn on to proceed", Toast.LENGTH_SHORT).show();
        }

        if (nfcAdapter != null) {
            // encapsulate sending logic in a separate class
            this.outcomingNfccallback = new OutcomingNfcManager(this);
            this.nfcAdapter.setOnNdefPushCompleteCallback(outcomingNfccallback, this);
            this.nfcAdapter.setNdefPushMessageCallback(outcomingNfccallback, this);
        }


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private boolean isNfcSupported() {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        return this.nfcAdapter != null;
    }


    @Override
    public String getOutcomingMessage() {
        return cardID;
    }


    @Override
    public void signalResult() {
        // this will be triggered when NFC message is sent to a device.
        // should be triggered on UI thread. We specify it explicitly
        // cause onNdefPushComplete is called from the Binder thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NFCActivity.this, "感應成功~", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
