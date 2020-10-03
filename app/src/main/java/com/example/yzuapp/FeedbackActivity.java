package com.example.yzuapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    EditText feedback;
    String feedbackText;
    Button send;
    ImageView backButton;
    String cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);

        SharedPreferences preferencesCode = getSharedPreferences("checkbox", MODE_PRIVATE);
        cardId = preferencesCode.getString("cardid", "");

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
        setContentView(R.layout.activity_feedback);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        feedback = findViewById(R.id.feedback_text);
        send = findViewById(R.id.send_btn);
        backButton = findViewById(R.id.backBtnFeedback);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //get text from user
                feedbackText = feedback.getText().toString();

                FirebaseDatabase db = FirebaseDatabase.getInstance();
                DatabaseReference ref = db.getReference("/Feedback/"+cardId);

                Map<String, Object>data = new HashMap<>();
                data.put("suggest",feedbackText);
                data.put("time", ServerValue.TIMESTAMP);
                ref.child(String.valueOf(System.currentTimeMillis())).setValue(data);
                //after send
                finish();

            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
