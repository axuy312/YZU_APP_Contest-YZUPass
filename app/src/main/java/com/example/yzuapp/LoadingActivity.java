package com.example.yzuapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class LoadingActivity extends AppCompatActivity {

    private String token = "", sid = "", card = "";
    ProgressBar load;
    ImageView icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        /*SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);

        if (preferencesTheme.getBoolean("themeColor", false)) {
            //dark theme
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.darkTheme);
        } else {
            //light theme
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setTheme(R.style.AppTheme);
        }*/
        //endregion

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        load = findViewById(R.id.loading_progress);
        icon = findViewById(R.id.loading_ic);
        Animation loading_anim = AnimationUtils.loadAnimation(this, R.anim.loading_transition);
        load.startAnimation(loading_anim);
        icon.startAnimation(loading_anim);
        //load.setProgress(10);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("----Fail----", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        token = task.getResult().getToken();

                        login();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.d("----token----", token);
                        Log.d("---Failure Listener---", e.toString());
                        startLoginActivity();
                    }
                });

    }


    void login() {

        //if login before, go through to home activity
        SharedPreferences preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        String checkbox = preferences.getString("remember", "");

        //fill in data
        if (checkbox.equals("true")) {

            sid = preferences.getString("studentid", "");
            card = preferences.getString("cardid", "");

            //load.setProgress(20);
        }

        Log.d("----token----", token);
        Log.d("----Sid----", sid);
        Log.d("----Card ID----", card);

        if (!sid.equals("") && !card.equals("")) {
            //firebase判斷帳密和token是否相同
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            final CollectionReference userRef = db.collection("User");
            //load.setProgress(50);
            userRef.document(card)
                    .get(Source.SERVER)
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            //load.setProgress(100);
                            if (documentSnapshot.exists() && documentSnapshot.get("id").toString().equals(sid) && documentSnapshot.get("code").toString().equals(card)) {

                                Log.d("----token----", token);

                                if (!documentSnapshot.contains("token") || documentSnapshot.get("token").toString().equals(token)) {

                                    userRef.document(card).update("token", token);

                                    Intent intent = new Intent(LoadingActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoadingActivity.this, "帳號已在其他裝置登入", Toast.LENGTH_SHORT).show();

                                    startLoginActivity();
                                }
                            } else {
                                Toast.makeText(LoadingActivity.this, "學號或卡號錯誤", Toast.LENGTH_SHORT).show();

                                startLoginActivity();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoadingActivity.this, "SomeThing Wrong: " + e, Toast.LENGTH_LONG).show();
                            startLoginActivity();
                        }
                    });
        } else {
            startLoginActivity();

            //Toast.makeText(LoadingActivity.this, "Start 2", Toast.LENGTH_SHORT).show();
        }

    }

    public void startLoginActivity() {
        Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        //finish();
    }
}
