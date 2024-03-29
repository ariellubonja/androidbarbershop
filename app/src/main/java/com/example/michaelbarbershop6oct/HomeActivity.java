package com.example.michaelbarbershop6oct;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.michaelbarbershop6oct.Common.Common;
import com.example.michaelbarbershop6oct.Fragments.HomeFragment;
import com.example.michaelbarbershop6oct.Fragments.ShoppingFragment;
import com.example.michaelbarbershop6oct.Model.User;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;


public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    @BindView(R.id.bottom_navigation)
    BottomNavigationView mBottomNavigationView;

    BottomSheetDialog mBottomSheetDialog;

    CollectionReference mUserRef;

    AlertDialog mAlertDialog;

    @Override
    protected void onResume() {
        super.onResume();
        // we will check key from Paper and if available, we will show rating dialog
        // Check rating dialog
        checkRatingDialog();
    }

    private void checkRatingDialog() {
        Paper.init(this);
        String dataSerialized = Paper.book().read(Common.RATING_INFORMATION_KEY, "");
        if (!TextUtils.isEmpty(dataSerialized)) {
            Map<String, String> dataReceived = new Gson()
                    .fromJson(dataSerialized, new TypeToken<Map<String, String>>() {
                    }.getType());
            if (dataReceived != null) {
                Common.showRatingDialog(HomeActivity.this,
                        dataReceived.get(Common.RATING_STATE_KEY),
                        dataReceived.get(Common.RATING_SALON_ID),
                        dataReceived.get(Common.RATING_SALON_NAME),
                        dataReceived.get(Common.RATING_BARBER_ID));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.d(TAG, "onCreate: started!!");

        ButterKnife.bind(HomeActivity.this);

        // init
        mUserRef = FirebaseFirestore.getInstance().collection(getResources().getString(R.string.collection_user));

        mAlertDialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();

        // Check intent, if is login == true, enable full access
        // If is login == false, just let user around shopping to view
        if (getIntent() != null) {
            boolean isLogin = getIntent().getBooleanExtra(Common.IS_LOGIN, false);
            if (isLogin) {
                Log.d(TAG, "isLogin: called!!");
                mAlertDialog.show();

                // Check if user is exists
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // Save userPhone by Paper
                Paper.init(HomeActivity.this);
                Paper.book().write(Common.LOGGED_KEY, user.getPhoneNumber());

                DocumentReference currentUser = mUserRef.document(user.getPhoneNumber());
                currentUser.get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot userSnapshot = task.getResult();
                                if (!userSnapshot.exists()) {
                                    Log.d(TAG, "onComplete: called!");
                                    showUpdateDialog(user.getPhoneNumber());
                                } else {
                                    // If user already available in out system.
                                    Common.currentUser = userSnapshot.toObject(User.class);
                                    mBottomNavigationView.setSelectedItemId(R.id.action_home);
                                }

                                if (mAlertDialog.isShowing()) {
                                    mAlertDialog.dismiss();
                                }

                            }
                        });
            }

        }


    // View
        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()

    {
        Fragment fragment = null;

        @Override
        public boolean onNavigationItemSelected (@NonNull MenuItem menuItem){
        if (menuItem.getItemId() == R.id.action_home) {
            fragment = new HomeFragment();
        } else if (menuItem.getItemId() == R.id.action_shopping) {
            fragment = new ShoppingFragment();
        }
        return loadFragment(fragment);
    }
    });
}

    private boolean loadFragment(Fragment fragment) {
        Log.d(TAG, "loadFragment: called!!");
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
            return true;
        }
        return false;
    }

    private void showUpdateDialog(String phoneNumber) {
        Log.d(TAG, "showUpdateDialog: ");

        // Init dialog
        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setTitle("One more step!");
        mBottomSheetDialog.setCanceledOnTouchOutside(false);
        mBottomSheetDialog.setCancelable(false);

        View sheetView = getLayoutInflater().inflate(R.layout.layout_update_information, null);

        Button btn_update = sheetView.findViewById(R.id.btn_update);
        TextInputEditText edt_name = sheetView.findViewById(R.id.edt_name);
        TextInputEditText edt_address = sheetView.findViewById(R.id.edt_address);

        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAlertDialog.isShowing()) {
                    mAlertDialog.show();
                }
                User user = new User(edt_name.getText().toString(),
                        edt_address.getText().toString(),
                        phoneNumber);

                mUserRef.document(phoneNumber)
                        .set(user)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mBottomSheetDialog.dismiss();

                                if (mAlertDialog.isShowing()) {
                                    mAlertDialog.dismiss();
                                }

                                Common.currentUser = user;
                                mBottomNavigationView.setSelectedItemId(R.id.action_home);

                                Toast.makeText(HomeActivity.this, "Thank you",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mBottomSheetDialog.dismiss();

                                if (mAlertDialog.isShowing()) {
                                    mAlertDialog.dismiss();
                                }

                                Toast.makeText(HomeActivity.this, "" + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        mBottomSheetDialog.setContentView(sheetView);
        mBottomSheetDialog.show();

    }
}
