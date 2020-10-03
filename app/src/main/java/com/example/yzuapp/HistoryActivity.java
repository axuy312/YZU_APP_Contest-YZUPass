package com.example.yzuapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.Fade;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.resources.TextAppearance;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    ImageView backButton;
    private TableLayout tableLayout;
    ProgressBar progressBar;
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences preferencesCode = getSharedPreferences("checkbox", MODE_PRIVATE);
        code = preferencesCode.getString("cardid", "");

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
        setContentView(R.layout.activity_history);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        backButton = findViewById(R.id.backBtn);
        tableLayout = findViewById(R.id.data_table);
        progressBar = findViewById(R.id.history_progress);

        //與上個頁面過場動畫
        Fade fade = new Fade();

        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);

        getHistory();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
        startActivity(intent);
    }

    private void updataView(String[] item) {
        //String[] item = str.split(",");
        int pixel = (findViewById(R.id.copy_icon)).getWidth();
        for (int i = 0; i < item.length; i += 2) {
            TableRow tableRow = new TableRow(this);
            tableRow.setBackground(getResources().getDrawable(R.drawable.table_down_width));
            TextView site = new TextView(this);
            TextView time = new TextView(this);
            ImageView icon = new ImageView(this);
            site.setTextSize(14);
            time.setTextSize(14);
            site.setText(item[i + 1]);
            time.setText(item[i]);

            site.setGravity(Gravity.CENTER);
            time.setGravity(Gravity.CENTER);

            //region 設定對應THEME
            SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);

            if (preferencesTheme.getBoolean("themeColor", false)) {
                //dark theme
                if ("圖書館".equals(item[i + 1])) {
                    icon.setImageResource(R.drawable.ic_library_dark);
                } else {
                    icon.setImageResource(R.drawable.ic_home_run_dark);
                }
            } else {
                //light theme
                if ("圖書館".equals(item[i + 1])) {
                    icon.setImageResource(R.drawable.ic_library);
                } else {
                    icon.setImageResource(R.drawable.ic_home_run);
                }
            }
            //endregion

            tableRow.addView(icon);
            tableRow.addView(site);
            tableRow.addView(time);
            tableRow.setGravity(Gravity.CENTER);
            tableRow.setPadding(0, 36, 0, 36);

            icon.requestLayout();
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.getLayoutParams().width = pixel / 2;
            icon.getLayoutParams().height = pixel / 2;
            icon.setPadding(25, 2, 5, 2);

            site.setWidth(pixel * 2);
            time.setWidth(pixel * 3);

            tableLayout.addView(tableRow);
        }

        progressBar.setVisibility(View.GONE);
    }

    private void getHistory() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("/Record/" + code);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String[] data = new String[(int) dataSnapshot.getChildrenCount() * 2];
                int i = (int) dataSnapshot.getChildrenCount() * 2 - 2;
                for (DataSnapshot key : dataSnapshot.getChildren()) {
                    Log.d("----dataSnapshot----", key.toString());
                    StringBuffer time = new StringBuffer();
                    time.append(key.child("time").getValue().toString());
                    //Log.d("Time:  ",time.toString());
                    //time.insert(4, '/').insert(7, '/').insert(10, " ").insert(13, ':');
                    data[i] = time.toString();
                    data[i + 1] = key.child("location").getValue().toString();
                    i -= 2;
                }
                updataView(data);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Failed to read value." + error.toException().toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
