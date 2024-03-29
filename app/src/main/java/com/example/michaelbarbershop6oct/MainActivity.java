package com.example.michaelbarbershop6oct;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.example.michaelbarbershop6oct.Common.Common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int APP_REQUEST_CODE = 711; //Any Number you want

    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @BindView(R.id.btn_login)
    Button btn_login;
    @BindView(R.id.txt_skip)
    TextView txt_skip;

    @OnClick(R.id.btn_login)
    void loginUser() {
        startActivityForResult(AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers).build(),APP_REQUEST_CODE);
    }

    @OnClick(R.id.txt_skip)
    void skipLoginJustGoHome() {
        Log.d(TAG, "skipLoginJustGoHome: called!!");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(Common.IS_LOGIN, false);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        if (authStateListener != null)
            firebaseAuth.removeAuthStateListener(authStateListener);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called!!");
        if (requestCode == APP_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else {
                Toast.makeText(this, "Failed to Sign in", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());

        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = firebaseAuth1 -> {
            FirebaseUser user = firebaseAuth1.getCurrentUser();
            if (user != null)
                checkUserFromFirebase(user);
        };

        Log.d(TAG, "onCreate: started!!");

        Dexter.withActivity(this)
                .withPermissions(new String[]{
                        Manifest.permission.WRITE_CALENDAR,
                        Manifest.permission.READ_CALENDAR
                })
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        // If already logged
                        if (user != null) {

                            // Get Token
                            FirebaseInstanceId.getInstance()
                                    .getInstanceId()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Common.updateToken(getBaseContext(), task.getResult().getToken());

                                            Log.d(TAG, "onComplete: TOKEN: "+task.getResult().getToken());

                                            Intent intent = new Intent(MainActivity.this,
                                                    HomeActivity.class);
                                            intent.putExtra(Common.IS_LOGIN, true);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(MainActivity.this,
                                                HomeActivity.class);
                                        intent.putExtra(Common.IS_LOGIN, true);
                                        startActivity(intent);
                                        finish();
                                    });

                        } else {
                            setContentView(R.layout.activity_main);
                            ButterKnife.bind(MainActivity.this);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                })
                .check();
    }

    private void checkUserFromFirebase(FirebaseUser user) {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Common.updateToken(getBaseContext(), task.getResult().getToken());

                        Log.d(TAG, "onComplete: TOKEN: "+task.getResult().getToken());

                        Intent intent = new Intent(MainActivity.this,
                                HomeActivity.class);
                        intent.putExtra(Common.IS_LOGIN, true);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this,
                            HomeActivity.class);
                    intent.putExtra(Common.IS_LOGIN, true);
                    startActivity(intent);
                    finish();
                });
    }

    private void printKeyHash() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KEYHASH", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
