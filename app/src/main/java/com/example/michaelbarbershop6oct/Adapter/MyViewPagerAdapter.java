package com.example.michaelbarbershop6oct.Adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.michaelbarbershop6oct.Fragments.BookingStep1Fragment;
import com.example.michaelbarbershop6oct.Fragments.BookingStep2Fragment;
import com.example.michaelbarbershop6oct.Fragments.BookingStep3Fragment;
import com.example.michaelbarbershop6oct.Fragments.BookingStep4Fragment;


public class MyViewPagerAdapter extends FragmentPagerAdapter {

    public MyViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

//    These steps represent 1. Choose Salon > 2. Choose Barber > 3. Choose time > 4. Confirm
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return BookingStep1Fragment.getInstance();
            case 1:
                return BookingStep2Fragment.getInstance();
            case 2:
                return BookingStep3Fragment.getInstance();
            case 3:
                return BookingStep4Fragment.getInstance();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 4;
    }
}
