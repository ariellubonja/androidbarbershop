package com.example.michaelbarbershop6oct;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.michaelbarbershop6oct.Adapter.MyViewPagerAdapter;
import com.example.michaelbarbershop6oct.Common.Common;
import com.example.michaelbarbershop6oct.Common.NonSwipeViewPager;
import com.example.michaelbarbershop6oct.Model.Barber;
import com.example.michaelbarbershop6oct.Model.EventBus.BarberDoneEvent;
import com.example.michaelbarbershop6oct.Model.EventBus.ConfirmBookingEvent;
import com.example.michaelbarbershop6oct.Model.EventBus.DisplayTimeSlotEvent;
import com.example.michaelbarbershop6oct.Model.EventBus.EnableNextButton;
import com.example.michaelbarbershop6oct.Model.EventBus.UnableNextButton;
import com.shuhart.stepview.StepView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;


public class BookingActivity extends AppCompatActivity {

    private static final String TAG = BookingActivity.class.getSimpleName();

    //LocalBroadcastManager mLocalBroadcastManager;
    private AlertDialog mDialog;
    private CollectionReference barberRef;

    @BindView(R.id.step_view)
    StepView stepView;
    @BindView(R.id.view_pager)
    NonSwipeViewPager viewPager;
    @BindView(R.id.btn_previous_step)
    Button btn_previous_step;
    @BindView(R.id.btn_next_step)
    Button btn_next_step;

    // Event
    @OnClick(R.id.btn_previous_step)
    void previousStep() {
        if (Common.step == 3 || Common.step > 0) {
            Common.step--;
            viewPager.setCurrentItem(Common.step);

            // Always enable NEXT when Step <3
            if (Common.step < 3) {
                btn_next_step.setEnabled(true);
                setColorButton();
            }
        }
    }

    @OnClick(R.id.btn_next_step)
    void nextClick() {
        if (Common.step < 3 || Common.step == 0) {
            // increase
            Common.step++;

            // After choose salon
            if (Common.step == 1) {
                if (Common.currentSalon != null) {
                    loadBarberBySalon(Common.currentSalon.getSalonId());
                }
            }
            // Pick time slot
            else if (Common.step == 2) {
                if (Common.currentBarber != null) {
                    loadTimeSlotOfBarber(Common.currentBarber.getBarberId());
                }
            }
            // Confirm
            else if (Common.step == 3) {
                if (Common.currentTimeSlot != -1) {
                    confirmBooking();
                }
            }

            viewPager.setCurrentItem(Common.step);
        }
    }

    // Broadcast Receiver (To listen)
//    private BroadcastReceiver buttonNextReceiver = new BroadcastReceiver() {
////        @Override
////        public void onReceive(Context context, Intent intent) {
////            Log.d(TAG, "onReceive: BroadcastReceiver Called!!");
////
////            int step = intent.getIntExtra(Common.KEY_STEP, 0);
////            if (step == 1) {
////                Common.currentSalon = intent.getParcelableExtra(Common.KEY_SALON_STORE);
////            } else if (step == 2) {
////                Common.currentBarber = intent.getParcelableExtra(Common.KEY_BARBER_SELECTED);
////            } else if (step == 3) {
////                Common.currentTimeSlot = intent.getIntExtra(Common.KEY_TIME_SLOT, -1);
////            }
////
////            btn_next_step.setEnabled(true);
////            setColorButton();
////        }
////    };
////
////
////    private BroadcastReceiver clearButtonNextReceiver = new BroadcastReceiver() {
////        @Override
////        public void onReceive(Context context, Intent intent) {
////            Log.d(TAG, "onReceive: called!!");
////            btn_next_step.setEnabled(false);
////        }
////    };

