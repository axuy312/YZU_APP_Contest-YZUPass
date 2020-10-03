package com.example.yzuapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.transition.Fade;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.suke.widget.SwitchButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import static android.provider.Settings.System.canWrite;

public class qrcode_activity extends AppCompatActivity {

    com.suke.widget.SwitchButton auto_Brightness;
    ImageView backButton, qrimg;
    int highestBrightness = 255, brightness = -1, countdown = 0;
    boolean successPermission = true;
    Handler mHandlerTime = new Handler();
    Bitmap bitmap;
    QRGEncoder qrgEncoder;
    String  CardID;
    TextView counting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);
        SharedPreferences preferencesCode = getSharedPreferences("checkbox", MODE_PRIVATE);
        CardID = preferencesCode.getString("cardid", "");

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
        setContentView(R.layout.activity_qrcode_activity);

        //設定螢幕垂直
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //與XML檔案做連結
        backButton = findViewById(R.id.backBtn);
        auto_Brightness = findViewById(R.id.auto_brightness);
        counting = findViewById(R.id.counting);
        qrimg = findViewById(R.id.qrgenerate);


        //背景執行post data
        new SendData().execute();

        //與上個頁面過場動畫
        Fade fade = new Fade();

        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);

        //判斷使用者是否有給app最高亮度權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(getApplicationContext())) {
            successPermission = false;
        }

        //沒給權限引導使用者開啟
        if (successPermission) {
            brightness = Settings.System.getInt(getApplicationContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, highestBrightness);
            //Toast.makeText(qrcode_activity.this, String.valueOf(brightness), Toast.LENGTH_SHORT).show();
            auto_Brightness.setChecked(true);
        } else {
            auto_Brightness.setChecked(false);
            getPermission(true);
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (successPermission)
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                Intent intent = new Intent(qrcode_activity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        auto_Brightness.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {

                getPermission(false);

                if (auto_Brightness.isChecked()) {
                    Log.d("--onYes--", String.valueOf(brightness));
                    //auto_Brightness.setChecked(true);
                    brightness = Settings.System.getInt(getApplicationContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);

                } else if (brightness >= 0) {

                    Log.d("--onNo--", String.valueOf(brightness));
                    //auto_Brightness.setChecked(false);
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);

                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("--onDestroy--", String.valueOf(brightness));
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference qrcodeQef = db.collection("QRcode");
        qrcodeQef.document(CardID).delete();
        auto_Brightness.setChecked(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("--onStop--", String.valueOf(brightness));
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference qrcodeQef = db.collection("QRcode");
        qrcodeQef.document(CardID).delete();
        auto_Brightness.setChecked(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("--onRestart--", String.valueOf(brightness));
        new SendData().execute();
        auto_Brightness.setChecked(true);
    }


    //取得權限
    private void getPermission(boolean ask) {
        if (successPermission) {
            return;
        }
        boolean value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            value = Settings.System.canWrite(getApplicationContext());
            if (value) {
                successPermission = true;
            } else {
                successPermission = false;
                if (ask) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                    startActivity(intent);
                }
            }
        } else {
            successPermission = true;
        }
    }

    //倒數時間
    private void counter() {
        if (countdown != 0) {
            counting.setText("Validate Time: " + countdown);
        } else {
            counting.setText(R.string.qr_expire);
        }
    }

    private final Runnable timerRun = new Runnable() {
        @Override
        public void run() {
            counter();

            if (countdown != 0) {
                --countdown;
                mHandlerTime.postDelayed(timerRun, 1000);
            } else {
                Log.d("--time out---", "no time");
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference qrcodeQef = db.collection("QRcode");
                qrcodeQef.document(CardID).delete();
            }
        }
    };

    @Override
    public void onBackPressed() {
        backButton.callOnClick();
        super.onBackPressed();
    }

    private class SendData extends AsyncTask<String, Integer, String> {
        ProgressBar qrProgress;

        @Override
        protected void onPreExecute() {
            //執行前
            super.onPreExecute();

            qrProgress = findViewById(R.id.qr_progress);
            qrProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {

            //執行中 在背景做事情
            return qrAddition(CardID);
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            //執行中
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String string) {
            //執行後 完成
            super.onPostExecute(string);

            qrProgress.setVisibility(View.GONE);
            qrimg.setVisibility(View.VISIBLE);

            QRCodeGenerator(string);

            if (countdown != 0) {
                countdown = 60;
            } else {
                countdown = 60;
                mHandlerTime.postDelayed(timerRun, 0);
            }
        }
    }

    /*private String qrAddition(final String code, final String id) {

        Log.d("---debug---", code);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("/QRcode/"+code);
        myRef.setValue(ServerValue.TIMESTAMP);


        return code;
        /*RequestQueue requestQueue = Volley.newRequestQueue(qrcode_activity.this);
        String url = "https://script.google.com/macros/s/AKfycbzSq8-oakUqsMBqF8p7lgzbxCFOAtuw0q1i2SC_wrTplvwOEco/exec";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response.equals("1")) {
                    Toast.makeText(qrcode_activity.this, "Database Connected: " + response, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(qrcode_activity.this, "Database Wrong: " + response, Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(qrcode_activity.this, "連線失敗~", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("method", "QRcodeWrite");
                params.put("id", id);
                params.put("code", code);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }*/
    private String qrAddition(final String code) {

        Log.d("---debug---", code);

        Map<String, Object> data = new HashMap<>();
        data.put("time", System.currentTimeMillis());
        data.put("code", code);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference qrcodeRef = db.collection("QRcode");
        qrcodeRef.document(code).set(data);

        return code;
    }

    private void QRCodeGenerator(String code) {
        if (code.length() > 0) {
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            int width = point.x;
            int height = point.y;
            int smallerDimension = Math.min(width, height);
            smallerDimension = smallerDimension * 3 / 4;

            //設定QR code 顏色
            qrgEncoder = new QRGEncoder(code, null, QRGContents.Type.TEXT, smallerDimension);
            qrgEncoder.setColorBlack(Color.WHITE);
            qrgEncoder.setColorWhite(android.R.color.transparent);

            try {
                // Getting QR-Code as Bitmap
                bitmap = qrgEncoder.getBitmap();
                // Setting Bitmap to ImageView
                qrimg.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
