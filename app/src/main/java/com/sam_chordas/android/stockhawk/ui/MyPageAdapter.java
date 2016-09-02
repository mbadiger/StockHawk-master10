package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

/**
 * Created by Saanu_mac on 7/29/16.
 */
public class MyPageAdapter extends FragmentStatePagerAdapter {

    String tabTitles[] = {"WEEK", "MONTH"};
    String symbol_arg;

    public MyPageAdapter(FragmentManager fm, Bundle args) {
        super(fm);
        symbol_arg = args.getString("symbol");
    }

    @Override
    public Fragment getItem(int position) {

        String pageTitle = (String) getPageTitle(position);
        ChartFragment chartFragment = ChartFragment.newInstance(pageTitle, symbol_arg);
        return chartFragment;
    }

    @Override
    public int getCount() {
        return 2;
    }


    @Override
    public CharSequence getPageTitle(int position) {
        //return super.getPageTitle(position);
        return tabTitles[position];
    }
}