    //=============================================================================
    // EventBus start

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void buttonNextReceiver(EnableNextButton event) {
        int step = event.getStep();
        if (step == 1) {
            Common.currentSalon = event.getSalon();
        } else if (step == 2) {
            Common.currentBarber = event.getBarber();
        } else if (step == 3) {
            Common.currentTimeSlot = event.getTimeSlot();
        }

        btn_next_step.setEnabled(true);
        setColorButton();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void clearButtonNextReceiver(UnableNextButton event) {
        if (event.isUnable()) {
            btn_next_step.setEnabled(false);
        }
    }
    //=============================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        Log.d(TAG, "onCreate: started");

        ButterKnife.bind(BookingActivity.this);

        mDialog = new SpotsDialog.Builder().setContext(this).build();

        // Register Listener
//        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
//        mLocalBroadcastManager.registerReceiver(buttonNextReceiver,
//                new IntentFilter(Common.KEY_ENABLE_BUTTON_NEXT));
//        mLocalBroadcastManager.registerReceiver(clearButtonNextReceiver,
//                new IntentFilter(Common.KEY_UNABLE_BUTTON_NEXT));

        setupStepView();
        setColorButton();

        viewPager.setAdapter(new MyViewPagerAdapter(getSupportFragmentManager()));
        // We have 4 fragment so we need keep state of this 4 screen page
        // If don't that, we will lost state of all view when we press previous
        viewPager.setOffscreenPageLimit(4);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // Show step
                stepView.go(position, true);

                if (position == 0) {
                    btn_previous_step.setEnabled(false);
                } else {
                    btn_previous_step.setEnabled(true);
                }

                btn_next_step.setEnabled(false);
                setColorButton();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called!!");
        Common.step = 0;
        //mLocalBroadcastManager.unregisterReceiver(buttonNextReceiver);
        //mLocalBroadcastManager.unregisterReceiver(clearButtonNextReceiver);
        super.onDestroy();
    }


    /**
     * remote IntentFilter
     * comment localBroadcast declare to find all position have working with localBroadcast
    **/
    private void confirmBooking() {
        Log.d(TAG, "confirmBooking: called!!");
        // send broadcast to fragment step four

        //Intent intent = new Intent(Common.KEY_CONFIRM_BOOKING);
        //mLocalBroadcastManager.sendBroadcast(intent);

        EventBus.getDefault().postSticky(new ConfirmBookingEvent(true));
    }

    private void loadTimeSlotOfBarber(String barberId) {
        Log.d(TAG, "loadTimeSlotOfBarber: called!!");
        // Send Local Broadcast to Fragment step3
        //Intent intent = new Intent(Common.KEY_DISPLAY_TIME_SLOT);
        //mLocalBroadcastManager.sendBroadcast(intent);

        EventBus.getDefault().postSticky(new DisplayTimeSlotEvent(true));
    }

    /**
     * Because here we use
     * intent.putParcelableArrayListExtra(Common.KEY_BARBER_LOAD_DONE, barbers);
     * to send an List of Barber to other Fragment, so we need create Event Class with property is Barber list
    **/
    private void loadBarberBySalon(String salonId) {
        Log.d(TAG, "loadBarberBySalon: called!!");

        mDialog.show();
        Log.d(TAG, "loadBarberBySalon: Common city: " + Common.city);

        // Now, select all barber of Salon
        // /AllSalon/NewYork/Branch/2QkgoRWoPoH7KMlsAbVK/Barber
        if (!TextUtils.isEmpty(Common.city)) {
            barberRef = FirebaseFirestore.getInstance()
                    .collection("SalonLocations")
                    .document(Common.city)
                    .collection("Branch")
                    .document(salonId)
                    .collection("Barber");

            barberRef.get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            ArrayList<Barber> barbers = new ArrayList<>();
                            for (QueryDocumentSnapshot barberSnapshot : task.getResult()) {
                                Barber barber = barberSnapshot.toObject(Barber.class);
                                barber.setPassword(""); // Remove password because in client app
                                barber.setBarberId(barberSnapshot.getId());

                                barbers.add(barber);
                            }

                            Log.d(TAG, "onComplete: Barbers Size: " + barbers.size());

                            // Send Broadcast to BookingStep2Fragment to load Recycler
                            //Intent intent = new Intent(Common.KEY_BARBER_LOAD_DONE);
                            //intent.putParcelableArrayListExtra(Common.KEY_BARBER_LOAD_DONE, barbers);
                            //mLocalBroadcastManager.sendBroadcast(intent);

                            EventBus.getDefault().postSticky(new BarberDoneEvent(barbers));

                            mDialog.dismiss();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                        }
                    });
        }
    }

    private void setColorButton() {
        Log.d(TAG, "setColorButton: called");

        if (btn_next_step.isEnabled()) {
            btn_next_step.setBackgroundResource(R.color.colorButton);
        }
        else {
            btn_next_step.setBackgroundResource(android.R.color.darker_gray);
        }

        if (btn_previous_step.isEnabled()) {
            btn_previous_step.setBackgroundResource(R.color.colorButton);
        }
        else {
            btn_previous_step.setBackgroundResource(android.R.color.darker_gray);
        }
    }

    private void setupStepView() {
        Log.d(TAG, "setupStepView: called");

        List<String> stepList = new ArrayList<>();
        stepList.add("Salon");
        stepList.add("Barber");
        stepList.add("Reservation Time");
        stepList.add("Confirm");
        stepView.setSteps(stepList);
    }
}
