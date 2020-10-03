package com.example.yzuapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.Fade;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.yzuapp.nfc.OutcomingNfcManager;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    CardView NFCButton, QRcodeButton, HistoryButton;
    ImageView ProfileImg, MenuButton;
    String sid, url, code;
    LinearLayout loadingImg;
    TextView Profile_name, Profile_title;
    EditText editName, editDescription;
    LinearLayout profile;
    private StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imgUri;
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region 設定背景顏色
        SharedPreferences preferencesTheme = getSharedPreferences("Theme", MODE_PRIVATE);
        SharedPreferences preferencesCode = getSharedPreferences("checkbox", MODE_PRIVATE);
        sid = preferencesCode.getString("studentid", "");
        code = preferencesCode.getString("cardid", "");

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
        setContentView(R.layout.activity_home);

        //設定垂直頁面
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        //與XML做連結
        ProfileImg = findViewById(R.id.profile_image);
        NFCButton = findViewById(R.id.nfc_pic);
        QRcodeButton = findViewById(R.id.qr_pic);
        HistoryButton = findViewById(R.id.history_pic);
        MenuButton = findViewById(R.id.home_menu);
        loadingImg = findViewById(R.id.load_Img);
        Profile_name = findViewById(R.id.profile_name);
        Profile_title = findViewById(R.id.profile_title);
        profile = findViewById(R.id.profile_text);

        //FireStore init
        storageReference = FirebaseStorage.getInstance().getReference("uploads/profile picture");

        //與NFC QR 過場
        Fade fade = new Fade();

        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);

        //取得大頭貼
        getProfile();

        NFCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NFCActivity.class);

                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(HomeActivity.this, NFCButton, ViewCompat.getTransitionName(NFCButton));

                startActivity(intent, optionsCompat.toBundle());
            }
        });

        QRcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, qrcode_activity.class);

                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(HomeActivity.this, NFCButton, ViewCompat.getTransitionName(QRcodeButton));

                startActivity(intent, optionsCompat.toBundle());
            }
        });

        HistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);

                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(HomeActivity.this, NFCButton, ViewCompat.getTransitionName(HistoryButton));
                startActivity(intent, optionsCompat.toBundle());
            }
        });

        ProfileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.updata_profile_page, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Edit Profile")
                        .setView(view)
                        .setCancelable(false);
                editName = view.findViewById(R.id.edit_name);
                editDescription = view.findViewById(R.id.edit_description);
                builder.setPositiveButton("Updata", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference("/User/" + code);
                        if (!editName.getText().toString().equals("")) {
                            Profile_name.setText(editName.getText().toString());
                            myRef.child("nick").setValue(editName.getText().toString());
                        }
                        if (!editDescription.getText().toString().equals("")) {
                            Profile_title.setText(editDescription.getText().toString());
                            myRef.child("description").setValue(editDescription.getText().toString());
                        }
                    }
                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        final ProgressDialog progressDialog = new ProgressDialog(HomeActivity.this);
        progressDialog.setMessage("Uploading");
        progressDialog.show();

        if (imgUri != null) {
            final StorageReference fileReference = storageReference.child(code + "." + getFileExtension(imgUri));

            uploadTask = fileReference.putFile(imgUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference("/User/" + code + "/profile picture");
                        myRef.setValue(mUri);

                        ProfileImg.setVisibility(View.GONE);
                        loadingImg.setVisibility(View.VISIBLE);
                        getProfile();
                        progressDialog.dismiss();
                    } else {
                        Toast.makeText(HomeActivity.this, "Failed!", Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }
            });
        } else {
            Toast.makeText(HomeActivity.this, "No image selected!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            imgUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(HomeActivity.this, "Upload in preogress", Toast.LENGTH_LONG).show();
            } else {
                uploadImage();
            }
        }
    }

    //跳出選單
    public void PopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.home_menu);
        popupMenu.show();
    }

    @Override
    public void onBackPressed() {

        //Alert視窗
        new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Exit?")
                .setMessage("真的要關掉嗎?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        moveTaskToBack(true);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        //判斷選單
        switch (item.getItemId()) {

            case R.id.logout_btn:

                DocumentReference acRef = FirebaseFirestore.getInstance().collection("User").document(code);
                acRef.update("token", "empty");

                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
                finish();
                return true;

            case R.id.setting_btn:
                Intent intent3 = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent3);
                return true;

            case R.id.feedback_btn:
                Intent intent1 = new Intent(HomeActivity.this, FeedbackActivity.class);
                startActivity(intent1);
                return true;

            case R.id.about_btn:
                Intent intent2 = new Intent(HomeActivity.this, AboutActivity.class);
                startActivity(intent2);
                return true;

            default:
                return false;
        }
    }

    private class GetImage extends AsyncTask<String, Integer, Bitmap> {

        @Override
        protected void onPreExecute() {
            //執行前
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            //執行中

            String urlStr = params[0];
            try {
                URL url = new URL(urlStr);
                return BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //執行進度
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //執行後
            super.onPostExecute(bitmap);
            ProfileImg.setImageBitmap(bitmap);
            ProfileImg.setVisibility(View.VISIBLE);
            loadingImg.setVisibility(View.GONE);
        }
    }


    private void getProfile() {

        //Profile updata
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference("/User/" + code);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("---RUN---", "getProfile");
                if (dataSnapshot == null) {
                    return;
                }

                Log.d("---RUN---", dataSnapshot.toString());
                if (dataSnapshot.child("nick") != null && (!("").equals(dataSnapshot.child("nick").getValue()))) {
                    Profile_name.setText(dataSnapshot.child("nick").getValue().toString());
                } else {
                    Profile_name.setText(R.string.user);
                }

                if (dataSnapshot.child("description") != null && (!("").equals(dataSnapshot.child("description").getValue()))) {
                    Profile_title.setText(dataSnapshot.child("description").getValue().toString());
                } else {
                    Profile_title.setText(R.string.student);
                }

                if (dataSnapshot.child("profile picture") != null && dataSnapshot.child("profile picture").getValue().toString().equals("default")) {
                    url = "https://portalx.yzu.edu.tw/PortalSocialVB/Include/ShowImage.aspx?ShowType=UserPic&UserAccount=s" + sid + "&UserPictureName=";
                } else if (dataSnapshot.child("profile picture") != null && !dataSnapshot.child("profile picture").getValue().toString().equals("")) {
                    url = dataSnapshot.child("profile picture").getValue().toString();
                } else {
                    ProfileImg.setImageResource(R.drawable.ic_default_profile);
                    return;
                }
                new GetImage().execute(url);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to read value." + error.toException().toString(), Toast.LENGTH_LONG).show();
            }
        });


    }

}

