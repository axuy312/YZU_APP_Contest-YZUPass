package com.example.yzuapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button loginButton;
    private EditText studentID, cardID;
    private NfcAdapter nfcAdapter;
    CheckBox remember;
    LinearLayout progressBar;
    TextInputLayout textInputSid, textInputCardid;
    boolean darkTheme;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);
        darkTheme = preferencesTheme.getBoolean("themeColor", false);


        if (darkTheme) {
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
        //建立View
        setContentView(R.layout.activity_main);
        //強制螢幕直立
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //get xml file id
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        loginButton = findViewById(R.id.loginBtn);
        studentID = findViewById(R.id.input_sid);
        cardID = findViewById(R.id.input_card_id);
        remember = findViewById(R.id.remember_me);
        progressBar = findViewById(R.id.progress_bar);
        textInputSid = findViewById(R.id.input_layout_sid);
        textInputCardid = findViewById(R.id.input_layout_cid);

        //checkbox fill with sid & card id
        SharedPreferences preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        String checkbox = preferences.getString("remember", "");

        //fill in data
        if (checkbox.equals("true")) {

            String newSid = preferences.getString("studentid", "");
            String newCardid = preferences.getString("cardid", "");

            studentID.setText(newSid);
            cardID.setText(newCardid);
            remember.setChecked(true);
        } else if (checkbox.equals("false")) {
            remember.setChecked(false);
        }

        textInputSid.setError(null);
        textInputSid.setErrorEnabled(false);
        textInputCardid.setError(null);
        textInputCardid.setErrorEnabled(false);

        //default button
        progressBar.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);

        if (nfcAdapter == null) {
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show();
        } else if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC disabled on this device. Turn on to proceed", Toast.LENGTH_SHORT).show();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (validateData()) {
                    SaveData();

                    //control place
                    progressBar.setVisibility(View.VISIBLE);
                    loginButton.setVisibility(View.GONE);

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
                                    Toast.makeText(MainActivity.this, "Please Connect to Internet.", Toast.LENGTH_LONG).show();
                                    progressBar.setVisibility(View.GONE);
                                    loginButton.setVisibility(View.VISIBLE);
                                }
                            });
                    //postRequest();
                }
            }
        });

        remember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SaveData();
            }
        });

        studentID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSid();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        cardID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateCid();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            enableForegroundDispatchSystem();
        }
    }


    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (nfcAdapter == null)
            return;
        //檢查intent 的行動是否是 ACTION_TAG_DISCOVERED
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            //檢查tag是否是我們的格式
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] paramArrayOfbyte = detectedTag.getId();
            long l2 = 0L;
            long l1 = 1L;
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; ; i++) {
                if (i >= paramArrayOfbyte.length)
                    break;
                l2 += (paramArrayOfbyte[i] & 0xFFL) * l1;
                l1 *= 256L;
            }
            cardID.setText(String.valueOf(l2));
        }
    }

    void login() {
        final String sid, card;
        sid = studentID.getText().toString();
        card = cardID.getText().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference userRef = db.collection("User");
        userRef.document(card)
                .get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists() && documentSnapshot.get("id").toString().equals(sid)) {

                            Log.d("----token----", token);
                            if (!documentSnapshot.contains("token") || documentSnapshot.get("token").toString().equals(token) || documentSnapshot.get("token").toString().equals("empty")) {

                                userRef.document(card).update("token", token);

                                Intent intent = new Intent(MainActivity.this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(MainActivity.this, "你的帳號已在其他裝置登入", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                loginButton.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "學號或卡號錯誤", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            loginButton.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Please Connect to Internet.", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        loginButton.setVisibility(View.VISIBLE);
                    }
                });

    }

    /*已棄用
    private void postRequest() {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        String url = "https://script.google.com/macros/s/AKfycbzSq8-oakUqsMBqF8p7lgzbxCFOAtuw0q1i2SC_wrTplvwOEco/exec";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response.equals("1")) {
                    Toast.makeText(MainActivity.this, "登入成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "學號或卡號錯誤", Toast.LENGTH_SHORT).show();

                    loginButton.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "連線失敗~", Toast.LENGTH_SHORT).show();

                loginButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("method", "CardIDCheck");
                params.put("id", studentID.getText().toString());
                params.put("code", cardID.getText().toString());
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
    }
    */

    private boolean validateData() {
        return (validateSid() && validateCid());
    }

    private boolean validateSid() {
        String tmpSid = studentID.getEditableText().toString();

        if (tmpSid.isEmpty()) {
            textInputSid.setError("Student Id Cannot be Empty");
            return false;
        } else {
            textInputSid.setError(null);
            textInputSid.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateCid() {
        String tmpCid = cardID.getText().toString();

        if (tmpCid.isEmpty()) {
            textInputCardid.setError("Card Id cannot be Empty");
            return false;
        } else {
            textInputCardid.setError(null);
            textInputCardid.setErrorEnabled(false);
            return true;
        }
    }

    private void SaveData() {

        //save student id & card variable
        SharedPreferences preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (remember.isChecked()) {
            editor.putString("remember", "true");
        } else {
            editor.putString("remember", "false");
        }

        editor.putString("studentid", studentID.getText().toString());
        editor.putString("cardid", cardID.getText().toString());
        editor.apply();
    }
}
